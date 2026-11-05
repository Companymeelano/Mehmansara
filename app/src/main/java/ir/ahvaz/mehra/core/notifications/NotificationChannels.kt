package ir.ahvaz.mehra.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import ir.ahvaz.mehra.R
import ir.ahvaz.mehra.core.common.Constants

object NotificationChannels {

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val ops = NotificationChannel(
            Constants.OPS_NOTIFICATION_CHANNEL,
            context.getString(R.string.notification_channel_ops_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_ops_desc)
            enableVibration(true)
        }

        val sync = NotificationChannel(
            Constants.SYNC_NOTIFICATION_CHANNEL,
            context.getString(R.string.notification_channel_sync_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_sync_desc)
        }

        manager.createNotificationChannel(ops)
        manager.createNotificationChannel(sync)
    }
}
