package dev.qwertyfinger.quotanoia.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.qwertyfinger.quotanoia.MainActivity
import timber.log.Timber
import kotlin.math.abs

class NotificationPoster(private val context: Context) {

    fun post(notifications: List<PendingNotification>) {
        if (!NotificationPermissionHelper.hasPermission(context)) {
            Timber.d("Notification permission not granted, skipping %d notifications", notifications.size)
            return
        }

        val manager = NotificationManagerCompat.from(context)

        for (notification in notifications) {
            if (notification.id < 0) {
                // Dismiss marker
                dismiss(abs(notification.id))
                continue
            }

            val tapIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notification.id,
                tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val built = NotificationCompat.Builder(context, notification.channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(notification.title)
                .setContentText(notification.body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            try {
                manager.notify(notification.id, built)
                Timber.d("Posted notification %d: %s", notification.id, notification.title)
            } catch (e: SecurityException) {
                Timber.w(e, "SecurityException posting notification — permission likely revoked")
            }
        }
    }

    fun dismiss(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
        Timber.d("Dismissed notification %d", notificationId)
    }
}
