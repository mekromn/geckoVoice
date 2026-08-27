package com.mekromn.geckovoice

import android.app.Application
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession

class GeckoVoiceApplication : Application() {
    lateinit var runtime: GeckoRuntime
        private set

    lateinit var notificationBridge: WebNotificationBridge
        private set

    lateinit var webPushBridge: WebPushBridge
        private set

    private val voiceLoaded = AtomicBoolean(false)

    val session: GeckoSession by lazy(LazyThreadSafetyMode.NONE) {
        GeckoSession().also { it.open(runtime) }
    }

    override fun onCreate() {
        super.onCreate()

        // Gecko child processes also create the Android Application object. Never create a second
        // GeckoRuntime there; GeckoRuntime is intentionally process-lifetime in the main app process.
        if (Application.getProcessName() != packageName) {
            return
        }

        PushEnvironment.ensureFirebaseInitialized(this)

        val settings = GeckoRuntimeSettings.Builder()
            .remoteDebuggingEnabled(BuildConfig.DEBUG)
            .build()

        runtime = GeckoRuntime.create(this, settings)
        runtime.warmUp()

        notificationBridge = WebNotificationBridge(this, runtime)
        webPushBridge = WebPushBridge(this, runtime)
        runtime.webPushController.delegate = webPushBridge

        Log.i(TAG, "GeckoVoice runtime initialized; pushConfigured=${PushEnvironment.isConfigured(this)}")
    }

    fun loadVoiceIfNeeded() {
        if (voiceLoaded.compareAndSet(false, true)) {
            session.loadUri(VOICE_URL)
        }
    }

    companion object {
        private const val TAG = "GeckoVoiceApp"
        const val VOICE_URL = "https://voice.google.com/"
    }
}
