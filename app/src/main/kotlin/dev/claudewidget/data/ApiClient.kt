package dev.claudewidget.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit

class ApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun fetchUsage(token: String): Result<UsageData> {
        val request = Request.Builder()
            .url(USAGE_ENDPOINT)
            .header("Authorization", "Bearer $token")
            .header("anthropic-beta", "oauth-2025-04-20")
            .get()
            .build()

        return try {
            val response = client.newCall(request).execute()
            response.use { resp ->
                when (resp.code) {
                    200 -> parseUsage(resp.body?.string() ?: "")
                    401 -> Result.failure(ApiException(AccountError.TokenExpired))
                    429 -> Result.failure(ApiException(AccountError.RateLimited))
                    in 500..599 -> Result.failure(
                        ApiException(AccountError.ServerError(resp.code))
                    )
                    else -> Result.failure(
                        ApiException(AccountError.ServerError(resp.code))
                    )
                }
            }
        } catch (e: IOException) {
            Result.failure(ApiException(AccountError.NetworkError(e.message ?: "Unknown")))
        }
    }

    private fun parseUsage(body: String): Result<UsageData> {
        return try {
            val json = JSONObject(body)
            val fiveHour = json.getJSONObject("five_hour")
            val sevenDay = json.getJSONObject("seven_day")

            Result.success(
                UsageData(
                    fiveHour = UsageWindow(
                        utilization = fiveHour.getDouble("utilization").toFloat(),
                        resetsAt = Instant.parse(fiveHour.getString("resets_at")),
                    ),
                    sevenDay = UsageWindow(
                        utilization = sevenDay.getDouble("utilization").toFloat(),
                        resetsAt = Instant.parse(sevenDay.getString("resets_at")),
                    ),
                )
            )
        } catch (e: Exception) {
            Result.failure(ApiException(AccountError.ParseError(e.message ?: "Parse failed")))
        }
    }

    class ApiException(val accountError: AccountError) : Exception(accountError.displayMessage)

    companion object {
        private const val USAGE_ENDPOINT = "https://api.anthropic.com/api/oauth/usage"
    }
}
