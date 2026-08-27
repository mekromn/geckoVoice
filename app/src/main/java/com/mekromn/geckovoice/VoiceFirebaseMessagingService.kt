package com.mekromn.geckovoice

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.tatarka.webpush.relay.WebPushRelay
import me.tatarka.webpush.relay.WebPushRelayKeyManager
import okio.buffer

class VoiceFirebaseMessagingService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "FCM token refreshed")
        appOrNull()?.webPushBridge?.onFirebaseTokenChanged()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        if (!WebPushRelay.isWebPush(message)) {
            Log.d(TAG, "Ignoring non-WebPush FCM message")
            return
        }

        serviceScope.launch {
            try {
                val encrypted = WebPushRelay.decode(message)
                val source = WebPushRelayKeyManager(this@VoiceFirebaseMessagingService)
                    .decrypt(encrypted)
                    .buffer()
                val payload = source.use { it.readByteArray() }
                appOrNull()?.webPushBridge?.deliverPush(payload)
            } catch (t: Throwable) {
                Log.e(TAG, "Unable to decode/decrypt incoming Web Push", t)
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun appOrNull(): GeckoVoiceApplication? =
        application as? GeckoVoiceApplication

    companion object {
        private const val TAG = "VoiceFcmService"
    }
}
