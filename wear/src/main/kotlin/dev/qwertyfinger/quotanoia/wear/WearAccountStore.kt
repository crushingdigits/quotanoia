package dev.qwertyfinger.quotanoia.wear

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

/**
 * Local cache for usage data received from the phone via Data Layer.
 * No tokens are stored on the watch — only display data.
 */
class WearAccountStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("wear_usage_cache", Context.MODE_PRIVATE)

    data class WearAccount(val id: String, val name: String)
    data class WearUsage(
        val fiveHourPct: Float,
        val sevenDayPct: Float,
        val fiveHourResetsAtMs: Long = 0L,
        val sevenDayResetsAtMs: Long = 0L,
        val hasError: Boolean = false,
        val errorMsg: String? = null,
    )

    data class AccountWithUsage(val account: WearAccount, val usage: WearUsage)

    fun getAll(): List<AccountWithUsage> {
        val count = prefs.getInt("count", 0)
        if (count == 0) return emptyList()

        return (0 until count).mapNotNull { i ->
            val id = prefs.getString("${i}_id", null) ?: return@mapNotNull null
            val name = prefs.getString("${i}_name", null) ?: return@mapNotNull null
            val hasData = prefs.getBoolean("${i}_has_data", false)
            val hasError = prefs.getBoolean("${i}_has_error", false)
            val errorMsg = prefs.getString("${i}_error_msg", null)

            val usage = if (hasData) {
                WearUsage(
                    fiveHourPct = prefs.getFloat("${i}_5h_pct", -1f),
                    sevenDayPct = prefs.getFloat("${i}_7d_pct", -1f),
                    fiveHourResetsAtMs = prefs.getLong("${i}_5h_resets", 0L),
                    sevenDayResetsAtMs = prefs.getLong("${i}_7d_resets", 0L),
                    hasError = hasError,
                    errorMsg = errorMsg,
                )
            } else {
                WearUsage(-1f, -1f, hasError = hasError, errorMsg = errorMsg)
            }

            AccountWithUsage(WearAccount(id, name), usage)
        }
    }

    fun updateFromJson(arr: JSONArray) {
        prefs.edit().apply {
            clear()
            putInt("count", arr.length())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                putString("${i}_id", obj.optString("id", ""))
                putString("${i}_name", obj.optString("name", ""))
                putBoolean("${i}_has_data", obj.optBoolean("has_data", false))
                putFloat("${i}_5h_pct", obj.optDouble("5h_pct", -1.0).toFloat())
                putFloat("${i}_7d_pct", obj.optDouble("7d_pct", -1.0).toFloat())
                putLong("${i}_5h_resets", obj.optLong("5h_resets_at", 0L))
                putLong("${i}_7d_resets", obj.optLong("7d_resets_at", 0L))
                putBoolean("${i}_has_error", obj.optBoolean("has_error", false))
                putString("${i}_error_msg", obj.optString("error_msg", ""))
            }
            apply()
        }
    }
}
