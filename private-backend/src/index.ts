interface Env {
  DB: D1Database;
  SESSION_SIGNING_SECRET: string;
}

interface UserRow {
  id: string;
  username: string;
  display_name: string;
  password_salt: string;
  password_hash: string;
  active: number;
}

const json = (data: unknown, status = 200) =>
  new Response(JSON.stringify(data), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" }
  });

const hex = (bytes: ArrayBuffer) =>
  [...new Uint8Array(bytes)].map(v => v.toString(16).padStart(2, "0")).join("");

async function sha256(value: string): Promise<string> {
  return hex(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value)));
}

async function passwordHash(password: string, salt: string): Promise<string> {
  const material = await crypto.subtle.importKey(
    "raw", new TextEncoder().encode(password), "PBKDF2", false, ["deriveBits"]
  );
  const bits = await crypto.subtle.deriveBits(
    { name: "PBKDF2", salt: new TextEncoder().encode(salt), iterations: 310000, hash: "SHA-256" },
    material, 256
  );
  return hex(bits);
}

function randomToken(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(32));
  return [...bytes].map(v => v.toString(16).padStart(2, "0")).join("");
}

async function currentUser(request: Request, env: Env): Promise<UserRow | null> {
  const token = request.headers.get("authorization")?.replace(/^Bearer\s+/i, "");
  if (!token) return null;
  const tokenHash = await sha256(token);
  const row = await env.DB.prepare(
    `SELECT u.id, u.username, u.display_name, u.password_salt, u.password_hash, u.active
     FROM sessions s JOIN users u ON u.id = s.user_id
     WHERE s.token_hash = ? AND s.expires_at > ? AND u.active = 1`
  ).bind(tokenHash, Math.floor(Date.now() / 1000)).first<UserRow>();
  return row ?? null;
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: {
        "access-control-allow-origin": "*",
        "access-control-allow-methods": "GET,POST,OPTIONS",
        "access-control-allow-headers": "content-type,authorization"
      }});
    }

    if (url.pathname === "/health" && request.method === "GET") {
      return json({ service: "CBSE Edu Desk Private API", status: "ok" });
    }

    if (url.pathname === "/auth/login" && request.method === "POST") {
      let input: { username?: string; password?: string };
      try { input = await request.json(); } catch { return json({ error: "Invalid JSON" }, 400); }
      if (!input.username || !input.password) return json({ error: "Username and password required" }, 400);

      const user = await env.DB.prepare(
        "SELECT * FROM users WHERE username = ? AND active = 1"
      ).bind(input.username.trim()).first<UserRow>();
      if (!user) return json({ error: "Invalid credentials" }, 401);

      const candidate = await passwordHash(input.password, user.password_salt);
      if (candidate !== user.password_hash) return json({ error: "Invalid credentials" }, 401);

      const token = randomToken();
      const tokenHash = await sha256(token);
      const expiresAt = Math.floor(Date.now() / 1000) + 60 * 60 * 24 * 30;
      await env.DB.prepare(
        "INSERT INTO sessions (token_hash, user_id, expires_at) VALUES (?, ?, ?)"
      ).bind(tokenHash, user.id, expiresAt).run();

      return json({ token, expiresAt, user: { id: user.id, username: user.username, displayName: user.display_name } });
    }

    if (url.pathname === "/auth/me" && request.method === "GET") {
      const user = await currentUser(request, env);
      if (!user) return json({ error: "Unauthorized" }, 401);
      return json({ id: user.id, username: user.username, displayName: user.display_name });
    }

    if (url.pathname === "/messages" && request.method === "GET") {
      const user = await currentUser(request, env);
      if (!user) return json({ error: "Unauthorized" }, 401);
      const rows = await env.DB.prepare(
        `SELECT id, sender_id AS senderId, recipient_id AS recipientId, body, created_at AS createdAt, read_at AS readAt
         FROM messages WHERE sender_id = ? OR recipient_id = ?
         ORDER BY created_at DESC LIMIT 100`
      ).bind(user.id, user.id).all();
      return json({ messages: rows.results.reverse() });
    }

    if (url.pathname === "/messages" && request.method === "POST") {
      const user = await currentUser(request, env);
      if (!user) return json({ error: "Unauthorized" }, 401);
      let input: { body?: string; recipientId?: string };
      try { input = await request.json(); } catch { return json({ error: "Invalid JSON" }, 400); }
      const body = input.body?.trim();
      if (!body || body.length > 10000 || !input.recipientId) return json({ error: "A recipient and message (up to 10,000 characters) are required" }, 400);

      const recipient = await env.DB.prepare(
        "SELECT id FROM users WHERE id = ? AND active = 1"
      ).bind(input.recipientId).first<{id:string}>();
      if (!recipient || recipient.id === user.id) return json({ error: "Recipient not found" }, 404);

      const id = crypto.randomUUID();
      await env.DB.prepare(
        "INSERT INTO messages (id, sender_id, recipient_id, body) VALUES (?, ?, ?, ?)"
      ).bind(id, user.id, recipient.id, body).run();
      return json({ id, senderId: user.id, recipientId: recipient.id, body }, 201);
    }

    return json({ error: "Not found" }, 404);
  }
};
