# CBSE Edu Desk — Native Android + Cloudflare plan

The product is a native Android APK, not a web application. No custom domain is required; Cloudflare provides a workers.dev endpoint.

## Selected architecture

- Native Android: Kotlin + Jetpack Compose.
- Build: GitHub Actions cloud runner; no Android Studio installation required on the user's Windows PC.
- API: Cloudflare Worker.
- Persistent data: Cloudflare D1.
- Realtime wake-up notifications: SQLite-backed Cloudflare Durable Object WebSocket room.
- Encrypted attachments: private Cloudflare R2 bucket.
- Android API transport: OkHttp over HTTPS and WSS.
- Identity: exactly two manually provisioned server accounts; no public registration and no phone-number login.
- Local app lock: separate four-digit PIN per person. PIN must never be sent to the server or stored in plaintext.
- Message confidentiality: client-side end-to-end encryption is a required next implementation milestone. The backend currently stores ciphertext/nonce fields but the Android encryption/key-exchange implementation is not complete.

## Two-person access and security model

- Worker provisioning endpoint allows at most two accounts and is protected by a high-entropy bootstrap secret.
- After provisioning both accounts, delete the BOOTSTRAP_SECRET so the provisioning endpoint is disabled.
- Users authenticate with separately generated strong server passwords. Never use the 4-digit PIN as a server password.
- Session tokens are HMAC-signed and time-limited.
- D1 stores password hashes, user records, encrypted message envelopes and attachment metadata.
- R2 bucket is private; attachments are fetched through authenticated Worker routes.
- Never put TOKEN_SECRET, BOOTSTRAP_SECRET, or other server secrets in the APK or GitHub.
- The public repository currently contains development scaffolding. Before real private messages are used, complete E2EE, key verification/recovery, login rate limiting, session revocation and a security review.

## Free-plan limits

Cloudflare Free is not unlimited. Published limits include 100,000 Worker requests/day; D1 Free has 500 MB per database and 5 GB total account storage with daily read/write allowances; SQLite-backed Durable Objects have their own daily compute/request/storage allowances; R2 Standard includes 10 GB-month storage, 1 million Class A and 10 million Class B operations/month. Exceeding limits can cause requests or operations to fail until reset or require an upgrade. Confirm current limits in the official Cloudflare pricing/limits pages before launch.

## Setup required from the owner

1. Create a free Cloudflare account.
2. Enable Workers and the workers.dev subdomain; no custom domain purchase is needed.
3. Install Node.js LTS on the Windows computer.
4. From cloudflare-worker run npm install and npx wrangler login.
5. Create D1 database cbse-edu-desk and R2 bucket cbse-edu-desk-media.
6. Put the returned D1 database_id into wrangler.toml.
7. Apply migration 0001_initial.sql.
8. Set TOKEN_SECRET and BOOTSTRAP_SECRET using Wrangler secrets; deploy the Worker.
9. Provision exactly two accounts, then delete BOOTSTRAP_SECRET.
10. Set the deployed Worker URL as API_BASE_URL in the Android Gradle file and rebuild the APK.

Detailed commands are in cloudflare-worker/README.md.

## Product milestones

1. Complete backend deployment and two-account provisioning.
2. Implement Android login and securely store session tokens.
3. Implement local PIN creation/unlock, immediate relock on app background, and Android Keystore protection.
4. Implement and independently review end-to-end message encryption and device/key verification.
5. Complete chat UI, replies, editing/deleting, reactions, receipts, presence and message search.
6. Complete encrypted media/voice attachments.
7. Add push notifications with a secure delivery path.
8. Add voice/video calls later using WebRTC signalling and TURN relay.
9. Build signed release APK and test on both devices.

## Current status

- Cloudflare Workers + D1 + Durable Objects + R2 selected.
- Backend source scaffold committed in cloudflare-worker/.
- Native Kotlin app has an OkHttp API client scaffold.
- No Cloudflare account has been connected or Worker deployed yet.
- No D1 database or R2 bucket has been created yet.
- No user accounts have been provisioned.
- No production-ready APK or end-to-end encrypted messenger is complete yet.

## Telegram source note

The downloaded Telegram Android source is tightly coupled to Telegram MTProto, Telegram accounts, storage, push and calls. Renaming it does not connect it to Cloudflare. The independent messenger is being built as a separate native Kotlin Android app. The Telegram fork remains a UI/feature reference only; comply with its applicable open-source licence if reusing code.
