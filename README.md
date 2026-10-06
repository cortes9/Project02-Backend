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
