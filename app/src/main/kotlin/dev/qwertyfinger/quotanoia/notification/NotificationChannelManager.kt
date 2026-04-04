package dev.qwertyfinger.quotanoia.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannelManager {

    const val CHANNEL_USAGE_WARNINGS = "usage_warnings"
    const val CHANNEL_USAGE_CRITICAL = "usage_critical"
    const val CHANNEL_ACCOUNT_ISSUES = "account_issues"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val warnings = NotificationChannel(
            CHANNEL_USAGE_WARNINGS,
            "Usage Warnings",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Alerts when usage reaches moderate thresholds"
        }

        val critical = NotificationChannel(
            CHANNEL_USAGE_CRITICAL,
            "Usage Critical",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alerts when usage reaches high or maximum thresholds"
        }

        val accountIssues = NotificationChannel(
            CHANNEL_ACCOUNT_ISSUES,
            "Account Issues",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alerts for session expiry and other account problems"
        }

        manager.createNotificationChannels(listOf(warnings, critical, accountIssues))
    }
}
