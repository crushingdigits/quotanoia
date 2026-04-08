package dev.qwertyfinger.quotanoia.data

import androidx.annotation.VisibleForTesting
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
data class UsageFetchResult(val usage: UsageData, val newSessionKey: String? = null)

class ApiClient {

    private val client: OkHttpClient = OkHttpClient
        .Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor { message -> Timber.tag("OkHttp").d(message) }.apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            },
        ).build()

    fun fetchUsage(
        sessionKey: String,
        orgId: String,
    ): Result<UsageFetchResult> {
        Timber.d("Fetching usage for org %s", orgId)

        val url = "$BASE_URL/$orgId/usage"
        val request = Request
            .Builder()
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
                val retryAfterMs = resp
                    .header("Retry-After")
                    ?.toLongOrNull()
                    ?.let { it * MILLIS_PER_SECOND }

                Timber.d("Response: %d for org %s", resp.code, orgId)

                when (resp.code) {
                    HTTP_OK -> {
                        val newKey = parseSetCookieSessionKey(resp)
                        parseUsage(resp.body?.string().orEmpty(), newKey)
                    }

                    HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> {
                        Timber.w("Session expired (%d) for org %s", resp.code, orgId)
                        Result.failure(ApiException(AccountError.TokenExpired))
                    }

                    HTTP_TOO_MANY_REQUESTS -> {
                        Timber.w("Rate limited (429), retry-after: %s ms", retryAfterMs)
                        Result.failure(ApiException(AccountError.RateLimited, retryAfterMs))
                    }

                    in HTTP_SERVER_ERROR_MIN..HTTP_SERVER_ERROR_MAX -> {
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

    @Suppress("TooGenericExceptionCaught")
    @VisibleForTesting
    internal fun parseUsage(
        body: String,
        newSessionKey: String?,
    ): Result<UsageFetchResult> {
        return try {
            val json = JSONObject(body)
            val fiveHour = json.optJSONObject("five_hour")
            val sevenDay = json.optJSONObject("seven_day")

            if (fiveHour == null || sevenDay == null) {
                return Result.failure(
                    ApiException(AccountError.ParseError("Missing five_hour or seven_day in response")),
                )
            }

            val data = UsageData(
                fiveHour = parseWindow(fiveHour, "five_hour"),
                sevenDay = parseWindow(sevenDay, "seven_day"),
            )
            Timber.d("Parsed usage: 5h=%.1f%%, 7d=%.1f%%", data.fiveHour.utilization, data.sevenDay.utilization)
            if (newSessionKey != null) Timber.d("Session key rotated")
            Result.success(UsageFetchResult(data, newSessionKey))
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse usage response")
            Result.failure(ApiException(AccountError.ParseError(e.message ?: "Parse failed")))
        }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    internal fun parseWindow(
        obj: JSONObject,
        label: String,
    ): UsageWindow {
        val utilization = obj.optDouble("utilization", 0.0).toFloat()
        val resetsAtRaw = obj.optString("resets_at", "")
        val resetsAt = resetsAtRaw
            .takeIf { it.isNotEmpty() && it != "null" }
            ?.let {
                try {
                    Instant.parse(it)
                } catch (e: Exception) {
                    Timber.w("Unparseable resets_at in %s: %s", label, it)
                    null
                }
            }
        return UsageWindow(utilization, resetsAt)
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

    class ApiException(val accountError: AccountError, val retryAfterMs: Long? = null) :
        Exception(accountError.displayMessage)

    companion object {
        private const val BASE_URL = "https://claude.ai/api/organizations"
        private const val TIMEOUT_SECONDS = 15L
        private const val MILLIS_PER_SECOND = 1000L
        private const val HTTP_OK = 200
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val HTTP_SERVER_ERROR_MIN = 500
        private const val HTTP_SERVER_ERROR_MAX = 599
    }
}
