# Deploying footpedia

Backend on Railway, frontend on Vercel. Both repos are already pushed:
- Backend: https://github.com/SABHYA18/footpedia
- Frontend: https://github.com/SABHYA18/footpedia-fe

## 0. Rotate secrets first

The Supabase DB password, football-data.org key, Gmail app password, and
API-Football key were all pasted in plaintext during development chat
sessions. Rotate at least the DB password before using it here:
Supabase dashboard -> Project Settings -> Database -> Reset password.

## 1. Backend on Railway

1. **New Project -> Deploy from GitHub repo** -> select `SABHYA18/footpedia`.
   Railway auto-detects the `Dockerfile` at the repo root and builds from it.
2. **Add Redis**: in the same Railway project, "+ New" -> Database -> Redis.
   This creates a second service you'll reference from the backend service's
   variables (step 3).
3. On the **footpedia service -> Variables**, add:
   ```
   DB_HOST=db.<your-project-ref>.supabase.co
   DB_PORT=5432
   DB_NAME=postgres
   DB_USER=postgres
   DB_PASSWORD=<the rotated password>
   DB_SSLMODE=require

   REDIS_HOST=${{Redis.REDISHOST}}
   REDIS_PORT=${{Redis.REDISPORT}}
   REDIS_PASSWORD=${{Redis.REDISPASSWORD}}

   JWT_SECRET=<generate with: openssl rand -base64 48>
   JWT_EXPIRATION_MINUTES=1440

   FOOTBALL_DATA_API_KEY=<your key>
   API_FOOTBALL_API_KEY=<your key>

   MAIL_USERNAME=<gmail address>
   MAIL_APP_PASSWORD=<gmail app password>
   MAIL_FROM=<gmail address>

   # Fill in after step 2 (frontend) is deployed and you have its real URL:
   FRONTEND_URL=https://<your-app>.vercel.app
   FRONTEND_ORIGIN=https://<your-app>.vercel.app
   ```
   The `${{Redis.VARNAME}}` syntax references the Redis service's own
   variables directly -- Railway resolves them at deploy time, no copy-pasting.
4. **Settings -> Healthcheck Path**: set to `/actuator/health`.
5. Deploy. Once it's up, copy the public URL Railway gives the service
   (Settings -> Networking -> Generate Domain if one isn't assigned yet) --
   you'll need it for the frontend's `VITE_API_BASE_URL`.

## 2. Frontend on Vercel

1. **Add New Project** -> import `SABHYA18/footpedia-fe`. Vercel auto-detects
   the Vite preset -- no build config needed.
2. **Environment Variables**: add
   ```
   VITE_API_BASE_URL=https://<your-railway-app>.up.railway.app
   ```
3. Deploy. Copy the resulting `*.vercel.app` URL.

## 3. Close the loop

Go back to the Railway backend service and set `FRONTEND_URL` and
`FRONTEND_ORIGIN` (step 1.3) to the real Vercel URL from step 2, then redeploy
the backend so CORS and password-reset links point at the right place.

## 4. Smoke test

- Visit the Vercel URL, sign up, confirm you land on `/app`.
- Set a reminder on a fixture and confirm the email arrives (uses the same
  Gmail SMTP already wired up).
- Check Railway logs for the historical-ingestion scheduler ticking cleanly
  (`Scheduled historical backfill: season ... matchday ... -> ...` every 6h).
