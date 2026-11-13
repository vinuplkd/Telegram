# CBSE Edu Desk — Native Android direction (supersedes Matrix hosting draft)

The product is a native Android APK. It is not a website or a web app.

## Decision

Do not require the user to buy or configure a custom domain. Use a managed Supabase project as the private data/realtime backend. Supabase provides a project HTTPS endpoint; the Android application remains a native Kotlin application installed directly on the two phones.

The earlier self-hosted Matrix/Synapse + PostgreSQL scaffold is not the selected deployment route and must not be treated as deployed or ready for use.

## Native app architecture

- Android client: Kotlin + Jetpack Compose, built as an APK using GitHub Actions.
- Backend: Supabase Auth (provisioned identities only), PostgreSQL, Realtime and private Storage.
- Exactly two approved accounts. Disable public sign-up; create the two users administratively.
- No phone-number login. Use non-phone credentials/device sessions for backend authentication.
- Four-digit PIN is only a local app lock. Never send it to Supabase or store it in plaintext. Protect locally encrypted key material using Android Keystore, use a memory-hard KDF and rate-limit retries.
- App locks as soon as it leaves the foreground and requires PIN on resume.
- Message contents and attachments must be encrypted on the client before upload; Supabase transport/storage alone is not end-to-end encryption.
- Enforce database row-level security so each user can access only the private conversation and its own authorised records. No client-side-only access restrictions.
- Push notifications and reliable background delivery require separate platform setup; calling requires a signalling service and TURN relay. These are later milestones.

## Critical scope correction

The downloaded Telegram client is tightly coupled to Telegram accounts, MTProto, Telegram storage, push and call services. Changing its name does not convert it into a client for Supabase. The independent app should be a separate native Android project rather than a superficial Telegram rebrand. Keep the Telegram fork only as a UI/feature reference and preserve its original branch.

## Required setup input

To connect the native Android app to Supabase, the owner must provide the Supabase project URL and publishable/anon client key (never the service-role key). The project should be dedicated to this app. Do not put database passwords, service-role keys or user passwords into the APK or GitHub.

## Milestones

1. Create a clean native Kotlin Android project and cloud APK build.
2. Connect the Supabase project using the public client key and strict RLS.
3. Provision exactly two users and disable public sign-up.
4. Implement local PIN lock and Keystore-backed key protection.
5. Implement encrypted one-to-one messages, receipts and media.
6. Add presence and push.
7. Add calls using signalling plus TURN.
8. Test, sign and distribute release APK.

## Current status

This is an architecture decision and implementation plan only. No Supabase project has been connected, no users have been created, and no independent messenger APK has been built yet.
