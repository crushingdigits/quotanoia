package dev.claudewidget.wear

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * Simplified account + usage store for Wear OS.
 * In a production app this would sync from the phone via DataClient.
 * For POC, accounts are entered directly on the watch or hardcoded.
 */
class WearAccountStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "wear_accounts",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val cache: SharedPreferences =
        context.getSharedPreferences("wear_cache", Context.MODE_PRIVATE)

    data class WearAccount(val id: String, val name: String, val token: String)
    data class WearUsage(
        val fiveHourPct: Float,
        val sevenDayPct: Float,
        val hasError: Boolean = false,
        val errorMsg: String? = null,
    )

    fun loadAccounts(): List<WearAccount> {
        val json = prefs.getString("accounts", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                WearAccount(obj.getString("id"), obj.getString("name"), obj.getString("token"))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveAccounts(accounts: List<WearAccount>) {
        val arr = JSONArray()
        accounts.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("name", a.name)
                put("token", a.token)
            })
        }
        prefs.edit().putString("accounts", arr.toString()).apply()
    }

    fun cacheUsage(accountId: String, usage: WearUsage) {
        cache.edit()
            .putFloat("${accountId}_5h", usage.fiveHourPct)
            .putFloat("${accountId}_7d", usage.sevenDayPct)
            .putBoolean("${accountId}_err", usage.hasError)
            .putString("${accountId}_errMsg", usage.errorMsg)
            .apply()
    }

    fun getCachedUsage(accountId: String): WearUsage {
        return WearUsage(
            fiveHourPct = cache.getFloat("${accountId}_5h", -1f),
            sevenDayPct = cache.getFloat("${accountId}_7d", -1f),
            hasError = cache.getBoolean("${accountId}_err", false),
            errorMsg = cache.getString("${accountId}_errMsg", null),
        )
    }
}
