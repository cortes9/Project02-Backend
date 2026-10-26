# Study Group Finder API

Spring Boot backend for Study Group Finder.

## Prerequisites

- Java 21
- Docker installed and running

Backend tests start an isolated PostgreSQL container. They do not use Supabase credentials or any shared database. The first test run may download the PostgreSQL container image, so it can take longer than later runs.

## Run tests

```bash
bash ./gradlew test
```

## Run with Docker

Docker must be installed and running. Build the API image from the repository root:

```bash
docker build -t study-group-finder-api .
```

Run the container with the Supabase database configuration supplied at runtime:

```bash
docker run --rm -p 8080:8080 \
  -e SUPABASE_DB_URL='<your-supabase-database-url>' \
  -e SUPABASE_DB_USERNAME='<your-supabase-database-username>' \
  -e SUPABASE_DB_PASSWORD='<your-supabase-database-password>' \
  -e GOOGLE_CLIENT_ID='<your-google-client-id>' \
  study-group-finder-api
```

The API listens on port `8080` locally by default. When `PORT` is set, the application listens on that port instead. Do not put Supabase credentials in the Dockerfile, image, build arguments, or documentation. The image does not copy `.env` files or local machine-specific configuration.

## Deploy to Render

The root-level `render.yaml` defines a Docker web service deployed from `main`. To create the service in Render:

1. Authorize Render to access the GitHub repository `cortes9/Project02-Backend`.
2. Create a new Blueprint from the repository. Render reads `render.yaml`, builds the root `Dockerfile`, and deploys the `main` branch.
3. When prompted, enter private values for `SUPABASE_DB_URL`, `SUPABASE_DB_USERNAME`, `SUPABASE_DB_PASSWORD`, and `GOOGLE_CLIENT_ID`.
4. Confirm that the health-check path is `/actuator/health`, then trigger the initial deployment.
5. Open the generated HTTPS URL and verify that `/actuator/health` returns an `UP` response.

To create the service manually instead of through the Blueprint:

1. Choose **New > Web Service** in the Render dashboard and connect `cortes9/Project02-Backend`.
2. Select `main` as the branch and Docker as the runtime. Use the repository's root `Dockerfile`.
3. Add private values for `SUPABASE_DB_URL`, `SUPABASE_DB_USERNAME`, `SUPABASE_DB_PASSWORD`, and `GOOGLE_CLIENT_ID`.
4. Set `/actuator/health` as the health-check path and deploy the service.
5. Verify the generated HTTPS URL by opening its `/actuator/health` endpoint.

Render supplies `PORT` at runtime; the application uses `8080` only when `PORT` is absent.

After deployment, record the real Render HTTPS URL in the team's deployment documentation. Do not add a placeholder URL. The Android application should use that HTTPS URL as its API base URL.

### Deployed API

The backend is deployed at:

`https://study-group-finder-api-bm09.onrender.com`

Health check:

`https://study-group-finder-api-bm09.onrender.com/actuator/health`

The Android application should use `https://study-group-finder-api-bm09.onrender.com/api/v1/` as its API base URL.

## Continuous integration

Pull requests targeting `main` run the backend test suite in GitHub Actions using the `Backend Tests` workflow. The tests use Testcontainers to start PostgreSQL with the GitHub-hosted runner's Docker daemon, so no database service, Supabase credentials, or repository secrets are required.

## Swagger and OpenAPI

Interactive Swagger UI is public at:

- Local: `http://localhost:8080/swagger-ui.html`
- Deployed: `https://study-group-finder-api-bm09.onrender.com/swagger-ui.html`

The generated OpenAPI JSON for the deployed implementation is available at:

`https://study-group-finder-api-bm09.onrender.com/v3/api-docs`

[`docs/openapi.yaml`](docs/openapi.yaml) remains the team's handwritten API contract. The `/v3/api-docs` document is generated at runtime from the implemented Spring controllers, DTOs, validation rules, and OpenAPI annotations.

## Seed demo data

The manual PostgreSQL seed script is located at [`scripts/seed-demo-data.sql`](scripts/seed-demo-data.sql). All records are fictional and use reserved negative identifiers and recognizable `demo` values. The script is never run automatically by Spring Boot. It is transactional and idempotent, so running it more than once updates the same demo records without creating duplicates.

Review the target database before executing the script. To use the Supabase SQL Editor:

1. Open the intended Supabase project and select **SQL Editor**.
2. Copy the complete contents of `scripts/seed-demo-data.sql` into a new query.
3. Confirm the selected project is the intended demo target, then run the query.

For a PostgreSQL database accessible through `psql`, run this generic command from the repository root and supply the connection information securely when prompted or through your normal local configuration:

```bash
psql '<database-connection-string>' -f scripts/seed-demo-data.sql
```

After seeding the deployed database, verify the public list endpoint:

```text
https://study-group-finder-api-bm09.onrender.com/api/v1/study-sessions?limit=20&offset=0
```
