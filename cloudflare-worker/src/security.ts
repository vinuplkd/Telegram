import type { Env, SessionClaims } from "./types";
import { HttpError } from "./types";

const encoder = new TextEncoder();
const decoder = new TextDecoder();

function toBase64Url(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
}

function fromBase64Url(value: string): Uint8Array {
  const padded = value.replace(/-/g, "+").replace(/_/g, "/") + "===".slice((value.length + 3) % 4);
  const binary = atob(padded);
  return Uint8Array.from(binary, c => c.charCodeAt(0));
}

export async function hashPassword(password: string, salt: Uint8Array): Promise<Uint8Array> {
  const material = await crypto.subtle.importKey("raw", encoder.encode(password), "PBKDF2", false, ["deriveBits"]);
  const bits = await crypto.subtle.deriveBits(
    { name: "PBKDF2", hash: "SHA-256", salt, iterations: 100_000 },
    material,
    256
  );
  return new Uint8Array(bits);
}

export function randomBytes(length: number): Uint8Array {
  return crypto.getRandomValues(new Uint8Array(length));
}

async function hmac(secret: string, data: string): Promise<Uint8Array> {
  const key = await crypto.subtle.importKey(
    "raw", encoder.encode(secret), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]
  );
  return new Uint8Array(await crypto.subtle.sign("HMAC", key, encoder.encode(data)));
}

export async function issueToken(env: Env, claims: SessionClaims): Promise<string> {
  const header = toBase64Url(encoder.encode(JSON.stringify({ alg: "HS256", typ: "JWT" })));
  const payload = toBase64Url(encoder.encode(JSON.stringify(claims)));
  const body = header + "." + payload;
  return body + "." + toBase64Url(await hmac(env.TOKEN_SECRET, body));
}

export async function verifyToken(env: Env, token: string): Promise<SessionClaims> {
  const parts = token.split(".");
  if (parts.length !== 3) throw new HttpError(401, "Invalid session");
  const body = parts[0] + "." + parts[1];
  const expected = await hmac(env.TOKEN_SECRET, body);
  const supplied = fromBase64Url(parts[2]);
  let diff = expected.length ^ supplied.length;
  for (let i = 0; i < Math.min(expected.length, supplied.length); i++) diff |= expected[i] ^ supplied[i];
  if (diff !== 0) throw new HttpError(401, "Invalid session");
  const claims = JSON.parse(decoder.decode(fromBase64Url(parts[1]))) as SessionClaims;
  if (!claims.sub || claims.exp <= Math.floor(Date.now() / 1000)) throw new HttpError(401, "Session expired");
  return claims;
}

export async function authenticate(request: Request, env: Env): Promise<SessionClaims> {
  const header = request.headers.get("Authorization") ?? "";
  if (!header.startsWith("Bearer ")) throw new HttpError(401, "Sign in required");
  return verifyToken(env, header.slice(7));
}

export async function verifyPassword(password: string, saltB64: string, expectedB64: string): Promise<boolean> {
  const salt = fromBase64Url(saltB64);
  const expected = fromBase64Url(expectedB64);
  const actual = await hashPassword(password, salt);
  let diff = actual.length ^ expected.length;
  for (let i = 0; i < Math.min(actual.length, expected.length); i++) diff |= actual[i] ^ expected[i];
  return diff === 0;
}

export function encodeSecret(bytes: Uint8Array): string {
  return toBase64Url(bytes);
}
