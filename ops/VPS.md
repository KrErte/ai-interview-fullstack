# VPS 62.171.153.133: how Futureproof runs

A shared Ubuntu 24.04 server. Other apps also run on it: **lumen** (`/opt/lumen`) and **inventory-service** (`/root/inventory-service-node`, web on port 8090).

## Layout

| What | Where | Ports |
|---|---|---|
| Shared reverse proxy (Caddy) | `/opt/caddy` (`ops/caddy`) | 80, 443 (public) |
| Umami analytics | `/opt/umami` (`ops/umami`) | `127.0.0.1:3100` (HTTPS via Caddy) |
| Futureproof (compose project `futureproof`) | `/opt/futureproof` (this repo) | frontend `127.0.0.1:8100`; backend and Postgres internal only |
| lumen | `/opt/lumen` | `127.0.0.1:8081` (moved from `0.0.0.0:80`; backup of its compose file next to it) |
| inventory-service | `/root/inventory-service-node` | 8090 (public), API `127.0.0.1:8080` |

**Caddy routing:**
- `https://careerrisk.ee` → Futureproof. The Let's Encrypt certificate is renewed by Caddy, and http redirects to https.
- `www.careerrisk.ee` and the old `62-171-153-133.sslip.io` → 301 to `https://careerrisk.ee`, path kept.
- Every other host name, including the bare IP → lumen over http.

**Domain:** `careerrisk.ee`, DNS at Zone.ee. The A record of `careerrisk.ee` points to 62.171.153.133, and
`www` is a CNAME to it.

**Changing the domain:**
1. Point its DNS A record to 62.171.153.133.
2. Add the domain to `ops/caddy/Caddyfile`.
3. Copy the file to `/opt/caddy/` and run `docker exec shared-caddy caddy reload --config /etc/caddy/Caddyfile`.
4. Set `FRONTEND_BASE_URL` in `/opt/futureproof/.env`. It is also the only CORS origin in prod, so change it
   together with the domain, then run `./deploy-vps.sh`.
5. Update `data-domains` for Umami (see below).

## Analytics (Umami)

- **Where:** `/opt/umami` (`ops/umami`), with its own Postgres. Served at `https://umami.62-171-153-133.sslip.io` (Caddy → `127.0.0.1:3100`). It is cookieless, so no cookie banner is needed.
- **Login:** user `admin`; the password is in `/opt/umami/admin-password` (mode 600). The default password was changed at install.
- **Tracking:** the script tag is in `frontend-risk/src/index.html` and `backend/src/main/resources/templates/jobs/layout.html` with `data-domains="careerrisk.ee"`, so local development is not tracked. The website id is in `/opt/umami/website-id`.
- **Custom events:** `test-start` (profession), `test-complete` (profession, band), `seo-cta` (profession, from the job pages).
- **After a domain change:** update `data-domains` and the website's domain in Umami.

## Deploy

```bash
./deploy-vps.sh        # pulls main on the server and rebuilds the futureproof project
```

Secrets are only in `/opt/futureproof/.env` (mode 600): `DB_PASSWORD` and `JWT_SECRET` (generated on the server), plus `FRONTEND_PORT` and `FRONTEND_BASE_URL`. Optionally add `OPENAI_API_KEY`; the AI comment option then appears in the test.

## Security

- **SSH is key-only:** `/etc/ssh/sshd_config.d/00-futureproof-hardening.conf` sets `PasswordAuthentication no` and `PermitRootLogin prohibit-password`. Add more keys to `/root/.ssh/authorized_keys`. If no key works, use the provider's VNC console with the root password.
- **Firewall (ufw):** incoming is denied except 22, 80, 443 and 8090. Ports that Docker publishes bypass ufw, so bind internal services to `127.0.0.1`, as Futureproof and lumen do.

## Backups

- **Nightly:** `/usr/local/bin/futureproof-backup.sh` (`ops/backup`) runs at 03:15 from `/etc/cron.d/futureproof-backup`. It writes `pg_dump`s to `/var/backups/futureproof` and keeps 14 days. The log is `/var/log/futureproof-backup.log`.
- **Restore:** `gunzip -c FILE | docker exec -i futureproof-postgres-1 psql -U ai -d ai`. This was tested on 2026-10-13: all 11 tables restored into a throwaway database.
- **Off-site:** the dumps stay on this server. Copy them off it for real disaster recovery.
