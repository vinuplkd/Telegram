export interface Env {
  DB: D1Database;
  MEDIA: R2Bucket;
  CHAT_ROOMS: DurableObjectNamespace;
  TOKEN_SECRET: string;
  BOOTSTRAP_SECRET: string;
  APP_NAME: string;
  TOKEN_TTL_SECONDS?: string;
}

export interface SessionClaims {
  sub: string;
  username: string;
  exp: number;
  iat: number;
}

export class HttpError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}
