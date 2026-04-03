package dev.qwertyfinger.quotanoia.wear

import android.content.Context
import android.content.SharedPreferences

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
                    hasError = hasError,
                    errorMsg = errorMsg,
                )
            } else {
                WearUsage(-1f, -1f, hasError, errorMsg)
            }

            AccountWithUsage(WearAccount(id, name), usage)
        }
    }

    fun updateFromDataMap(
        count: Int,
        getter: (key: String) -> Any?,
    ) {
        prefs.edit().apply {
            clear()
            putInt("count", count)
            for (i in 0 until count) {
                putString("${i}_id", getter("${i}_id") as? String ?: "")
                putString("${i}_name", getter("${i}_name") as? String ?: "")
                putBoolean("${i}_has_data", getter("${i}_has_data") as? Boolean ?: false)
                putFloat("${i}_5h_pct", (getter("${i}_5h_pct") as? Float) ?: -1f)
                putFloat("${i}_7d_pct", (getter("${i}_7d_pct") as? Float) ?: -1f)
                putBoolean("${i}_has_error", getter("${i}_has_error") as? Boolean ?: false)
                putString("${i}_error_msg", getter("${i}_error_msg") as? String ?: "")
            }
            apply()
        }
    }
}
