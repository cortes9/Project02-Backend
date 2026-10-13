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

The API listens on port `8080`. Do not put Supabase credentials in the Dockerfile, image, build arguments, or documentation. The image does not copy `.env` files or local machine-specific configuration.

## Continuous integration

Pull requests targeting `main` run the backend test suite in GitHub Actions using the `Backend Tests` workflow. The tests use Testcontainers to start PostgreSQL with the GitHub-hosted runner's Docker daemon, so no database service, Supabase credentials, or repository secrets are required.
