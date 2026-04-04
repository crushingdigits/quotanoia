package dev.qwertyfinger.quotanoia.notification

import android.content.Context
import android.content.SharedPreferences

class NotificationPrefsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Master toggle ──

    fun isAlertsEnabled(accountId: String): Boolean =
        prefs.getBoolean("${accountId}_alerts_enabled", false)

    fun setAlertsEnabled(accountId: String, enabled: Boolean) {
        prefs.edit().putBoolean("${accountId}_alerts_enabled", enabled).apply()
        if (enabled && getThresholds(accountId, "5h").isEmpty()) {
            setThresholds(accountId, "5h", DEFAULT_THRESHOLDS)
            setThresholds(accountId, "7d", DEFAULT_THRESHOLDS)
        }
    }

    // ── Thresholds ──

    fun getThresholds(accountId: String, window: String): List<Int> {
        val raw = prefs.getString("${accountId}_${window}_thresholds", null) ?: return emptyList()
        return raw.split(",").mapNotNull { it.trim().toIntOrNull() }.sorted()
    }

    fun setThresholds(accountId: String, window: String, thresholds: List<Int>) {
        prefs.edit()
            .putString("${accountId}_${window}_thresholds", thresholds.sorted().joinToString(","))
            .apply()
    }

    // ── Seen flags (reset-cycle-aware dedup) ──

    fun isThresholdSeen(accountId: String, window: String, threshold: Int): Boolean {
        return prefs.getString("${accountId}_${window}_${threshold}_seen_reset", null) != null
    }

    fun isThresholdSeenForReset(
        accountId: String,
        window: String,
        threshold: Int,
        currentResetsAt: String,
    ): Boolean {
        val stored = prefs.getString("${accountId}_${window}_${threshold}_seen_reset", null)
        return stored != null && stored == currentResetsAt
    }

    fun markThresholdSeen(accountId: String, window: String, threshold: Int, resetsAt: String) {
        prefs.edit()
            .putString("${accountId}_${window}_${threshold}_seen_reset", resetsAt)
            .apply()
    }

    fun clearStaleSeenFlags(accountId: String, window: String, currentResetsAt: String) {
        val thresholds = getThresholds(accountId, window)
        val editor = prefs.edit()
        for (threshold in thresholds) {
            val key = "${accountId}_${window}_${threshold}_seen_reset"
            val stored = prefs.getString(key, null)
            if (stored != null && stored != currentResetsAt) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    // ── Hint card ──

    fun isHintCardDismissed(): Boolean =
        prefs.getBoolean(KEY_HINT_DISMISSED, false)

    fun dismissHintCard() {
        prefs.edit().putBoolean(KEY_HINT_DISMISSED, true).apply()
    }

    // ── Session error dedup ──

    fun isSessionErrorNotified(accountId: String): Boolean =
        prefs.getBoolean("${accountId}_session_error_notified", false)

    fun setSessionErrorNotified(accountId: String, notified: Boolean) {
        prefs.edit().putBoolean("${accountId}_session_error_notified", notified).apply()
    }

    companion object {
        const val PREFS_NAME = "notification_prefs"
        val DEFAULT_THRESHOLDS = listOf(50, 75, 90, 100)
        private const val KEY_HINT_DISMISSED = "hint_card_dismissed"
    }
}
