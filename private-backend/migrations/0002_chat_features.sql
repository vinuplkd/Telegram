ALTER TABLE users ADD COLUMN last_seen_at TEXT;

ALTER TABLE messages ADD COLUMN edited_at TEXT;
ALTER TABLE messages ADD COLUMN deleted_at TEXT;
ALTER TABLE messages ADD COLUMN delivered_at TEXT;
ALTER TABLE messages ADD COLUMN reply_to_id TEXT;

CREATE INDEX IF NOT EXISTS messages_recipient_unread_idx
  ON messages(recipient_id, read_at, created_at);

CREATE TABLE IF NOT EXISTS message_reactions (
  message_id TEXT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  reaction TEXT NOT NULL,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (message_id, user_id, reaction)
);

-- Enforce the two-account ceiling at database level. Existing two rows are retained.
CREATE TRIGGER IF NOT EXISTS users_max_two_insert
BEFORE INSERT ON users
WHEN (SELECT COUNT(*) FROM users) >= 2
BEGIN
  SELECT RAISE(ABORT, 'This private messenger is limited to two accounts');
END;
