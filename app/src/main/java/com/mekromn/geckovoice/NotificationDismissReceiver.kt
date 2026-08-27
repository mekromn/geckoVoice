package com.mekromn.geckovoice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val key = intent.getStringExtra(MainActivity.EXTRA_NOTIFICATION_KEY) ?: return
        (context.applicationContext as? GeckoVoiceApplication)
            ?.notificationBridge
            ?.onDismissed(key)
    }
}
