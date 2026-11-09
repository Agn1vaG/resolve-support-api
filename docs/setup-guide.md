# Local Setup Guide

## Prerequisites

- Java Development Kit (JDK) 21.
- PostgreSQL 15 or newer, running locally. PostgreSQL is not bundled with this application.
- Docker Engine or Docker Desktop for the full PostgreSQL Testcontainers integration suite.

Confirm Java and PostgreSQL tools with `java -version` and `psql --version`. The Maven Wrapper downloads the configured Maven distribution on first use, so a globally installed Maven is not required.

## Install Java

Install a JDK 21 distribution using the package manager or installer for your operating system. Set `JAVA_HOME` to the JDK installation and add its `bin` directory to `PATH`. Open a new terminal and verify the version. No global Maven installation is needed; `mvnw` and `mvnw.cmd` use Maven 3.9.9 configured in `.mvn/wrapper/maven-wrapper.properties`.

## Install and start PostgreSQL

Install PostgreSQL from the official installer/package for your operating system, or use a managed local PostgreSQL service. Start the PostgreSQL server/service and ensure it listens on `localhost:5432` (or update `DB_URL`).

Create a database and a dedicated login role. From a PostgreSQL administrative session, run (replace the example password with a local secret):

```sql
CREATE ROLE resolve_app LOGIN PASSWORD 'replace-with-a-local-secret';
CREATE DATABASE resolve_db OWNER resolve_app;
```

Do not reuse this example password outside a local development environment. PostgreSQL creates the database; Spring Boot and Flyway connect to it and create Flyway's migration history table on the first successful startup.

## Environment variables

Set these variables in the shell that will run the Maven Wrapper:

| Variable | Required | Example |
|---|---|---|
| `DB_URL` | No | `jdbc:postgresql://localhost:5432/resolve_db` (default) |
| `DB_USERNAME` | Yes | `resolve_app` |
| `DB_PASSWORD` | Yes | Your local PostgreSQL role password |
| `SERVER_PORT` | No | `8080` (default) |
| `JWT_SECRET_BASE64` | Yes | Random Base64-encoded value from at least 32 cryptographic random bytes |
| `JWT_EXPIRATION_MS` | No | `900000` (15 minutes; minimum 60000) |

`.env.example` documents the variable names. Spring Boot does not automatically load `.env`, so export/set values in your shell or configure them in your IDE's run configuration. Keep real credentials in an ignored `.env` file only if your chosen shell tooling explicitly loads it; never commit it.

PowerShell example for the current session:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/resolve_db"
$env:DB_USERNAME = "resolve_app"
$env:DB_PASSWORD = "your-local-password"
$jwtBytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($jwtBytes)
$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String($jwtBytes)
$env:JWT_EXPIRATION_MS = "900000"
```

`JWT_SECRET_BASE64` must decode to at least 32 random bytes for HS256 signing. Generate a new secret per environment and keep it outside source control. `.env.example` contains a placeholder only and is not a usable signing key. Startup fails if the secret is absent or too short.

## Run the application

From the project root, run the wrapper script for your shell. The first run downloads Maven 3.9.9 from Maven Central.

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

```shell
./mvnw clean test
./mvnw spring-boot:run
```

To package and run a jar instead:

```shell
./mvnw clean package
java -jar target/resolve-support-api-0.0.1-SNAPSHOT.jar
```

On Windows, use `.\mvnw.cmd clean package` for the packaging command.

The application listens on port 8080 by default. Check the public health endpoint at `http://localhost:8080/actuator/health`; it should return JSON with status `UP`. With the default health aggregation, database connectivity is also checked and a database failure makes the overall health status `DOWN`.

## Register and authenticate

Register a regular user. Public registration always assigns `USER`, regardless of any extra role field in the request:

```shell
curl -i -X POST http://localhost:8080/api/v1/auth/register -H "Content-Type: application/json" -d "{\"name\":\"Ari Example\",\"email\":\"ari@example.com\",\"password\":\"SecurePass123\"}"
```

Login to receive a signed access token:

```shell
curl -i -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d "{\"email\":\"ari@example.com\",\"password\":\"SecurePass123\"}"
```

Send the returned token on protected endpoints using `Authorization: Bearer <accessToken>`. The default lifetime is 900 seconds. Do not log or commit the token. `POST /api/v1/users`, `GET /api/v1/users`, and role management are admin-only; bootstrap an initial admin directly in a trusted local database with a BCrypt hash, or add a dedicated provisioning process before deployment. Never expose a public admin registration path.

## Existing Phase 4 account data

Phase 4 temporarily stored submitted passwords without hashing. Phase 5 login uses BCrypt and will not authenticate those old plaintext values. Delete those development-only accounts and register them again, or provide a controlled password reset before using the data. No automatic plaintext-to-hash migration is performed because the original passwords should not be retained as credentials.

## Security tests

The PostgreSQL integration tests use Testcontainers and are skipped when Docker is unavailable. Run `./mvnw clean test` (or `.\mvnw.cmd clean test` in PowerShell) with Docker running for the full integration suite; without Docker, the JUnit/Mockito unit tests still run. The security tests cover registration, login, JWT validation, and role/ownership checks.

## Continuous integration

GitHub Actions runs `.github/workflows/ci.yml` on pushes to `main` and pull requests targeting `main`. The job installs Temurin Java 21, caches Maven dependencies, runs `./mvnw --batch-mode --no-transfer-progress clean verify` on Ubuntu, and uploads Surefire XML reports even when the build fails. GitHub-hosted Ubuntu runners provide Docker for Testcontainers; CI does not depend on local PostgreSQL credentials because integration tests start their own PostgreSQL container.

## What happens on startup

Spring Boot creates a PostgreSQL datasource from `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Flyway scans `classpath:db/migration`, runs versioned migrations that have not yet been applied, and records them in `flyway_schema_history`. V1 establishes migration history, V2 creates core tables, and V3 extends ticket history for assignment and creation events. Hibernate uses `ddl-auto: validate`, so it checks entity mappings against the schema rather than creating or changing tables.

Paged endpoints default to 20 records and cap requests at 100 records per page.

## Troubleshooting

- **`java` not recognized:** install JDK 21, set `JAVA_HOME`, update `PATH`, then open a fresh terminal. The wrapper does not remove the Java requirement.
- **Maven distribution download fails:** check network access to Maven Central or configure an approved Maven mirror in the wrapper properties.
- **Connection refused:** confirm PostgreSQL is installed, its service is running, and host/port in `DB_URL` are correct.
- **Password authentication failed:** verify the role/password and that `DB_USERNAME`/`DB_PASSWORD` are set in the same shell running Maven.
- **Database does not exist:** create `resolve_db` and ensure the login role owns it or has connection privileges.
- **Port 8080 is in use:** set `SERVER_PORT` to an unused port and call the matching health URL.
- **Flyway migration error:** inspect the first startup exception and `flyway_schema_history`; do not edit a migration already applied to a shared database. Add a new versioned migration instead.
- **Health reports `DOWN`:** inspect application logs and verify PostgreSQL connection settings and availability. The health endpoint intentionally does not expose component details.
- **JWT signing configuration fails:** generate a new random Base64 secret with at least 32 decoded bytes and set `JWT_SECRET_BASE64`; do not use the placeholder from `.env.example`.
- **Login fails for a Phase 4 account:** recreate the development account with `/api/v1/auth/register`; the old stored plaintext is deliberately not accepted as a BCrypt password hash.
- **Maven cannot download dependencies:** confirm network access to Maven Central or configure your organization's Maven mirror.
