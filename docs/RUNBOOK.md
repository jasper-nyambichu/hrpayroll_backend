# Runbook: operating Dhwarsh Employee Management

Commands run on the VM as the deploy user unless stated. Each organisation lives in its own folder:
`~/kisiimatt` (port 8081, `https://employeemanagent.taild2f884.ts.net`) and `~/shahceramics`
(port 8082, `...ts.net:8443`). `<org>` below means one of those names.

## 1. Deploying

A push to `main` runs the tests, builds one image and deploys it to each organisation in turn. Each deploy
waits until the backend is **healthy** (answers HTTP) and smoke-tests it; if one organisation fails, the
pipeline stops and the next one is not touched. The compose file is copied to the server with every release
(the previous one is kept as `docker-compose.prod.yml.previous`).

* **Keep an organisation on its current version:** `echo "reason" > ~/<org>/HOLD`. Remove the file to resume.
* **Deploy by hand / roll back:** (images from the last 7 days are kept)
  ```sh
  cd ~/<org> && IMAGE_TAG=<commit sha> docker compose -p <org> -f docker-compose.prod.yml up -d backend
  docker inspect -f '{{.State.Health.Status}}' <org>-backend        # wait for: healthy
  ```
* Deploy the backend before a web change that needs it; web projects redeploy themselves on push.

## 2. Health checks

```sh
docker ps --format '{{.Names}}  {{.Status}}'                          # every container "healthy"
curl -s http://127.0.0.1:8081/public/organisation; echo               # Kisiimatt
curl -s http://127.0.0.1:8082/public/organisation; echo               # Shah Ceramics
docker logs --tail 100 <org>-backend
```
A user's error message ending in "(Reference 1a2b3c...)" is the request ID: `docker logs <org>-backend | grep 1a2b3c`.

## 3. Backups (set up once)

```sh
mkdir -p ~/backups
crontab -e
# add:
30 2 * * * $HOME/dhwarsh-deploy/deploy/backup.sh >> $HOME/backups/backup.log 2>&1
0 4 * * 0  $HOME/dhwarsh-deploy/deploy/restore-check.sh >> $HOME/backups/restore-check.log 2>&1
```
* Nightly: each organisation's database (`~/backups/<org>/<org>-<date>.dump`) and uploads, kept 14 days.
* Weekly drill: the newest backup is restored into a throwaway database and checked (PASS/FAIL in the log).
* **Off the server:** a disk failure would take the backups with it. At least weekly, copy `~/backups` elsewhere,
  e.g. from a PC on the tailnet: `scp -r kisiimatt@employeemanagent:backups D:\dhwarsh-backups\`
  (or set `OFFSITE_DIR` to a mounted drive in the cron line).
* Targets agreed for now: lose at most one day of data (RPO 24 h); back in service within half a day (RTO 4 h).

## 4. Restoring a real backup (data loss or corruption)

1. `echo "restoring" > ~/<org>/HOLD` so no deploy interferes; tell users the system is briefly unavailable.
2. Stop only that organisation's backend: `cd ~/<org> && docker compose -p <org> -f docker-compose.prod.yml stop backend`
3. Keep a copy of the current state first: run `~/dhwarsh-deploy/deploy/backup.sh`.
4. Restore (replaces the database contents):
   ```sh
   docker exec -i <org>-postgres sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner' < ~/backups/<org>/<file>.dump
   ```
5. Start it: `docker compose -p <org> -f docker-compose.prod.yml up -d backend`, check §2, remove `HOLD`.

## 5. New organisation

1. `mkdir ~/<org>`, copy `docker-compose.prod.yml` and `new-organisation.sql` from `~/dhwarsh-deploy`.
2. Write `.env` with new values for **every** secret: `STACK`, `BACKEND_PORT` (next free, e.g. 8083),
   `PG_VOLUME=<org>_pgdata`, `POSTGRES_DB/USER/PASSWORD`, `JWT_SECRET`, `BIOMETRIC_TEMPLATE_KEY`,
   `CREDENTIAL_KEY` (`openssl rand -base64 32`), `FRONTEND_URL`, `MAIL_*`. `chmod 600 .env`; keep a private copy.
3. `IMAGE_TAG=<sha> docker compose -p <org> -f docker-compose.prod.yml up -d`, wait for healthy.
4. Run `new-organisation.sql` (creates the company and the only login, SUPER-ADMIN; refuses any database with data).
5. `sudo tailscale funnel --bg --https=<10000> http://127.0.0.1:<port>`.
6. New Vercel project from the same repo: `API_BASE_URL`, new `SESSION_SECRET`, domain.
7. Add `<org>` to the `for stack in ...` line in `.github/workflows/deploy.yml` and to `$organisations` in the
   desktop `build-installer.ps1`.

## 6. Incident checklist

1. What is affected: one organisation or both? Web, API or branch PCs? (§2)
2. Recent change? Roll back that organisation (§1) before investigating further.
3. Find the request ID / time in the logs. Never paste passwords, tokens or `.env` contents into chats or tickets.
4. Data damaged? Restore (§4). Secrets exposed? Replace them in `.env` and restart (a new `JWT_SECRET` signs
   everyone out; a new `BIOMETRIC_TEMPLATE_KEY` means re-enrolling fingerprints, so change it only if leaked).
5. Afterwards: write down cause, fix and a test that would have caught it.

## 7. Secrets per organisation (in `~/<org>/.env`, never in Git)

`POSTGRES_PASSWORD`, `JWT_SECRET`, `CREDENTIAL_KEY`, `BIOMETRIC_TEMPLATE_KEY`, `MAIL_PASSWORD`. Vercel holds
`SESSION_SECRET` per project. The desktop release key lives only in `%USERPROFILE%\.dhwarsh` on the build PC.
