package ir.ahvaz.mehra.core.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.ahvaz.mehra.MainActivity
import ir.ahvaz.mehra.R
import ir.ahvaz.mehra.core.common.Constants
import ir.ahvaz.mehra.domain.model.NotificationItem
import ir.ahvaz.mehra.domain.model.RealtimeEvent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns a hub event into a system notification. Nothing is posted when the operator has
 * disabled notifications or has not granted POST_NOTIFICATIONS on Android 13+.
 */
@Singleton
class NotificationPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun publish(event: RealtimeEvent.Received, notificationsEnabled: Boolean) {
        if (!notificationsEnabled || !canNotify()) return
        notify(id = event.receivedAt.toEpochMilli().toInt(), title = event.title, body = event.body)
    }

    fun publish(item: NotificationItem, notificationsEnabled: Boolean) {
        if (!notificationsEnabled || !canNotify()) return
        notify(id = item.id.hashCode(), title = item.title, body = item.body)
    }

    fun publishPendingSync(count: Int) {
        if (!canNotify()) return
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, Constants.SYNC_NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("عملیات در انتظار ارسال")
            .setContentText("$count عملیات تا تأیید سرور باقی مانده است.")
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(Constants.SYNC_NOTIFICATION_ID, notification) }
    }

    private fun notify(id: Int, title: String, body: String?) {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, Constants.OPS_NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .apply { if (!body.isNullOrBlank()) setContentText(body) }
            .setStyle(NotificationCompat.BigTextStyle().bigText(body.orEmpty()))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    private fun canNotify(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
