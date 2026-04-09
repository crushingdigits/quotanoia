package dev.qwertyfinger.quotanoia.wear

import android.content.Context
import android.content.SharedPreferences
import dev.qwertyfinger.quotanoia.data.sync.WearSyncAccount
import kotlinx.serialization.json.Json

/**
 * Local cache for usage data received from the phone via Data Layer.
 * No tokens are stored on the watch — only display data.
 */
class WearAccountStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("wear_usage_cache", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

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

    fun parsePayload(data: ByteArray): List<WearSyncAccount> {
        val raw = String(data, Charsets.UTF_8)
        return json.decodeFromString<List<WearSyncAccount>>(raw)
    }

    fun updateFromSyncAccounts(accounts: List<WearSyncAccount>) {
        prefs.edit().apply {
            clear()
            putInt("count", accounts.size)
            for ((i, account) in accounts.withIndex()) {
                putString("${i}_id", account.id)
                putString("${i}_name", account.name)
                putBoolean("${i}_has_data", account.hasData)
                putFloat("${i}_5h_pct", account.fiveHourPct.toFloat())
                putFloat("${i}_7d_pct", account.sevenDayPct.toFloat())
                putLong("${i}_5h_resets", account.fiveHourResetsAtMs)
                putLong("${i}_7d_resets", account.sevenDayResetsAtMs)
                putBoolean("${i}_has_error", account.hasError)
                putString("${i}_error_msg", account.errorMsg)
            }
            apply()
        }
    }
}
