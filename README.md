# geckoVoice

A dedicated Android Google Voice web client built on **Mozilla GeckoView**, with a real **Web Push → Firebase Cloud Messaging → Gecko Service Worker → native Android notification** bridge.

The core goal is simple: keep the Google Voice web experience, but stop needing to manually reopen/refresh the site to discover new texts, missed calls, or voicemail.

## Status

This is the v0.1 bootstrap focused on proving the hardest part first:

- GeckoView shell for `https://voice.google.com/`
- persistent Gecko cookies/profile, so Google sign-in survives app restarts
- Android 13+ notification permission handling
- microphone/camera permission bridge for Google Voice calling
- Gecko `WebPushDelegate` implementation
- standards-compliant `PushSubscription` endpoint backed by FCM
- local P-256 WebPush key in Android Keystore
- local RFC WebPush payload decryption
- background FCM service feeding decrypted pushes to Gecko's Service Worker
- Gecko `WebNotificationDelegate` → Android `NotificationManager`
- notification tap → Gecko `WebNotification.click()` when the originating process object is still available
- FCM token-rotation → Gecko subscription-change notification
- no periodic polling loop
- GitHub Actions debug APK build

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the full data flow.

## Push transport setup

The source builds with placeholder push values, but real background push needs **your own Firebase Android app and a small HTTPS relay**.

### 1. Firebase Android app

Create a Firebase Android app. The release package ID is:

```text
com.mekromn.geckovoice
```

Debug and release intentionally use that same package ID for the first validation phase, so one Firebase Android-app registration covers both builds.

From Firebase / `google-services.json`, replace the placeholders in:

```text
app/src/main/res/values/push_config.xml
```

Required values:

```xml
<string name="google_app_id">...</string>
<string name="project_id">...</string>
<string name="google_api_key">...</string>
<string name="gecko_voice_relay_base_url">https://voice-push.example.com</string>
```

The helper `tools/write-push-config.sh` can generate this file from environment variables.

**Do not put a Firebase service-account private key in the APK or Git repository.**

### 2. Relay

The relay setup is in [`relay/`](relay/README.md). It uses `evant/webpush-fcm-relay` plus Caddy for HTTPS.

The relay receives the same standards-compliant encrypted Web Push request a browser push service would receive. It does not need to know the Google Voice payload contents; it forwards ciphertext through FCM, and the phone decrypts it locally.

## Build

Requirements:

- JDK 17
- Gradle 8.13
- Android SDK 36

Build locally:

```bash
gradle :app:assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

The included GitHub Actions workflow does the same and uploads the APK as a workflow artifact.

## First validation sequence

1. Configure Firebase + relay.
2. Build/install the APK.
3. Launch Gecko Voice and sign in to Google Voice.
4. Grant Android notification permission.
5. Allow the Google Voice website notification permission when it subscribes.
6. Verify the relay receives a Web Push subscription endpoint request path from the app.
7. Background/close the UI.
8. Send a text to the Google Voice number from another number.
9. Confirm FCM wakes the app process, the payload reaches Gecko's service worker, and an Android notification appears.
10. Tap the notification and verify Google Voice handles the notification click/focus path.

## Deliberate v0.1 limits

This bootstrap does not yet claim perfect parity with Google's private native Voice app. The web application decides what push events and notification metadata exist. Native direct-reply buttons, telecom-framework incoming call UI, conversation-specific notification channels, and exact deep-link behavior should be added only after we capture what Google Voice actually emits in the first push-validation build.

The design intentionally avoids scraping internal Google Voice APIs or running a wasteful 10-minute polling worker. The preferred path is the website's own standards-based Service Worker and Web Push behavior.
