# Deploying to Render (Free Tier)

This project includes a `Dockerfile` and `render.yaml` so you can deploy on Render's free tier.

## 1) Fork or push the repo to GitHub
Ensure your code is on GitHub and accessible to Render.

## 2) Create a new Web Service
- Go to Render → New → Web Service
- Connect your GitHub repo
- It will detect `render.yaml` and propose a service named `aibot`
- Choose the Free plan

## 3) Configure environment
- The Docker image uses `scripts/render-start.sh` to adapt `DATABASE_URL` to Spring Boot env vars
- Options:
  1) Without a database (demo only):
     - The app will try to use PostgreSQL; to allow demo mode, you can switch to the `dev` profile by setting `SPRING_PROFILES_ACTIVE=dev` in the service's Environment
     - In this mode, H2 is used (in-memory) and data resets on each restart
  2) With Render PostgreSQL (recommended):
     - Add a Render PostgreSQL instance (there is a limited free plan)
     - Render will set `DATABASE_URL` automatically for the web service
     - No extra env vars required; the entrypoint parses `DATABASE_URL` to set Spring datasource vars

- Add other env vars as needed:
  - `JWT_SECRET`: click Add Secret → Generate value
  - `JWT_EXPIRATION`: 3600000
  - `OPENAI_API_KEY`: optional
  - `JAVA_OPTS`: "-Xms256m -Xmx512m"

## 4) Deploy
- Click Create Web Service → Render builds and deploys using Dockerfile
- Health check path: `/`

## 5) File uploads
- Free tier has no persistent disk by default; uploaded files won't survive a restart
- For persistence, add a Render Disk (may require a paid plan) and set `FILE_UPLOAD_DIR` accordingly
- Alternatively, change the app to store files on S3 (future enhancement)

## 6) Verify
- Visit the service URL
- Login/register, upload a small file, ask a question (if using DB)

## 7) Troubleshooting
- 502 or timeouts: check Logs → deploy/build errors
- DB connection: ensure you have either `SPRING_PROFILES_ACTIVE=dev` or a valid `DATABASE_URL`
- Memory: reduce `JAVA_OPTS` heap if OOM-killed
- Cold starts: free tier sleeps; first request may be slow
