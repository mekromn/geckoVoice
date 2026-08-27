# Project status

## 2026-08-27 — v0.1 bootstrap

Target: prove background Google Voice Web Push and native Android notification delivery with GeckoView.

### Implemented

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
- ARM64-only Pixel validation package without rendering/runtime fidelity changes

### Build validation — PASSED

The Android scaffold has been compiled successfully by GitHub Actions.

Validated build stack:

- JDK 17
- Gradle 9.7.1
- Android Gradle Plugin 9.3.1
- compileSdk 37.1
- targetSdk 36
- Kotlin Gradle Plugin 2.4.10 with AGP built-in Kotlin
- GeckoView 154.0.20260814215756

Final clean ARM64 validation run:

- GitHub Actions run: `33107952565`
- result: `assembleDebug` PASS
- APK artifact upload: PASS
- source head used for the artifact: `9e638e01efd7cb6450bfbfd071be3fd3fd35db0b`
- validated fixes were squash-merged to `main` as `c67ccddfeb29132d44a41be9aa699bb740d12065`

### Current APK scope

The current APK is a real compiled ARM64 GeckoView build intended for Pixel-class Android devices. It can be used now to validate:

1. installation / launch
2. Google Voice rendering
3. Google sign-in
4. cookie/session persistence
5. Android/site permission behavior
6. microphone/camera calling basics

It does **not yet have live Web Push transport enabled**, because the repository intentionally contains placeholder Firebase/relay values rather than secrets or project-specific credentials.

### External prerequisites for real background push

- Firebase project / Android app registered for `com.mekromn.geckovoice`
- client-side Firebase values from that Android app (`google_app_id`, `project_id`, `google_api_key`)
- public HTTPS relay hostname
- Firebase service-account key installed only on the relay host and never committed or packaged in the APK

### Remaining validation gates

1. Install current APK and verify Google sign-in + Voice rendering.
2. Configure Firebase + HTTPS relay and rebuild.
3. Confirm Google Voice invokes `PushManager.subscribe` in GeckoView.
4. Confirm the relay-backed subscription endpoint is accepted by Google Voice.
5. Confirm a real Voice text/call/voicemail event reaches FCM while the UI is backgrounded/closed.
6. Confirm Gecko service worker receives the push and emits `WebNotification`.
7. Confirm Android notification content/tap behavior reaches Voice `notificationclick` handling.
8. Based on captured real Voice notification metadata, add native refinements such as separate channels, conversation grouping, direct reply, or call presentation only where the web app exposes enough information.

### Repository status

GitHub repository-content write access is working. The validated v0.1 build fixes are on `main`, and `PROJECT_STATUS.md` is the continuity checkpoint for the next iteration.
