# CBSE Edu Desk — Private Messenger Engineering Plan

## Product requirement

CBSE Edu Desk is a private, two-person Android messenger. The school-themed name and icon are a visual disguise only; the app must not claim to be an official CBSE product or use CBSE trademarks.

## Non-negotiable requirements

- Exactly two accounts, provisioned by the owner; no public registration.
- No phone-number login and no Telegram account dependency.
- Each person unlocks the app with their own four-digit PIN.
- Lock immediately when the app leaves the foreground; require PIN on return.
- Private one-to-one text and media messaging, delivery/read receipts, presence/last-seen, replies, edits/deletes, reactions, attachments and voice messages.
- Voice/video calls are a later milestone after reliable messaging.
- End-to-end encryption for message content and attachments.

## Security model

A four-digit PIN is an app-unlock secret, not a suitable network identity or server credential. The PIN must never be sent to the server or stored in plaintext. The app should derive/unlock a local key using a memory-hard KDF, enforce retry throttling, and store key material in Android Keystore. Server authentication must use separately provisioned, revocable device credentials. Both users must be provisioned out-of-band by the owner; public registration must remain disabled.

## Architecture decision

The Telegram Android source is GPL-licensed and implements Telegram's MTProto protocol, Telegram account authentication, Telegram-specific storage and push/call infrastructure. Renaming the app or changing LoginActivity does not replace those dependencies. A true independent service requires replacing the protocol, account/authentication layer, message sync, media storage, push notifications, and call signalling/relay infrastructure.

Therefore:
1. Keep this repository as the Telegram UI/reference fork and preserve the successful original build on `master`.
2. Do not present a renamed Telegram login client as the requested independent messenger.
3. Build the independent client/backend as a separate implementation with a documented API and two pre-provisioned identities.
4. Reuse only components whose licences and technical interfaces permit it; retain required GPL notices/source obligations for any redistributed Telegram-derived code.

## Delivery milestones

1. Confirm the independent backend and hosting model.
2. Provision two identities and implement device authentication.
3. Implement the Android PIN lock and foreground/background relock.
4. Implement encrypted one-to-one messaging and message state.
5. Add encrypted media, attachments and voice messages.
6. Add push notifications and presence.
7. Add voice/video calling with TURN infrastructure.
8. Security review, backup/recovery plan, signed release APK.

## Current repository state

- `master`: original Telegram source and successful debug build workflow.
- `private-messenger`: working branch; display label changed to CBSE Edu Desk; cloud APK workflow enabled.
- The current APK is still a Telegram client. The independent two-account authentication and backend described above are not implemented yet.
