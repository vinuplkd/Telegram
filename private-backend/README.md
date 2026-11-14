# CBSE Edu Desk — Private Messenger API (starter)

This is a separate backend for the private two-person messenger. It must **not** share the existing CBSE Edu Desk school API database.

## Current scope

- Private username/password login (no public registration endpoint)
- 30-day bearer sessions; only a SHA-256 hash of each session token is stored
- Authenticated message send/list endpoints
- D1 schema designed for two manually provisioned accounts

## Important security status

This is an early API foundation, **not production-ready messaging**. Message bodies are currently stored as ordinary text in D1. End-to-end encryption, encrypted attachments, delivery/read receipts, push notifications, rate limiting, account provisioning and key recovery are not implemented yet. Do not use it for confidential conversations until end-to-end encryption is implemented and independently reviewed.

The four-digit app PIN belongs on the Android device as a local app-lock credential. It must not be used as the server password. Use a separate, stronger password for each server account.

## Cloudflare setup

1. Create a **new** Cloudflare D1 database named `cbse-edu-desk-private`.
2. Copy its database ID into `wrangler.toml`.
3. In this folder, run `npm install`.
4. Apply the schema with `npx wrangler d1 migrations apply cbse-edu-desk-private --remote`.
5. Set a long random secret using `npx wrangler secret put SESSION_SIGNING_SECRET` (reserved for the next token-signing hardening step; do not put secrets in Git).
6. Deploy with `npx wrangler deploy`.

The two user rows must be provisioned privately with password hashes; there is intentionally no public sign-up route. Do not paste passwords or PINs into GitHub, source files, or chat.

## API routes

- `GET /health`
- `POST /auth/login` with JSON `{"username":"...","password":"..."}`
- `GET /auth/me` with `Authorization: Bearer <token>`
- `GET /messages` with bearer token
- `POST /messages` with JSON `{"recipientId":"...","body":"..."}` and bearer token

## Next engineering milestones

1. Secure private provisioning for exactly two accounts and enforce the two-account cap.
2. End-to-end encryption before message persistence.
3. Android login/session integration and immediate four-digit PIN lock on background.
4. Attachments via a dedicated private R2 bucket with encrypted objects.
5. Delivery/read receipts, presence, push notifications, then voice/video calling.
