package com.mekromn.geckovoice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import me.tatarka.webpush.relay.WebPushRelay
import me.tatarka.webpush.relay.WebPushRelayKeyManager
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebPushDelegate
import org.mozilla.geckoview.WebPushSubscription

class WebPushBridge(
    private val context: Context,
    private val runtime: GeckoRuntime,
) : WebPushDelegate {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val keyManager = WebPushRelayKeyManager(context)

    override fun onSubscribe(scopeName: String, appServerKey: ByteArray?): GeckoResult<WebPushSubscription> {
        val result = GeckoResult<WebPushSubscription>()

        if (!PushEnvironment.ensureFirebaseInitialized(context)) {
            result.completeExceptionally(
                IllegalStateException("Web Push is not configured. Add Firebase values and an HTTPS relay URL."),
            )
            return result
        }

        scope.launch {
            try {
                val subscription = createSubscription(scopeName, appServerKey)
                persistSubscription(scopeName, appServerKey)
                result.complete(subscription)
                Log.i(TAG, "Web Push subscription created for scope=$scopeName")
            } catch (t: Throwable) {
                Log.e(TAG, "Web Push subscription failed", t)
                result.completeExceptionally(t)
            }
        }

        return result
    }

    override fun onGetSubscription(scopeName: String): GeckoResult<WebPushSubscription> {
        val result = GeckoResult<WebPushSubscription>()
        val storedScope = prefs.getString(KEY_SCOPE, null)

        if (storedScope != scopeName) {
            result.complete(null)
            return result
        }

        if (!PushEnvironment.ensureFirebaseInitialized(context)) {
            result.complete(null)
            return result
        }

        scope.launch {
            try {
                result.complete(createSubscription(scopeName, readAppServerKey()))
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to restore Web Push subscription", t)
                result.completeExceptionally(t)
            }
        }

        return result
    }

    override fun onUnsubscribe(scopeName: String): GeckoResult<Void> {
        if (prefs.getString(KEY_SCOPE, null) == scopeName) {
            prefs.edit().remove(KEY_SCOPE).remove(KEY_APP_SERVER_KEY).apply()
        }
        val result = GeckoResult<Void>()
        result.complete(null)
        return result
    }

    /** Called by FirebaseMessagingService whenever FCM rotates the device token. */
    fun onFirebaseTokenChanged() {
        val storedScope = prefs.getString(KEY_SCOPE, null) ?: return
        mainHandler.post {
            Log.i(TAG, "FCM token changed; notifying Gecko subscription scope=$storedScope")
            runtime.webPushController.onSubscriptionChanged(storedScope)
        }
    }

    /** Feeds a decrypted Web Push payload back into Gecko's Service Worker machinery. */
    fun deliverPush(payload: ByteArray) {
        val storedScope = prefs.getString(KEY_SCOPE, null)
        if (storedScope == null) {
            Log.w(TAG, "Push received with no persisted Gecko service-worker scope; ignoring.")
            return
        }

        mainHandler.post {
            Log.i(TAG, "Delivering decrypted Web Push to Gecko scope=$storedScope bytes=${payload.size}")
            runtime.webPushController.onPushEvent(storedScope, payload)
        }
    }

    private suspend fun createSubscription(
        scopeName: String,
        appServerKey: ByteArray?,
    ): WebPushSubscription {
        val token = FirebaseMessaging.getInstance().token.await()
        val relayPath = WebPushRelay.path(token)
        val endpoint = "${PushEnvironment.relayBaseUrl(context)}/$relayPath"
        val publicKey = keyManager.getOrCreatePublicKey().toByteArray()
        val authSecret = keyManager.requireAuthSecret().toByteArray()

        return WebPushSubscription(
            scopeName,
            endpoint,
            appServerKey,
            publicKey,
            authSecret,
        )
    }

    private fun persistSubscription(scopeName: String, appServerKey: ByteArray?) {
        val editor = prefs.edit().putString(KEY_SCOPE, scopeName)
        if (appServerKey == null) {
            editor.remove(KEY_APP_SERVER_KEY)
        } else {
            editor.putString(KEY_APP_SERVER_KEY, encode(appServerKey))
        }
        editor.apply()
    }

    private fun readAppServerKey(): ByteArray? =
        prefs.getString(KEY_APP_SERVER_KEY, null)?.let(::decode)

    private fun encode(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    private fun decode(value: String): ByteArray =
        Base64.decode(value, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    companion object {
        private const val TAG = "WebPushBridge"
        private const val PREFS = "web_push_bridge"
        private const val KEY_SCOPE = "scope"
        private const val KEY_APP_SERVER_KEY = "app_server_key"
    }
}
