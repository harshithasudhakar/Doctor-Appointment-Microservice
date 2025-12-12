How to Make It Live (No Local Run Needed)

Simplest: Render Blueprint (one click)
1) Push this repository to GitHub.
2) In Render, choose "New" → "Blueprint", pick your repo.
3) Render reads `render.yaml` at the repo root and provisions:
   - A free PostgreSQL database (appointments-db).
   - A free Web Service (appointments-service) using `backend/Dockerfile`.
   - Environment variables are wired automatically:
     - SPRING_PROFILES_ACTIVE=postgres
     - DATABASE_URL (postgres connection string)
     - DB_USER, DB_PASSWORD (database credentials)
     - SERVER_PORT=10000
4) Click "Deploy". Once `/actuator/health` shows UP, your API is live at the assigned URL.

Manual alternative (if you prefer):
- Create the DB and Web Service manually and set env vars:
  - SPRING_PROFILES_ACTIVE=postgres
  - DATABASE_URL (postgres connection string, Render injects it for DBs)
  - DB_USER, DB_PASSWORD
  - SERVER_PORT=10000

Verify
- Health check: `https://<your-render-url>/actuator/health` → status UP
- Example requests:
  - POST `https://<your-render-url>/api/doctors`
  - GET `https://<your-render-url>/api/doctors`
  - GET `https://<your-render-url>/api/doctors/{id}/availability?date=YYYY-MM-DD`
  - POST `https://<your-render-url>/api/appointments`
  - PUT `https://<your-render-url>/api/appointments/{id}`

Troubleshooting
- If the service fails to start:
  - Confirm the Blueprint created both services and env vars are present.
  - Check service logs in Render: Flyway or JPA validation errors typically indicate a missing table; migrations should create them automatically.
- If health check fails:
  - Ensure the DB is reachable; Render manages networking between the web service and the database automatically in Blueprints.
