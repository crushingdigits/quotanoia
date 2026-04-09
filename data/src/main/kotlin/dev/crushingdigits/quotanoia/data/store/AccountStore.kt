package dev.crushingdigits.quotanoia.data.store

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dev.crushingdigits.quotanoia.data.model.AccountError
import dev.crushingdigits.quotanoia.data.model.BASE_POLL_INTERVAL_MS
import dev.crushingdigits.quotanoia.data.model.ClaudeAccount
import dev.crushingdigits.quotanoia.data.model.UsageData
import dev.crushingdigits.quotanoia.data.model.UsageWindow
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.time.Instant

/**
 * Persists accounts (with tokens encrypted) and usage cache.
 */
@Suppress("TooManyFunctions")
class AccountStore(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private val masterKey = MasterKey
        .Builder(context)
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
        val raw = securePrefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<ClaudeAccount>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveAccounts(accounts: List<ClaudeAccount>) {
        securePrefs.edit().putString(KEY_ACCOUNTS, json.encodeToString(accounts)).apply()
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

    fun cacheUsage(
        accountId: String,
        usage: UsageData,
    ) {
        val dto = UsageCacheDto(
            fiveHourUtil = usage.fiveHour.utilization,
            fiveHourResets = usage.fiveHour.resetsAt?.toString(),
            sevenDayUtil = usage.sevenDay.utilization,
            sevenDayResets = usage.sevenDay.resetsAt?.toString(),
            fetchedAt = usage.fetchedAt.toString(),
        )
        cachePrefs
            .edit()
            .putString("usage_$accountId", json.encodeToString(dto))
            .remove("error_$accountId")
            .putLong("backoff_$accountId", BASE_POLL_INTERVAL_MS)
            .apply()
    }

    fun cacheError(
        accountId: String,
        error: AccountError,
        backoffMs: Long,
    ) {
        cachePrefs
            .edit()
            .putString("error_$accountId", error.displayMessage)
            .putLong("backoff_$accountId", backoffMs)
            .apply()
    }

    @Suppress("ReturnCount")
    fun getCachedUsage(accountId: String): UsageData? {
        val raw = cachePrefs.getString("usage_$accountId", null) ?: return null
        return try {
            val dto = json.decodeFromString<UsageCacheDto>(raw)
            UsageData(
                fiveHour = UsageWindow(
                    utilization = dto.fiveHourUtil,
                    resetsAt = parseInstant(dto.fiveHourResets),
                ),
                sevenDay = UsageWindow(
                    utilization = dto.sevenDayUtil,
                    resetsAt = parseInstant(dto.sevenDayResets),
                ),
                fetchedAt = parseInstant(dto.fetchedAt) ?: return null,
            )
        } catch (_: Exception) {
            null
        }
    }

    fun getCachedError(accountId: String): String? = cachePrefs.getString("error_$accountId", null)

    fun getBackoff(accountId: String): Long = cachePrefs.getLong("backoff_$accountId", BASE_POLL_INTERVAL_MS)

    private fun clearCachedState(accountId: String) {
        cachePrefs
            .edit()
            .remove("usage_$accountId")
            .remove("error_$accountId")
            .remove("backoff_$accountId")
            .apply()
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun parseInstant(value: String?): Instant? {
        if (value.isNullOrEmpty() || value == "null") return null
        return try {
            Instant.parse(value)
        } catch (e: Exception) {
            Timber.w("Unparseable Instant: %s", value)
            null
        }
    }

    companion object {
        private const val KEY_ACCOUNTS = "accounts_json"
    }
}
