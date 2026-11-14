# CBSE Edu Desk — Private Messenger API

This is a separate backend foundation for the private two-person messenger. It must **not** share the existing CBSE Edu Desk school API database.

## Implemented in this branch

- Private username/password login; there is no public sign-up route.
- Random bearer session tokens; only SHA-256 token hashes are stored in D1.
- Password hashing with PBKDF2-SHA-256.
- A database trigger that prevents more than two user accounts.
- Peer lookup for the other account.
- Message send/list, replies, edit and sender-side delete.
- Recipient-side read receipts.
- Basic reactions.
- Last-seen heartbeat endpoint.

## Not implemented yet

- End-to-end encryption. It is not a launch blocker, as requested. Traffic must use HTTPS/TLS, but the server can read message bodies stored in D1.
- Attachment upload/download, voice notes, push notifications, typing indicators, reliable online presence, and voice/video calling.
- Automated private account provisioning and rate limiting.
- Android integration with this API. The Telegram-derived Android client still uses its original Telegram networking/login stack until that is replaced. Do not distribute it as an independent messenger before this integration is complete.

## PIN and account security

The four-digit PIN is a **local app-lock PIN**, not the server password. Keep the server passwords separate and stronger. The Android client should store the PIN verifier and session token in Android Keystore-backed encrypted storage. The app should lock as soon as it enters the background. The Telegram-derived UI's existing passcode pause hook has been adjusted on the private-messenger branch to request an immediate lock, provided a passcode is configured.

## Cloudflare setup

1. Create a **new** Cloudflare D1 database named `cbse-edu-desk-private`.
2. Copy its database ID into `wrangler.toml`. Never bind this API to the existing school database.
3. In this folder, run `npm install`.
4. Apply migrations with `npx wrangler d1 migrations apply cbse-edu-desk-private --remote`.
5. Deploy with `npx wrangler deploy`.

No public registration endpoint exists. The two user rows must be provisioned privately by an administrator. Do not put PINs, passwords, or tokens in GitHub or chat.

## API routes

All routes except health and login require `Authorization: Bearer <token>`.

- `GET /health`
- `POST /auth/login` — JSON: `{"username":"...","password":"..."}`
- `GET /auth/me`
- `GET /users/peer`
- `POST /presence` — update own last-seen heartbeat
- `GET /presence` — fetch peer's last-seen timestamp
- `GET /messages` — latest 200 conversation messages
- `POST /messages` — JSON: `{"recipientId":"...","body":"...","replyToId":null}`
- `POST /messages/:id/read` — mark an incoming message as read
- `POST /messages/:id/reaction` — JSON: `{"reaction":"❤️"}`
- `PUT /messages/:id` — edit own message
- `DELETE /messages/:id` — soft-delete own message

## Next engineering milestones

1. Provision the two private accounts and deploy the isolated API.
2. Replace Telegram authentication/MTProto message transport with this API.
3. Connect the Android app's login, conversation list, send/receive, read receipts and local PIN lock.
4. Add encrypted R2 attachment storage, then push notifications.
5. Add voice/video calling after the messaging foundation is stable.
6. Consider optional end-to-end encryption later.
