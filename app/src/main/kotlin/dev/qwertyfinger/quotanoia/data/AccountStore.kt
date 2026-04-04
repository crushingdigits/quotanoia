package dev.qwertyfinger.quotanoia.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists accounts (with tokens encrypted) and usage cache.
 */
class AccountStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "claude_accounts",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    /** Plain prefs for usage cache + error state (not sensitive). */
    private val cachePrefs: SharedPreferences =
        context.getSharedPreferences("usage_cache", Context.MODE_PRIVATE)

    // ── Accounts ────────────────────────────────────────────

    fun loadAccounts(): List<ClaudeAccount> {
        val json = securePrefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                ClaudeAccount(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    sessionKey = obj.getString("sessionKey"),
                    orgId = obj.getString("orgId"),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveAccounts(accounts: List<ClaudeAccount>) {
        val arr = JSONArray()
        accounts.forEach { a ->
            arr.put(
                JSONObject().apply {
                    put("id", a.id)
                    put("name", a.name)
                    put("sessionKey", a.sessionKey)
                    put("orgId", a.orgId)
                }
            )
        }
        securePrefs.edit().putString(KEY_ACCOUNTS, arr.toString()).apply()
    }

    fun addAccount(account: ClaudeAccount) {
        val list = loadAccounts().toMutableList()
        list.add(account)
        saveAccounts(list)
    }

    fun removeAccount(id: String) {
        saveAccounts(loadAccounts().filter { it.id != id })
        clearCachedState(id)
    }

    fun updateAccount(account: ClaudeAccount) {
        val list = loadAccounts().toMutableList()
        val idx = list.indexOfFirst { it.id == account.id }
        if (idx >= 0) {
            list[idx] = account
            saveAccounts(list)
        }
    }

    // ── Usage cache ─────────────────────────────────────────

    fun cacheUsage(accountId: String, usage: UsageData) {
        val obj = JSONObject().apply {
            put("fiveHourUtil", usage.fiveHour.utilization)
            put("fiveHourResets", usage.fiveHour.resetsAt?.toString() ?: JSONObject.NULL)
            put("sevenDayUtil", usage.sevenDay.utilization)
            put("sevenDayResets", usage.sevenDay.resetsAt?.toString() ?: JSONObject.NULL)
            put("fetchedAt", usage.fetchedAt.toString())
        }
        cachePrefs.edit()
            .putString("usage_$accountId", obj.toString())
            .remove("error_$accountId")
            .putLong("backoff_$accountId", BASE_POLL_INTERVAL_MS)
            .apply()
    }

    fun cacheError(accountId: String, error: AccountError, backoffMs: Long) {
        cachePrefs.edit()
            .putString("error_$accountId", error.displayMessage)
            .putLong("backoff_$accountId", backoffMs)
            .apply()
    }

    fun getCachedUsage(accountId: String): UsageData? {
        val json = cachePrefs.getString("usage_$accountId", null) ?: return null
        return try {
            val obj = JSONObject(json)
            UsageData(
                fiveHour = UsageWindow(
                    utilization = obj.optDouble("fiveHourUtil", 0.0).toFloat(),
                    resetsAt = obj.optString("fiveHourResets", "").takeIf { it.isNotEmpty() && it != "null" }?.let { java.time.Instant.parse(it) },
                ),
                sevenDay = UsageWindow(
                    utilization = obj.optDouble("sevenDayUtil", 0.0).toFloat(),
                    resetsAt = obj.optString("sevenDayResets", "").takeIf { it.isNotEmpty() && it != "null" }?.let { java.time.Instant.parse(it) },
                ),
                fetchedAt = java.time.Instant.parse(obj.getString("fetchedAt")),
            )
        } catch (_: Exception) {
            null
        }
    }

    fun getCachedError(accountId: String): String? =
        cachePrefs.getString("error_$accountId", null)

    fun getBackoff(accountId: String): Long =
        cachePrefs.getLong("backoff_$accountId", BASE_POLL_INTERVAL_MS)

    private fun clearCachedState(accountId: String) {
        cachePrefs.edit()
            .remove("usage_$accountId")
            .remove("error_$accountId")
            .remove("backoff_$accountId")
            .apply()
    }

    companion object {
        private const val KEY_ACCOUNTS = "accounts_json"
    }
}
