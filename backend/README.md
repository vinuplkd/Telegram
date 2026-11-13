# CBSE Edu Desk — private messaging backend foundation

This folder is the initial self-hosted backend scaffold. It uses Matrix Synapse with PostgreSQL. It is not deployed yet, and it is not connected to an Android client yet.

## Intended account policy

- Exactly two user accounts, created manually by the owner.
- Public registration disabled.
- No phone-number login.
- Use separate strong server credentials/device sessions; the four-digit PIN is only a local app-unlock secret.
- Enable end-to-end encryption in the client. Synapse does not make an ordinary unencrypted room end-to-end encrypted automatically.

## Important deployment prerequisite

Synapse requires a stable server name (normally a domain you control) and HTTPS in production. Do not expose port 8008 directly to the public internet. Put a TLS reverse proxy in front of it and expose the Matrix client API through HTTPS.

## 1. Generate Synapse configuration

Install Docker Engine / Docker Compose on the server that will host this backend. These commands are for the server, not the Windows computer used to build the APK.

Choose the final server name first, for example chat.example.com. It must be a DNS name you control; do not use that example literally.

From this backend directory:

    mkdir -p synapse
    docker run --rm -it \
      -v "$PWD/synapse:/data" \
      -e SYNAPSE_SERVER_NAME=YOUR_REAL_SERVER_NAME \
      -e SYNAPSE_REPORT_STATS=no \
      ghcr.io/element-hq/synapse:latest generate

The command generates synapse/homeserver.yaml and signing keys. Keep the generated signing key private and back it up securely.

## 2. Configure PostgreSQL

Edit synapse/homeserver.yaml and set its database section to:

    database:
      name: psycopg2
      args:
        user: synapse
        password: "THE_SAME_LONG_PASSWORD_FROM_DOT_ENV"
        database: synapse
        host: postgres
        port: 5432
        cp_min: 5
        cp_max: 10

Set these security/account settings in the generated file:

    enable_registration: false
    enable_registration_without_verification: false
    allow_guest_access: false

Do not enable public registration. Keep the generated server name and signing-key path intact.

## 3. Start services

Copy .env.example to .env, replace the placeholder with a unique random password, and ensure the same password is in the Synapse database configuration.

    docker compose up -d
    docker compose logs -f synapse

## 4. Create exactly two accounts

After Synapse is running, create accounts interactively (the command prompts for passwords; do not put passwords in shell history):

    docker compose exec synapse register_new_matrix_user \
      -c /data/homeserver.yaml http://localhost:8008

Run it twice, once for each approved person. Do not create additional accounts. Keep registration disabled. Store the two credentials securely and do not commit them to this repository.

## 5. Production hardening before real use

- Configure a TLS reverse proxy and a DNS name you control.
- Restrict inbound network access; expose only HTTPS.
- Set up encrypted off-site backups of PostgreSQL and Synapse signing/media data.
- Establish a recovery process for lost devices and account credentials.
- Configure rate limits, monitoring, updates and firewall rules.
- Review federation policy. If the service is intended to communicate only between the two local accounts, disable federation in Synapse and verify that the chosen client still works as expected.
- Add TURN infrastructure later for reliable voice/video calls.
- Do not rely on a four-digit PIN as the server password. A 4-digit PIN has only 10,000 possibilities and must only unlock locally protected key material with retry throttling and Android Keystore support.

## Current status

This is a deployment scaffold only. The server name, TLS, generated Synapse configuration, two account credentials, backups and Android client integration still need to be completed. No user credentials or production secrets are included here.
