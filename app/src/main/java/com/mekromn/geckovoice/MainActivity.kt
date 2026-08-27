package com.mekromn.geckovoice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

class MainActivity : ComponentActivity() {
    private lateinit var geckoView: GeckoView
    private lateinit var app: GeckoVoiceApplication
    private var canGoBack = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Gecko's content permission is handled separately by VoicePermissionDelegate. */ }

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (canGoBack) {
                app.session.goBack()
            } else {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as GeckoVoiceApplication

        geckoView = GeckoView(this)
        setContentView(geckoView)

        app.session.permissionDelegate = VoicePermissionDelegate(this)
        app.session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                this@MainActivity.canGoBack = canGoBack
            }
        }

        geckoView.setSession(app.session)
        app.loadVoiceIfNeeded()
        onBackPressedDispatcher.addCallback(this, backCallback)

        requestAndroidNotificationPermissionIfNeeded()
        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        app.session.setActive(true)
        app.session.setFocused(true)
    }

    override fun onStop() {
        app.session.setFocused(false)
        app.session.setActive(false)
        super.onStop()
    }

    override fun onDestroy() {
        if (::geckoView.isInitialized) {
            geckoView.releaseSession()
        }
        super.onDestroy()
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val key = intent?.getStringExtra(EXTRA_NOTIFICATION_KEY) ?: return
        app.notificationBridge.onTapped(key)
        intent.removeExtra(EXTRA_NOTIFICATION_KEY)
    }

    private fun requestAndroidNotificationPermissionIfNeeded() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        const val EXTRA_NOTIFICATION_KEY = "com.mekromn.geckovoice.NOTIFICATION_KEY"
    }
}
