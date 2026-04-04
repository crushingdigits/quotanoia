package dev.qwertyfinger.quotanoia.data

import dev.qwertyfinger.quotanoia.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit

/** Result of a usage fetch — includes an optional rotated session key. */
data class UsageFetchResult(
    val usage: UsageData,
    val newSessionKey: String? = null,
)

class ApiClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor { message -> Timber.tag("OkHttp").d(message) }.apply {
                level = if (BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.BODY
                else
                    HttpLoggingInterceptor.Level.NONE
            }
        )
        .build()

    fun fetchUsage(sessionKey: String, orgId: String): Result<UsageFetchResult> {
        Timber.d("Fetching usage for org %s", orgId)

        val url = "$BASE_URL/$orgId/usage"
        val request = Request.Builder()
            .url(url)
            .header("accept", "*/*")
            .header("content-type", "application/json")
            .header("anthropic-client-platform", "web_claude_ai")
            .header("Cookie", "sessionKey=$sessionKey")
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.use { resp ->
                val retryAfterMs = resp.header("Retry-After")?.toLongOrNull()
                    ?.let { it * 1000L }

                Timber.d("Response: %d for org %s", resp.code, orgId)

                when (resp.code) {
                    200 -> {
                        val newKey = parseSetCookieSessionKey(resp)
                        parseUsage(resp.body?.string() ?: "", newKey)
                    }
                    401, 403 -> {
                        Timber.w("Session expired (%d) for org %s", resp.code, orgId)
                        Result.failure(ApiException(AccountError.TokenExpired))
                    }
                    429 -> {
                        Timber.w("Rate limited (429), retry-after: %s ms", retryAfterMs)
                        Result.failure(ApiException(AccountError.RateLimited, retryAfterMs))
                    }
                    in 500..599 -> {
                        Timber.w("Server error (%d), retry-after: %s ms", resp.code, retryAfterMs)
                        Result.failure(ApiException(AccountError.ServerError(resp.code), retryAfterMs))
                    }
                    else -> {
                        Timber.w("Unexpected status %d", resp.code)
                        Result.failure(ApiException(AccountError.ServerError(resp.code)))
                    }
                }
            }
        } catch (e: IOException) {
            Timber.e(e, "Network error fetching usage")
            Result.failure(ApiException(AccountError.NetworkError(e.message ?: "Unknown")))
        }
    }

    private fun parseUsage(body: String, newSessionKey: String?): Result<UsageFetchResult> {
        return try {
            val json = JSONObject(body)
            val fiveHour = json.getJSONObject("five_hour")
            val sevenDay = json.getJSONObject("seven_day")

            val data = UsageData(
                fiveHour = UsageWindow(
                    utilization = fiveHour.getDouble("utilization").toFloat(),
                    resetsAt = fiveHour.optString("resets_at", "").takeIf { it.isNotEmpty() && it != "null" }?.let { Instant.parse(it) },
                ),
                sevenDay = UsageWindow(
                    utilization = sevenDay.getDouble("utilization").toFloat(),
                    resetsAt = sevenDay.optString("resets_at", "").takeIf { it.isNotEmpty() && it != "null" }?.let { Instant.parse(it) },
                ),
            )
            Timber.d("Parsed usage: 5h=%.1f%%, 7d=%.1f%%", data.fiveHour.utilization, data.sevenDay.utilization)
            if (newSessionKey != null) Timber.d("Session key rotated")
            Result.success(UsageFetchResult(data, newSessionKey))
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse usage response")
            Result.failure(ApiException(AccountError.ParseError(e.message ?: "Parse failed")))
        }
    }

    /** Extract rotated sessionKey from Set-Cookie header if present. */
    private fun parseSetCookieSessionKey(resp: okhttp3.Response): String? {
        // OkHttp may return multiple Set-Cookie headers
        for (value in resp.headers("Set-Cookie")) {
            for (part in value.split(";")) {
                val trimmed = part.trim()
                if (trimmed.startsWith("sessionKey=")) {
                    return trimmed.removePrefix("sessionKey=")
                }
            }
        }
        return null
    }

    class ApiException(
        val accountError: AccountError,
        val retryAfterMs: Long? = null,
    ) : Exception(accountError.displayMessage)

    companion object {
        private const val BASE_URL = "https://claude.ai/api/organizations"
    }
}
