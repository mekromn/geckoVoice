package com.mekromn.geckovoice

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp

object PushEnvironment {
    private const val PLACEHOLDER = "REPLACE_ME"
    private const val TAG = "PushEnvironment"

    fun isConfigured(context: Context): Boolean {
        val appId = context.getString(R.string.google_app_id)
        val projectId = context.getString(R.string.project_id)
        val apiKey = context.getString(R.string.google_api_key)
        val relay = relayBaseUrl(context)

        return appId.isRealValue() &&
            projectId.isRealValue() &&
            apiKey.isRealValue() &&
            relay.startsWith("https://") &&
            !relay.contains("REPLACE-ME", ignoreCase = true)
    }

    fun relayBaseUrl(context: Context): String =
        context.getString(R.string.gecko_voice_relay_base_url).trim().trimEnd('/')

    fun ensureFirebaseInitialized(context: Context): Boolean {
        if (!isConfigured(context)) {
            Log.w(TAG, "Firebase/WebPush relay values are placeholders; Web Push subscriptions will be rejected.")
            return false
        }

        return try {
            FirebaseApp.getInstance()
            true
        } catch (_: IllegalStateException) {
            FirebaseApp.initializeApp(context) != null
        }
    }

    private fun String.isRealValue(): Boolean =
        isNotBlank() && !equals(PLACEHOLDER, ignoreCase = true) && !contains("REPLACE_ME", ignoreCase = true)
}
