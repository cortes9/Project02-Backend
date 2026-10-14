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
  study-group-finder-api
```

The API listens on port `8080` locally by default. When `PORT` is set, the application listens on that port instead. Do not put Supabase credentials in the Dockerfile, image, build arguments, or documentation. The image does not copy `.env` files or local machine-specific configuration.

## Deploy to Render

The root-level `render.yaml` defines a Docker web service deployed from `main`. To create the service in Render:

1. Authorize Render to access the GitHub repository `cortes9/Project02-Backend`.
2. Create a new Blueprint from the repository. Render reads `render.yaml`, builds the root `Dockerfile`, and deploys the `main` branch.
3. When prompted, enter private values for `SUPABASE_DB_URL`, `SUPABASE_DB_USERNAME`, and `SUPABASE_DB_PASSWORD`.
4. Confirm that the health-check path is `/actuator/health`, then trigger the initial deployment.
5. Open the generated HTTPS URL and verify that `/actuator/health` returns an `UP` response.

To create the service manually instead of through the Blueprint:

1. Choose **New > Web Service** in the Render dashboard and connect `cortes9/Project02-Backend`.
2. Select `main` as the branch and Docker as the runtime. Use the repository's root `Dockerfile`.
3. Add private values for `SUPABASE_DB_URL`, `SUPABASE_DB_USERNAME`, and `SUPABASE_DB_PASSWORD`.
4. Set `/actuator/health` as the health-check path and deploy the service.
5. Verify the generated HTTPS URL by opening its `/actuator/health` endpoint.

Render supplies `PORT` at runtime; the application uses `8080` only when `PORT` is absent.

After deployment, record the real Render HTTPS URL in the team's deployment documentation. Do not add a placeholder URL. The Android application should use that HTTPS URL as its API base URL.

## Continuous integration

Pull requests targeting `main` run the backend test suite in GitHub Actions using the `Backend Tests` workflow. The tests use Testcontainers to start PostgreSQL with the GitHub-hosted runner's Docker daemon, so no database service, Supabase credentials, or repository secrets are required.
