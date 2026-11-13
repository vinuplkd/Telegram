# CBSE Edu Desk — Native Android + Supabase plan

This is the selected architecture. The product is a native Android APK, not a web app. A custom domain is not required.

## Selected stack

- Android: native Kotlin + Jetpack Compose.
- Build: GitHub Actions cloud runner; no Android Studio installation required on the user's Windows PC.
- Backend: managed Supabase project; the project-provided HTTPS endpoint is used, so no custom domain is needed.
- Authentication: Supabase Auth with exactly two administratively provisioned email/password identities. No phone authentication and no public sign-up UI.
- Database/realtime: PostgreSQL + Supabase Realtime.
- Attachments: private Supabase Storage bucket with strict access policies.
- Push: Android push notifications require a separate delivery integration; do not assume Supabase Realtime alone delivers notifications while the app is closed.
- Local app lock: individual 4-digit PINs, stored only as a locally verified unlock factor. The PIN must never be sent to Supabase or stored in plaintext.
- Device key protection: Android Keystore; derive/unlock local encryption keys using a suitable password KDF and enforce retry throttling.
- Message confidentiality: client-side end-to-end encryption. HTTPS, Supabase Auth and Row Level Security are not themselves end-to-end encryption.

## Two-person access model

- Create exactly two Supabase Auth users administratively.
- Disable public sign-ups; do not implement public account creation in the app.
- Use database Row Level Security policies that explicitly authorise only the two approved user IDs and only their shared conversation.
- Use private Storage buckets and policies that restrict media access to those two users.
- Never put the Supabase service-role key in the APK or GitHub. The publishable/anon key may be embedded in the client, but only alongside strict RLS policies.
- The 4-digit PIN is not the network password or server credential.

## Supabase setup required from the owner

1. Create a Supabase project named CBSE Edu Desk.
2. Keep the generated project URL; no custom domain is required.
3. Enable email/password authentication and disable public sign-ups.
4. Create the two approved users administratively only after the security policies are ready.
5. Create the private conversation and message schema.
6. Enable Realtime for the required message tables.
7. Create a private Storage bucket and policies for attachments.
8. Provide the project URL and publishable key for the Android client. Never provide the service-role key.
9. Check current plan limits and backups before relying on it for long-term storage.

## Product requirements

- Two users only; no phone-number login.
- Separate PIN per user; immediate relock whenever app leaves foreground.
- Telegram-inspired chat UI with distinct CBSE Edu Desk identity.
- Text, reply, edit/delete, reactions, delivery/read states, presence/last-seen.
- Encrypted media, files and voice messages.
- Push notifications.
- Voice/video calls later, using WebRTC with signalling plus TURN relay.

## Critical implementation note

The downloaded Telegram Android source is tightly coupled to Telegram's MTProto protocol, Telegram accounts, Telegram storage, push and call infrastructure. Changing its name or login screen does not connect it to Supabase. The independent messenger is a separate native Kotlin Android application. Keep the Telegram fork as a UI/feature reference only and comply with its applicable open-source licence if any code is reused.

## Current status

- Firebase rejected because the user's Firebase account/project capacity is full.
- Supabase selected as the backend.
- Custom domain not required.
- No Supabase project is connected yet.
- No user credentials have been created.
- No independent production-ready CBSE Edu Desk APK has been built yet.
- Earlier Matrix/Synapse scaffold and Firebase dependencies are superseded.
