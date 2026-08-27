# WebPush → FCM relay

`geckoVoice` needs a public HTTPS endpoint because a GeckoView embedder does not automatically inherit Firefox's production Mozilla Autopush transport.

This directory uses [`evant/webpush-fcm-relay`](https://github.com/evant/webpush-fcm-relay). It accepts standard encrypted Web Push requests from Google Voice and forwards the still-encrypted message through your Firebase Cloud Messaging project. The APK decrypts the payload locally with its Android Keystore key and auth secret, then hands the plaintext push payload to Gecko's `WebPushController`.

## Setup

1. Create a Firebase project and Android app for `com.mekromn.geckovoice`. Debug and release intentionally share that package ID during the first validation phase.
2. Enable Cloud Messaging.
3. Create a Firebase service-account key JSON for the relay server.
4. Put that JSON in `relay/credentials/`. **Never commit this directory.**
5. Copy `.env.example` to `.env` and set a DNS hostname that points to the relay host.
6. Run `docker compose up -d`.
7. Caddy obtains/renews TLS automatically when DNS and ports 80/443 are reachable.
8. Put the resulting `https://your-hostname` into `gecko_voice_relay_base_url` in the Android push config.

The relay server's Firebase private key stays server-side. The APK only contains normal Firebase client identifiers/API key plus the relay URL.
