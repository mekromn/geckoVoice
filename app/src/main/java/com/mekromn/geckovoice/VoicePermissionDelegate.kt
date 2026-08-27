package com.mekromn.geckovoice

import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession

class VoicePermissionDelegate(
    private val activity: ComponentActivity,
) : GeckoSession.PermissionDelegate {
    private var pendingAndroidPermissionCallback: GeckoSession.PermissionDelegate.Callback? = null

    private val androidPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val callback = pendingAndroidPermissionCallback
        pendingAndroidPermissionCallback = null
        if (result.values.all { it }) callback?.grant() else callback?.reject()
    }

    override fun onAndroidPermissionsRequest(
        session: GeckoSession,
        permissions: Array<out String>,
        callback: GeckoSession.PermissionDelegate.Callback,
    ) {
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            callback.grant()
            return
        }

        if (pendingAndroidPermissionCallback != null) {
            callback.reject()
            return
        }

        pendingAndroidPermissionCallback = callback
        androidPermissionLauncher.launch(missing.toTypedArray())
    }

    override fun onContentPermissionRequest(
        session: GeckoSession,
        perm: GeckoSession.PermissionDelegate.ContentPermission,
    ): GeckoResult<Int> {
        val decision = if (isTrustedGoogleVoiceOrigin(perm.uri)) {
            GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW
        } else {
            GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY
        }
        return GeckoResult.fromValue(decision)
    }

    override fun onMediaPermissionRequest(
        session: GeckoSession,
        uri: String,
        video: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
        audio: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
        callback: GeckoSession.PermissionDelegate.MediaCallback,
    ) {
        if (!isTrustedGoogleVoiceOrigin(uri)) {
            callback.reject()
            return
        }

        callback.grant(video?.firstOrNull(), audio?.firstOrNull())
    }

    private fun isTrustedGoogleVoiceOrigin(uri: String): Boolean {
        val host = runCatching { Uri.parse(uri).host.orEmpty().lowercase() }.getOrDefault("")
        return host == "voice.google.com" || host.endsWith(".voice.google.com")
    }
}
