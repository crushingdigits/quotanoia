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

    fun fetchUsage(token: String): Result<UsageData> {
        Timber.d("Fetching usage for token ending …%s", token.takeLast(8))

        val request = Request.Builder()
            .url(USAGE_ENDPOINT)
            .header("Authorization", "Bearer $token")
            .header("anthropic-beta", "oauth-2025-04-20")
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.use { resp ->
                val retryAfterMs = resp.header("Retry-After")?.toLongOrNull()
                    ?.let { it * 1000L }

                Timber.d("Response: %d for token …%s", resp.code, token.takeLast(8))

                when (resp.code) {
                    200 -> parseUsage(resp.body?.string() ?: "")
                    401 -> {
                        Timber.w("Token expired (401) for …%s", token.takeLast(8))
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

    private fun parseUsage(body: String): Result<UsageData> {
        return try {
            val json = JSONObject(body)
            val fiveHour = json.getJSONObject("five_hour")
            val sevenDay = json.getJSONObject("seven_day")

            val data = UsageData(
                fiveHour = UsageWindow(
                    utilization = fiveHour.getDouble("utilization").toFloat(),
                    resetsAt = Instant.parse(fiveHour.getString("resets_at")),
                ),
                sevenDay = UsageWindow(
                    utilization = sevenDay.getDouble("utilization").toFloat(),
                    resetsAt = Instant.parse(sevenDay.getString("resets_at")),
                ),
            )
            Timber.d("Parsed usage: 5h=%.1f%%, 7d=%.1f%%", data.fiveHour.utilization, data.sevenDay.utilization)
            Result.success(data)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse usage response")
            Result.failure(ApiException(AccountError.ParseError(e.message ?: "Parse failed")))
        }
    }

    class ApiException(
        val accountError: AccountError,
        val retryAfterMs: Long? = null,
    ) : Exception(accountError.displayMessage)

    companion object {
        private const val USAGE_ENDPOINT = "https://api.anthropic.com/api/oauth/usage"
    }
}
