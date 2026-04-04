package dev.qwertyfinger.quotanoia.notification

import dev.qwertyfinger.quotanoia.data.AccountError
import dev.qwertyfinger.quotanoia.data.AccountState
import dev.qwertyfinger.quotanoia.data.UsageWindow

data class PendingNotification(
    val id: Int,
    val channelId: String,
    val accountName: String,
    val title: String,
    val body: String,
    val accountId: String,
)

class NotificationEvaluator(private val prefs: NotificationPrefsStore) {

    fun evaluate(states: List<AccountState>): List<PendingNotification> {
        val result = mutableListOf<PendingNotification>()

        for (state in states) {
            val accountId = state.account.id
            val accountName = state.account.name

            if (!prefs.isAlertsEnabled(accountId)) continue

            // Session expiry handling
            if (state.error is AccountError.TokenExpired) {
                if (!prefs.isSessionErrorNotified(accountId)) {
                    prefs.setSessionErrorNotified(accountId, true)
                    result.add(
                        PendingNotification(
                            id = "${accountId}_session".hashCode(),
                            channelId = NotificationChannelManager.CHANNEL_ACCOUNT_ISSUES,
                            accountName = accountName,
                            title = "Session Expired",
                            body = "Session for $accountName has expired. Re-enter credentials in settings.",
                            accountId = accountId,
                        ),
                    )
                }
                // Dedup: already notified, skip
            } else {
                // No token error — if we previously notified, produce dismiss marker
                if (prefs.isSessionErrorNotified(accountId)) {
                    prefs.setSessionErrorNotified(accountId, false)
                    result.add(
                        PendingNotification(
                            id = -kotlin.math.abs("${accountId}_session".hashCode()),
                            channelId = NotificationChannelManager.CHANNEL_ACCOUNT_ISSUES,
                            accountName = accountName,
                            title = "",
                            body = "",
                            accountId = accountId,
                        ),
                    )
                }
            }

            // Usage threshold evaluation
            val usage = state.usage ?: continue

            evaluateWindow(accountId, accountName, "5h", "5-Hour", usage.fiveHour, result)
            evaluateWindow(accountId, accountName, "7d", "7-Day", usage.sevenDay, result)
        }

        return result
    }

    private fun evaluateWindow(
        accountId: String,
        accountName: String,
        windowKey: String,
        windowLabel: String,
        window: UsageWindow,
        result: MutableList<PendingNotification>,
    ) {
        val resetsAt = window.resetsAt?.toString() ?: return // no reset time = window not active

        // Clear stale seen flags if reset cycle changed
        prefs.clearStaleSeenFlags(accountId, windowKey, resetsAt)

        val thresholds = prefs.getThresholds(accountId, windowKey)
        val utilization = window.utilization

        for (threshold in thresholds) {
            if (utilization >= threshold && !prefs.isThresholdSeenForReset(accountId, windowKey, threshold, resetsAt)) {
                val channel = if (threshold >= 90) {
                    NotificationChannelManager.CHANNEL_USAGE_CRITICAL
                } else {
                    NotificationChannelManager.CHANNEL_USAGE_WARNINGS
                }

                result.add(
                    PendingNotification(
                        id = "${accountId}_${windowKey}_${threshold}".hashCode(),
                        channelId = channel,
                        accountName = accountName,
                        title = "$accountName: $windowLabel usage at ${utilization.toInt()}%",
                        body = "$windowLabel window has reached the $threshold% threshold.",
                        accountId = accountId,
                    ),
                )

                prefs.markThresholdSeen(accountId, windowKey, threshold, resetsAt)
            }
        }
    }
}
