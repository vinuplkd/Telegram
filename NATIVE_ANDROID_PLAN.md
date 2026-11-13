# CBSE Edu Desk — Native Android + Firebase plan

This is the selected architecture. The product is a native Android APK, not a web app. A custom domain is not required.

## Selected stack

- Android: native Kotlin + Jetpack Compose.
- Build: GitHub Actions cloud runner; no Android Studio installation required on the user's Windows PC.
- Authentication: Firebase Authentication with two administratively created email/password identities. No phone authentication and no public sign-up UI.
- Database/realtime: Cloud Firestore.
- Attachments: Firebase Storage, with strict per-user/conversation access rules.
- Push: Firebase Cloud Messaging (FCM).
- Local app lock: individual 4-digit PINs, stored only as a locally verified unlock factor. The PIN must never be sent to Firebase or stored in plaintext.
- Device key protection: Android Keystore; derive/unlock local encryption keys using a suitable password KDF and enforce retry throttling.
- Message confidentiality: client-side end-to-end encryption. Firebase TLS, Authentication and Security Rules are not themselves end-to-end encryption.

## Two-person access model

- Create exactly two Firebase Auth users manually in the Firebase Console.
- Do not implement public sign-up or account creation in the app.
- Store an explicit allowlist of the two Firebase UIDs in trusted backend configuration / Firestore Security Rules.
- Rules must deny all other users and prevent either account from reading or writing unrelated conversations.
- Never put Firebase Admin SDK credentials or service-account private keys in the APK or GitHub.
- Firebase client configuration is not a secret, but Security Rules and Authentication are mandatory; hiding a UI button is not access control.

## Firebase setup required from the owner

1. Create a Firebase project named CBSE Edu Desk.
2. Add an Android app. The final application ID will be selected in the native project before registration; do not register an arbitrary package ID yet.
3. Enable Email/Password sign-in in Authentication.
4. Create the two approved user accounts manually after the app/backend policy is ready.
5. Enable Cloud Firestore.
6. Enable Cloud Storage if image/video/file/voice attachments are required. Check current Firebase billing and storage requirements in the Console before enabling; do not assume the free tier covers all storage use.
7. Enable Cloud Messaging for notifications.
8. Download google-services.json only after the final Android package ID has been chosen. Keep it out of public repositories; configure it as a protected GitHub Actions secret or add it to a private repository with appropriate care. Never upload service-account JSON.

## Product requirements

- Two users only; no phone-number login.
- Separate PIN per user; immediate relock whenever the app leaves foreground.
- Telegram-inspired chat UI with distinct CBSE Edu Desk identity.
- Text, reply, edit/delete, reactions, delivery/read states, presence/last seen.
- Encrypted media, files and voice messages.
- Push notifications.
- Voice/video calls later, using WebRTC with a signalling channel and TURN relay. Firebase alone does not provide a complete calling system.

## Critical implementation note

The downloaded Telegram Android source is tightly coupled to Telegram's MTProto protocol, Telegram accounts, Telegram storage, push and call infrastructure. Changing its name or login screen does not connect it to Firebase. The independent messenger should be a separate native Kotlin Android application. Keep the Telegram fork as a UI/feature reference only and comply with its applicable open-source licence if any code is reused.

## Current status

- Firebase selected as backend.
- Custom domain not required.
- No Firebase project is connected yet.
- No Firebase credentials or user accounts have been created.
- No independent CBSE Edu Desk APK has been built yet.
- The previously created Matrix/Synapse scaffold is superseded and is not the selected backend.
