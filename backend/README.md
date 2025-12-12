Appointments Service (Spring Boot, Java 17)
===========================================

A backend microservice for managing doctor and patient appointments, focusing on RESTful API design, JPA data persistence, and concurrency control via optimistic locking.

Tech Stack
- Java 17
- Spring Boot 3.x
- Spring Data JPA (Hibernate)
- H2 (default), PostgreSQL/MySQL (via profiles)
- Flyway for schema migrations
- JUnit 5 & Mockito (unit tests), Testcontainers (integration tests)
- Maven

Run Locally (H2, default)
1. From the backend/ directory:
   - mvn spring-boot:run
2. Service will start at http://localhost:8081

Profiles: PostgreSQL and MySQL
- PostgreSQL:
  - Start DB via Docker Compose: docker compose up -d postgres
  - Run app: mvn spring-boot:run -Dspring-boot.run.profiles=postgres
- MySQL:
  - Start DB via Docker Compose: docker compose up -d mysql
  - Run app: mvn spring-boot:run -Dspring-boot.run.profiles=mysql

Database Schema Management (Flyway)
- Flyway is enabled in all profiles.
- Migrations:
  - H2: src/main/resources/db/migration/h2/V1__init.sql
  - PostgreSQL: src/main/resources/db/migration/postgres/V1__init.sql
  - MySQL: src/main/resources/db/migration/mysql/V1__init.sql
- JPA `ddl-auto` is set to `validate` to ensure the schema matches entities.

Key Entities
- Doctor: id, name, specialization, contact_email (unique), per_slot_duration (minutes, >=5)
- Appointment: id, doctor (FK), patient_name, start_time, end_time, status (CONFIRMED/CANCELLED), version (@Version, optimistic locking)
- Unique constraint on (doctor_id, start_time) prevents double booking at DB level.

Concurrency Control (Optimistic Locking)
- Appointment has @Version field; concurrent updates to the same row will throw ObjectOptimisticLockingFailureException and are translated to HTTP 409.
- Booking flow:
  - If overlapping CONFIRMED appointment exists, respond 409 (SlotAlreadyBookedException).
  - If a CANCELLED appointment exists for the requested slot, it is updated to CONFIRMED (version checked).
  - If no row exists, a new appointment is created.
  - Concurrent inserts for the same (doctor_id, start_time) are caught (DataIntegrityViolationException) and translated to HTTP 409.

API Endpoints
- POST /api/doctors
  - Create a new doctor.
  - Body: { "name": "...", "specialization": "...", "contactEmail": "...", "perSlotDurationMinutes": 30 }
- GET /api/doctors?specialization=Cardiology
  - List doctors, optional filter by specialization.
- GET /api/doctors/{id}/availability?date=YYYY-MM-DD
  - Return available slots for a doctor on a given date (respecting configured working hours).
- POST /api/appointments
  - Book appointment.
  - Body: { "doctorId": 1, "patientName": "John Doe", "startTime": "2025-12-31T10:00:00" }
- PUT /api/appointments/{id}
  - Cancel appointment (status -> CANCELLED).

Testing
- Unit tests: mvn test
- Integration tests (Testcontainers):
  - Concurrency scenario: backend/src/test/java/com/example/appointments/integration/ConcurrencyIntegrationTest.java
  - Runs a PostgreSQL container and attempts two concurrent bookings for the same slot; asserts 1 success and 1 conflict.

Postman Collection
- Import backend/postman/appointments.postman_collection.json into Postman.
- Set {{baseUrl}} to http://localhost:8081 (default).

CI (GitHub Actions)
- Workflow: .github/workflows/ci.yml
- Runs Maven verify (unit + integration tests) on push/PR.

One-click Launch on Render (Blueprint)
- We added a repo-root render.yaml. Render can provision both the PostgreSQL database and the web service automatically.
- Steps:
  1. Push this repo to GitHub.
  2. In Render, click "New +" → "Blueprint" and select your GitHub repo.
  3. Render will read render.yaml at the root and:
     - Create a free PostgreSQL database (appointments-db).
     - Create a free Docker Web Service (appointments-service) using backend/Dockerfile.
     - Wire environment variables:
       - SPRING_PROFILES_ACTIVE=postgres
       - DATABASE_URL, DB_USER, DB_PASSWORD from the database (automatically injected).
       - SERVER_PORT=10000
  4. Deploy. Health check path /actuator/health should return UP when ready.
  5. Your live URL will be provided by Render (e.g., https://appointments-service.onrender.com).

Notes:
- The application auto-detects DATABASE_URL and converts it to a JDBC URL. You do not need to manually set SPRING_DATASOURCE_URL.
- Flyway migrations run on startup to create/update the schema automatically.
- If you prefer manual service creation, you can still use the Docker Web Service and set env vars as described earlier.

Docker (local)
- Build: `docker build -t appointments-service:latest -f backend/Dockerfile backend`
- Run with H2: `docker run -p 8081:8081 appointments-service:latest`
- Run with Postgres:
  ```
  docker run -p 8081:8081 \
    -e SPRING_PROFILES_ACTIVE=postgres \
    -e SPRING_DATASOURCE_URL="jdbc:postgresql://host.docker.internal:5432/appointmentsdb" \
    -e SPRING_DATASOURCE_USERNAME=postgres \
    -e SPRING_DATASOURCE_PASSWORD=postgres \
    appointments-service:latest
  ```

AWS (optional outline)
- Build a Docker image and push to ECR.
- Deploy to ECS/Fargate or Elastic Beanstalk using the Docker image.
- Provide env vars as above and configure a health check path `/actuator/health`.

Health Check
- With actuator enabled, the health endpoint is available at `/actuator/health`.
- For local runs: `curl http://localhost:8081/actuator/health` should return `"status":"UP"`.

Concurrency Demo Guide (Manual)
- Create a doctor:
  - POST /api/doctors with perSlotDurationMinutes = 30
- Attempt concurrent bookings for the same slot via bash:
