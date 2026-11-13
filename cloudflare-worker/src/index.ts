import { ChatRoom } from "./chat-room";
import { authenticate, encodeSecret, hashPassword, issueToken, randomBytes, verifyPassword } from "./security";
import type { Env } from "./types";
import { HttpError } from "./types";

export { ChatRoom };

const json = (data: unknown, status = 200) =>
  new Response(JSON.stringify(data), { status, headers: { "Content-Type": "application/json", "Cache-Control": "no-store" } });

async function body<T>(request: Request): Promise<T> {
  try { return await request.json<T>(); }
  catch { throw new HttpError(400, "Invalid JSON body"); }
}

function cors(request: Request, response: Response): Response {
  const headers = new Headers(response.headers);
  headers.set("Access-Control-Allow-Origin", request.headers.get("Origin") ?? "*");
  headers.set("Access-Control-Allow-Headers", "Authorization, Content-Type");
  headers.set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
  headers.set("Vary", "Origin");
  return new Response(response.body, { status: response.status, headers });
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    try {
      if (request.method === "OPTIONS") return cors(request, new Response(null, { status: 204 }));
      const url = new URL(request.url);
      let response: Response;

      if (url.pathname === "/health" && request.method === "GET") {
        response = json({ app: env.APP_NAME, status: "ok" });
      } else if (url.pathname === "/v1/admin/provision" && request.method === "POST") {
        if (!env.BOOTSTRAP_SECRET || request.headers.get("X-Bootstrap-Secret") !== env.BOOTSTRAP_SECRET) {
          throw new HttpError(403, "Provisioning not authorised");
        }
        const input = await body<{ username: string; password: string }>(request);
        const username = input.username?.trim();
        if (!username || !/^[a-zA-Z0-9._-]{3,32}$/.test(username)) throw new HttpError(400, "Invalid username");
        if (!input.password || input.password.length < 16) throw new HttpError(400, "Use a random password of at least 16 characters");
        const count = await env.DB.prepare("SELECT COUNT(*) AS count FROM users").first<{ count: number }>();
        const existing = await env.DB.prepare("SELECT id FROM users WHERE username = ?").bind(username).first();
        if (!existing && (count?.count ?? 0) >= 2) throw new HttpError(409, "The two permitted accounts already exist");
        const id = existing ? (existing as { id: string }).id : crypto.randomUUID();
        const salt = randomBytes(16);
        const passwordHash = await hashPassword(input.password, salt);
        await env.DB.prepare(
          "INSERT INTO users (id, username, password_salt, password_hash, active) VALUES (?, ?, ?, ?, 1) ON CONFLICT(username) DO UPDATE SET password_salt=excluded.password_salt, password_hash=excluded.password_hash, active=1"
        ).bind(id, username, encodeSecret(salt), encodeSecret(passwordHash)).run();
        response = json({ created: true, username, provisionedAccounts: Math.min(2, (count?.count ?? 0) + (existing ? 0 : 1)) });
      } else if (url.pathname === "/v1/auth/login" && request.method === "POST") {
        const input = await body<{ username: string; password: string }>(request);
        if (!input.username || !input.password) throw new HttpError(400, "Username and password are required");
        const user = await env.DB.prepare(
          "SELECT id, username, password_salt, password_hash FROM users WHERE username = ? COLLATE NOCASE AND active = 1"
        ).bind(input.username.trim()).first<{ id: string; username: string; password_salt: string; password_hash: string }>();
        if (!user || !(await verifyPassword(input.password, user.password_salt, user.password_hash))) {
          throw new HttpError(401, "Invalid credentials");
        }
        const now = Math.floor(Date.now() / 1000);
        const ttl = Math.min(86400, Math.max(900, Number(env.TOKEN_TTL_SECONDS ?? 43200)));
        const token = await issueToken(env, { sub: user.id, username: user.username, iat: now, exp: now + ttl });
        response = json({ accessToken: token, expiresIn: ttl, user: { id: user.id, username: user.username } });
      } else if (url.pathname === "/v1/messages" && request.method === "GET") {
        const user = await authenticate(request, env);
        const otherId = url.searchParams.get("with");
        if (!otherId) throw new HttpError(400, "Conversation partner is required");
        const rows = await env.DB.prepare(
          "SELECT id, sender_id AS senderId, recipient_id AS recipientId, ciphertext, nonce, attachment_key AS attachmentKey, created_at AS createdAt, delivered_at AS deliveredAt, read_at AS readAt FROM messages WHERE (sender_id = ? AND recipient_id = ?) OR (sender_id = ? AND recipient_id = ?) ORDER BY created_at DESC LIMIT 100"
        ).bind(user.sub, otherId, otherId, user.sub).all();
        response = json({ messages: rows.results.reverse() });
      } else if (url.pathname === "/v1/messages" && request.method === "POST") {
        const user = await authenticate(request, env);
        const input = await body<{ recipientId: string; ciphertext: string; nonce: string; attachmentKey?: string }>(request);
        if (!input.recipientId || !input.ciphertext || !input.nonce) throw new HttpError(400, "Encrypted message fields are required");
        const recipient = await env.DB.prepare("SELECT id FROM users WHERE id = ? AND active = 1").bind(input.recipientId).first();
        if (!recipient || input.recipientId === user.sub) throw new HttpError(403, "Invalid conversation partner");
        if (input.ciphertext.length > 20000 || input.nonce.length > 256) throw new HttpError(413, "Message too large");
        const id = crypto.randomUUID();
        await env.DB.prepare(
          "INSERT INTO messages (id, sender_id, recipient_id, ciphertext, nonce, attachment_key) VALUES (?, ?, ?, ?, ?, ?)"
        ).bind(id, user.sub, input.recipientId, input.ciphertext, input.nonce, input.attachmentKey ?? null).run();
        const room = env.CHAT_ROOMS.get(env.CHAT_ROOMS.idFromName("two-person-chat"));
        await room.fetch(new Request("https://chat-room/broadcast", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ type: "message", id, senderId: user.sub, recipientId: input.recipientId })
        }));
        response = json({ id, accepted: true }, 201);
      } else if (url.pathname === "/v1/live" && request.method === "GET") {
        await authenticate(request, env);
        if (request.headers.get("Upgrade")?.toLowerCase() !== "websocket") throw new HttpError(426, "WebSocket upgrade required");
        const room = env.CHAT_ROOMS.get(env.CHAT_ROOMS.idFromName("two-person-chat"));
        response = await room.fetch(request);
      } else {
        throw new HttpError(404, "Not found");
      }
      return cors(request, response);
    } catch (error) {
      const status = error instanceof HttpError ? error.status : 500;
      const message = error instanceof Error ? error.message : "Internal server error";
      return cors(request, json({ error: status === 500 ? "Internal server error" : message }, status));
    }
  }
};
