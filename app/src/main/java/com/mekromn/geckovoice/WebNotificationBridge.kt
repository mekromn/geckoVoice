package com.mekromn.geckovoice

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ConcurrentHashMap
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebNotification
import org.mozilla.geckoview.WebNotificationDelegate

class WebNotificationBridge(
    private val context: Context,
    runtime: GeckoRuntime,
) : WebNotificationDelegate {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val active = ConcurrentHashMap<String, WebNotification>()

    init {
        createChannel()
        runtime.webNotificationDelegate = this
    }

    override fun onShowNotification(notification: WebNotification) {
        mainHandler.post { showOnMain(notification) }
    }

    override fun onCloseNotification(notification: WebNotification) {
        mainHandler.post {
            val key = keyFor(notification)
            active.remove(key)
            NotificationManagerCompat.from(context).cancel(idFor(key))
            notification.dismiss()
        }
    }

    fun onTapped(key: String) {
        mainHandler.post {
            val notification = active.remove(key)
            if (notification != null) {
                notification.click()
                notification.dismiss()
            }
            NotificationManagerCompat.from(context).cancel(idFor(key))
        }
    }

    fun onDismissed(key: String) {
        mainHandler.post {
            active.remove(key)?.dismiss()
        }
    }

    private fun showOnMain(notification: WebNotification) {
        if (!canPostNotifications()) {
            Log.w(TAG, "Android notification permission is not granted; dismissing web notification")
            notification.dismiss()
            return
        }

        val key = keyFor(notification)
        active[key] = notification
        val id = idFor(key)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_NOTIFICATION_KEY, key)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            id,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val dismissIntent = Intent(context, NotificationDismissReceiver::class.java).apply {
            putExtra(MainActivity.EXTRA_NOTIFICATION_KEY, key)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_voice)
            .setContentTitle(notification.title ?: "Google Voice")
            .setContentText(notification.text ?: "New Google Voice activity")
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.text))
            .setContentIntent(openPendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .setAutoCancel(true)
            .setSilent(notification.silent)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)

        if (!notification.silent && notification.vibrate.isNotEmpty()) {
            builder.setVibrate(notification.vibrate.map { it.toLong() }.toLongArray())
        }

        NotificationManagerCompat.from(context).notify(id, builder.build())
        notification.show()
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun keyFor(notification: WebNotification): String =
        notification.tag.ifBlank {
            "${notification.origin}|${notification.title.orEmpty()}|${notification.text.orEmpty()}"
        }

    private fun idFor(key: String): Int = key.hashCode() and 0x7fffffff

    companion object {
        private const val TAG = "WebNotificationBridge"
        private const val CHANNEL_ID = "google_voice_web"
    }
}
