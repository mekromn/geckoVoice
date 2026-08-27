# geckoVoice architecture

## Goal

Provide the Google Voice web application in an Android shell without the primary weakness of a normal Android WebView wrapper: missing standards-compliant background Web Push and Web Notifications.

## Flow

```text
voice.google.com
  │
  │ PushManager.subscribe(applicationServerKey)
  ▼
GeckoView WebPushDelegate
  │
  │ returns PushSubscription:
  │   endpoint = https://relay.example/wpush/<firebase-project>/<fcm-token>
  │   p256dh    = Android Keystore public key
  │   auth      = local 16-byte auth secret
  ▼
Google Voice push sender
  │
  │ RFC 8030 / RFC 8291 encrypted WebPush POST
  ▼
webpush-fcm-relay (HTTPS)
  │
  │ forwards ciphertext through FCM
  ▼
VoiceFirebaseMessagingService
  │
  │ decrypts with Android Keystore private key + auth secret
  ▼
GeckoRuntime.webPushController.onPushEvent(scope, payload)
  │
  ▼
Google Voice Service Worker `push` event
  │
  │ showNotification(...)
  ▼
Gecko WebNotificationDelegate
  │
  ▼
Android NotificationManager
```

## Why not Android WebView?

Android WebView does not expose the full Push API / Web Notification embedding path needed here. GeckoView explicitly exposes `WebPushController`, `WebPushDelegate`, `WebPushSubscription`, `WebNotification`, and `WebNotificationDelegate` to embedders.

## Why not Mozilla Autopush?

Mozilla Android Components can bridge Gecko Web Push through Mozilla Autopush and FCM, but its FCM sender/project must be configured on the Autopush server. A third-party APK cannot assume Mozilla's production server has credentials for an arbitrary Firebase project. The small generic relay in this project gives us our own standards-compliant endpoint instead.

## Process lifetime

`GeckoRuntime` lives in `GeckoVoiceApplication` and is only created in the app's main process. This matters because Gecko child processes also instantiate the Android `Application` class and a second runtime must not be created there.

FCM can start the main app process while the UI is closed. `VoiceFirebaseMessagingService` therefore has access to the process-lifetime Gecko runtime and can deliver a push event without opening the visible Google Voice page first.

## Persistent state

- Gecko profile/cookies: GeckoView app profile, persistent across launches.
- Push service-worker scope: SharedPreferences.
- VAPID application-server key: SharedPreferences.
- P-256 private key: Android Keystore.
- WebPush auth secret: relay client's SharedPreferences secret store.
- FCM token: managed by Firebase Messaging.

## Token rotation

When FCM rotates the token, `VoiceFirebaseMessagingService.onNewToken()` calls `WebPushController.onSubscriptionChanged(scope)`. Gecko can then tell the Google Voice service worker that its PushSubscription changed so the site can update its server-side subscription.
