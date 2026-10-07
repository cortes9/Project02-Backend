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

## Continuous integration

Pull requests targeting `main` run the backend test suite in GitHub Actions using the `Backend Tests` workflow. The tests use Testcontainers to start PostgreSQL with the GitHub-hosted runner's Docker daemon, so no database service, Supabase credentials, or repository secrets are required.
