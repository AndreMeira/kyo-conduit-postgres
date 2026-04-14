# Conduit

A [RealWorld](https://docs.realworld.show/introduction/) backend implementation built with **Scala 3**, **Kyo**, and **PostgreSQL**.

RealWorld is a standardised Medium-like blogging platform spec (sometimes called the "mother of all demo apps") that lets you compare backend implementations across languages and frameworks. This one uses the Kyo effect system end-to-end.

## Tech Stack

| Layer          | Technology                                                    |
|----------------|---------------------------------------------------------------|
| Language       | Scala 3.8.2                                                  |
| Effect system  | [Kyo](https://getkyo.io/) 1.0-RC1                            |
| HTTP           | [Tapir](https://tapir.softwaremill.com/) via kyo-tapir, Netty |
| Database       | PostgreSQL 17, [Magnum](https://github.com/AugustNagworthy/magnum) ORM |
| Migrations     | Flyway                                                        |
| JSON           | Circe                                                         |
| Auth           | JWT (jwt-scala), HMAC-SHA256                                  |
| Configuration  | PureConfig, HOCON                                             |
| Testing        | Munit, TestContainers                                         |

## API Endpoints

All 19 endpoints from the [Conduit spec](https://docs.realworld.show/specifications/backend/endpoints/):

| Method   | Path                                | Auth       |
|----------|-------------------------------------|------------|
| POST     | `/api/users`                        | -          |
| POST     | `/api/users/login`                  | -          |
| GET      | `/api/user`                         | Required   |
| PUT      | `/api/user`                         | Required   |
| GET      | `/api/profiles/:username`           | Optional   |
| POST     | `/api/profiles/:username/follow`    | Required   |
| DELETE   | `/api/profiles/:username/follow`    | Required   |
| GET      | `/api/articles`                     | Optional   |
| GET      | `/api/articles/feed`                | Required   |
| GET      | `/api/articles/:slug`               | Optional   |
| POST     | `/api/articles`                     | Required   |
| PUT      | `/api/articles/:slug`               | Required   |
| DELETE   | `/api/articles/:slug`               | Required   |
| POST     | `/api/articles/:slug/favorite`      | Required   |
| DELETE   | `/api/articles/:slug/favorite`      | Required   |
| GET      | `/api/articles/:slug/comments`      | Optional   |
| POST     | `/api/articles/:slug/comments`      | Required   |
| DELETE   | `/api/articles/:slug/comments/:id`  | Required   |
| GET      | `/api/tags`                         | -          |

## Architecture

```
application/
  http/          Tapir endpoint definitions, route handlers, Netty server
domain/
  model/         User, Article, Comment, Profile, Tag
  error/         Typed domain errors (NotFound, Unauthorised, Conflict, ...)
  request/       Use case input types
  response/      Use case output types
  service/
    usecase/     Business logic (one class per operation)
    persistence/ Repository interfaces + Database transaction abstraction
    authentication/  JWT token creation and verification
    validation/  State validation (uniqueness checks, etc.)
infrastructure/
  postgres/      PostgreSQL repositories, Flyway migrations, HikariCP
  inmemory/      In-memory repositories (for testing)
  configuration/ PureConfig loaders
  codecs/        JSON and HTTP codec mappings
```

Dependencies flow inward: `infrastructure` implements `domain` interfaces, `application` wires everything together via Kyo Layers.

## Prerequisites

- Java 21
- sbt
- Docker

## Getting Started

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Run the application:

```bash
sbt run
```

The server starts on `http://localhost:8080`.

Try it out:

```bash
# Register
curl -s -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"user":{"username":"jake","email":"jake@jake.jake","password":"jakejake"}}'

# Login
curl -s -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"user":{"email":"jake@jake.jake","password":"jakejake"}}'

# List tags
curl -s http://localhost:8080/api/tags
```


To reset the database (remove all data), stop Postgres and delete the volume:

```bash
docker compose down
docker volume rm kyo-conduit-postgres_conduit-data
```


## Testing

Tests use TestContainers to spin up a PostgreSQL instance automatically — no manual setup needed:

```bash
sbt test
```

## Docker

Build the Docker image and run the full stack:

```bash
./bin/build
docker compose up
```

## Configuration

Configuration files live in `src/main/resources/config/`:

- `database.conf` — JDBC URL, credentials, pool settings, Flyway migration config
- `authentication.conf` — password salt, JWT token salt and TTL

Environment variable overrides are supported for deployment:

| Variable      | Default                                        |
|---------------|------------------------------------------------|
| `DB_URL`      | `jdbc:postgresql://localhost:5432/conduit`     |
| `DB_USER`     | `conduit`                                      |
| `DB_PASSWORD` | `conduit`                                      |
