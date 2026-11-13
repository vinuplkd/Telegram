# Backend status — Cloudflare selected

The earlier Matrix/Synapse scaffold is superseded and must not be deployed for CBSE Edu Desk. Firebase and Supabase were considered but are not selected. The chosen backend is Cloudflare Workers + D1 + SQLite-backed Durable Objects + R2. A custom domain is not required; the Worker uses a workers.dev endpoint.

See `cloudflare-worker/README.md` for deployment instructions and `NATIVE_ANDROID_PLAN.md` for the current architecture and security milestones.
