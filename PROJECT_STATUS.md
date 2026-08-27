# Project status

## 2026-08-27 — v0.1 bootstrap

Target: prove background Google Voice Web Push and native Android notification delivery with GeckoView.

### Implemented in scaffold

- GeckoView process-lifetime runtime
- dedicated Google Voice session
- persistent browser profile/cookies
- Android notification permission request
- Google Voice site permission bridge
- microphone/camera media selection
- WebPushDelegate subscription implementation
- Firebase token → standards WebPush relay endpoint
- Android Keystore P-256 key material
- WebPush auth secret persistence
- FCM message reception and local decryption
- decrypted payload → Gecko WebPushController
- Gecko WebNotification → Android notification
- notification tap/dismiss callbacks
- FCM token-rotation handling
- GitHub Actions debug APK build workflow
- relay Docker/Caddy deployment template

### External prerequisites for real push

- Firebase project / Android app
- public HTTPS relay hostname
- Firebase service-account key installed only on the relay host

### Validation gates

1. Compile scaffold in GitHub Actions.
2. Install APK and verify Google sign-in + Voice rendering.
3. Confirm Google Voice invokes PushManager.subscribe in GeckoView.
4. Confirm returned relay endpoint is accepted by Google Voice.
5. Confirm a real Voice event reaches FCM while UI is backgrounded.
6. Confirm Gecko service worker emits WebNotification.
7. Confirm Android notification tap reaches Voice notificationclick handling.

### Repository status

GitHub repository-content write access was restored on 2026-08-27. The bootstrap is now tracked directly in `mekromn/geckoVoice`; GitHub Actions is the compile-validation gate.
