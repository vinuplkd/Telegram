# CBSE Edu Desk Cloudflare backend

This is the private API backend for the native Android APK. It is not a web application. The Android app calls the Worker API; users do not open a website.

## Services used

- Workers: authenticated HTTPS API.
- D1: two user records, encrypted message envelopes and attachment metadata.
- Durable Objects (SQLite-backed): WebSocket notifications for the one private conversation.
- R2: private storage for encrypted attachments.

A custom domain is not required. Cloudflare provides a `workers.dev` URL after deployment.

## Free-plan caveats

Cloudflare Free is usage-limited, not unlimited. Current published limits include 100,000 Workers requests/day; D1 Free has 500 MB per database, 5 GB total account storage, and daily row read/write limits; SQLite-backed Durable Objects have daily request, compute, and storage allowances; R2 Standard includes 10 GB-month storage, 1 million Class A and 10 million Class B operations monthly. Exceeding free limits can cause requests/operations to fail until reset or require an upgrade. Review the official pages before launch:

- https://developers.cloudflare.com/workers/platform/limits/
- https://developers.cloudflare.com/d1/platform/limits/
- https://developers.cloudflare.com/durable-objects/platform/pricing/
- https://developers.cloudflare.com/r2/pricing/

## Prerequisites

1. Create a free Cloudflare account and enable Workers.
2. Install Node.js LTS on the development computer.
3. From this directory run `npm install`.
4. Authenticate the CLI with `npx wrangler login`.

## Create resources

Run these commands from this directory:

```bash
npx wrangler d1 create cbse-edu-desk
npx wrangler r2 bucket create cbse-edu-desk-media
```

Copy the D1 `database_id` returned by Wrangler into `wrangler.toml`, replacing `REPLACE_AFTER_CREATING_D1`.

Apply the database migration:

```bash
npx wrangler d1 migrations apply cbse-edu-desk --remote
```

Set high-entropy secrets (do not commit them):

```bash
npx wrangler secret put TOKEN_SECRET
npx wrangler secret put BOOTSTRAP_SECRET
```

Use separate random values of at least 32 bytes. The bootstrap secret is only for creating the two approved accounts.

Deploy:

```bash
npx wrangler deploy
```

Wrangler will print a URL similar to `https://cbse-edu-desk-api.<your-account>.workers.dev`. Enable the `workers.dev` route for the Worker if prompted.

## Provision exactly two accounts

Call `POST /v1/admin/provision` twice using the bootstrap secret. Each password must be a long random server credential (at least 16 characters). Do not use the 4-digit app PIN as the password. Example using PowerShell after deployment:

```powershell
$base = "https://YOUR-WORKER.YOUR-SUBDOMAIN.workers.dev"
$headers = @{ "X-Bootstrap-Secret" = "YOUR_BOOTSTRAP_SECRET" }
$body = @{ username = "userone"; password = "PASTE_A_LONG_RANDOM_PASSWORD" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$base/v1/admin/provision" -Headers $headers -ContentType "application/json" -Body $body
```

Repeat for the second user with a different username and password. The endpoint refuses to create a third account. After both users are provisioned, remove the bootstrap secret:

```bash
npx wrangler secret delete BOOTSTRAP_SECRET
```

This disables further account provisioning until an administrator deliberately adds the secret again.

## Native Android connection

The Android app uses package ID `com.cbsedudesk.mobile`. Update `API_BASE_URL` in `native-app/app/build.gradle.kts` to the deployed Worker URL, then build the APK through the GitHub Actions workflow.

The Android client uses the two provisioned usernames/passwords for server login. The separate 4-digit PIN is local-only and must never be sent to the Worker.

## Security status and remaining work

This is a development scaffold, not a production-ready messenger.

- Passwords are stored as PBKDF2-HMAC-SHA256 hashes with random salts.
- Sessions are HMAC-signed and expire.
- The provision endpoint caps the user table at two accounts.
- Messages are designed to store ciphertext and nonce only; client-side end-to-end encryption and key verification still need to be implemented before real private content is sent.
- The attachment endpoint stores opaque encrypted bytes in a private R2 bucket; client-side encryption must be implemented before upload.
- WebSocket notifications are only wake-up hints; the client must fetch messages over the authenticated API.
- Add login rate limiting, device/session revocation, read/delivery receipt APIs, key backup/recovery, push notifications, security review and robust monitoring before production.
- Do not expose R2 publicly. Do not put Worker secrets in the APK, GitHub source, or screenshots.
- Voice/video calls are a later phase and need WebRTC signalling plus TURN infrastructure.
