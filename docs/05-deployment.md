# Running and deploying

- [Running locally](#running-locally)
- [Configuration](#configuration)
- [Granting ROLE_ADMIN](#granting-role_admin)
- [Docker](#docker)
- [Continuous integration](#continuous-integration)
- [The deployed environment](#the-deployed-environment)
- [Constraints that shaped the setup](#constraints-that-shaped-the-setup)
- [Testing](#testing)

---

## Running locally

**Requirements:** JDK 21, and Docker for the database.

```bash
git clone https://github.com/patrickpriebe/pesca-brasil-api.git
cd pesca-brasil-api
docker compose up -d
```

That brings up PostgreSQL on **5433** — 5432 is usually taken by a native install; inside
the compose network it is still `postgres:5432`.

Then copy the example configuration and fill in what you need:

```bash
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties
```

The real file is git-ignored; the example is not, so the shape of the configuration is
in the repository and the values never are.

```bash
./mvnw spring-boot:run
```

The application listens on **8080**. `spring.profiles.active=local` is set in
`application.properties`, so the local file is picked up without a flag.

Hibernate creates the schema on first boot — see
[Data model](02-data-model.md#schema-ownership) for what `ddl-auto=update` does and does
not do. `RoleDataLoader` seeds `ROLE_ADMIN` and `ROLE_PESCADOR` on every boot,
idempotently.

A first request that needs no data:

```bash
curl "http://localhost:8080/api/fishes?size=5"
```

With the example configuration, Swagger is open at
**http://localhost:8080/swagger-ui/index.html** — `pescabrasil.openapi.public=true` is
set there and defaults to `false` everywhere else.

### What you need for each flow

`jwt.secret` is required to boot at all. The rest may be unset, and the flows that need
them fail at call time rather than at startup:

| To exercise | You need |
|---|---|
| Booting | `jwt.secret`, at least 32 characters — there is no default |
| Any read | PostgreSQL |
| Registration, login and password reset | reCAPTCHA secret **and** SMTP credentials |
| Photo upload | Cloudinary credentials, and a token |
| Catalogue writes | An account holding `ROLE_ADMIN` — see below |

Registration is the awkward one locally: without a working reCAPTCHA secret the endpoint
refuses every request, because the service
[fails closed](04-security.md#recaptcha). Google's documented test keys are the usual way
through this, and inserting a pre-verified user directly into `tb_user` with a BCrypt
hash is the other.

---

## Configuration

Every value the application reads, and where it comes from.

| Property | Environment variable | Default | Required |
|---|---|---|---|
| `spring.datasource.url` | `DB_URL` | *(empty)* | yes |
| `spring.datasource.username` | `DB_USER` | *(empty)* | yes |
| `spring.datasource.password` | `DB_PASSWORD` | *(empty)* | yes |
| `jwt.secret` | `JWT_SECRET` | **none** | yes — the boot fails without it |
| `cloudinary.cloud-name` | `CLOUDINARY_CLOUD_NAME` | *(empty)* | for uploads |
| `cloudinary.api-key` | `CLOUDINARY_API_KEY` | *(empty)* | for uploads |
| `cloudinary.api-secret` | `CLOUDINARY_API_SECRET` | *(empty)* | for uploads |
| `spring.mail.username` | `MAIL_USER` | *(empty)* | for the account flows |
| `spring.mail.password` | `MAIL_PASSWORD` | *(empty)* | for the account flows |
| `google.recaptcha.secret` | `RECAPTCHA_SECRET` | *(empty)* | for register/login/reset |
| `spring.datasource.hikari.maximum-pool-size` | `DB_POOL_SIZE` | `5` | no |
| `pescabrasil.cors.allowed-origins` | `CORS_ALLOWED_ORIGINS` | the two real origins | no |
| `pescabrasil.openapi.public` | `OPENAPI_PUBLIC` | `false` | no |
| `pescabrasil.rate-limit.enabled` | `RATE_LIMIT_ENABLED` | `true` | no |
| `pescabrasil.rate-limit.requests-per-window` | `RATE_LIMIT_REQUESTS` | `20` | no |
| `pescabrasil.rate-limit.window-seconds` | `RATE_LIMIT_WINDOW_SECONDS` | `60` | no |

Fixed in `application.properties` and not overridable per environment: the Postgres
dialect, `ddl-auto=update`, `open-in-view=false`, the 5 MB multipart cap,
`server.forward-headers-strategy=framework`, the Actuator exposure, and Gmail's SMTP
host, port and 5-second timeouts.

> **`JWT_SECRET` has no default.** `${JWT_SECRET}` with no `:` means Spring cannot
> resolve the placeholder when the variable is absent, and the context fails to start —
> deliberately, so a forgotten variable is a failed deploy rather than tokens signed with
> a string anybody can read in this repository. `JwtUtil` additionally refuses a secret
> shorter than 32 characters, with a message naming the property. See
> [Security](04-security.md#secrets).

---

## Granting `ROLE_ADMIN`

Catalogue writes require `ROLE_ADMIN`, and **no endpoint hands it out** — registration
always assigns `ROLE_PESCADOR`. That is deliberate: a registration endpoint able to grant
the role that guards the catalogue would not be guarding it.

Promoting an account is a database operation:

```sql
INSERT INTO tb_user_roles (user_id, role_id)
SELECT u.id, r.id FROM tb_user u, tb_role r
WHERE u.email = 'you@example.com' AND r.name = 'ROLE_ADMIN';
```

`RoleDataLoader` guarantees both rows exist in `tb_role` after any boot, so this runs
against a freshly created database without seeding anything first.

> Until an account is promoted, the frontend's management screens answer `403` on their
> first write — and the Angular interceptor signs the user out on `403`, so it looks like
> a session problem rather than a permission one. That is [roadmap item
> 9](06-roadmap.md#9--the-frontends-role-guard), and it lives in the UI repository.

---

## Docker

A two-stage build.

```dockerfile
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN useradd --system --uid 10001 --create-home appuser
USER appuser
COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
docker build -t pesca-brasil-api .
docker run -p 8080:8080 --env-file .env pesca-brasil-api
```

Four things about this file are decisions rather than boilerplate:

- **`pom.xml` is copied before `src`.** Dependencies change far less often than code, so
  resolving them in their own layer means an ordinary commit reuses the cache instead of
  re-downloading the world.
- **`-DskipTests`.** The suite runs in CI, with reports. Repeating it here would only
  lengthen every image build; before there was a CI pipeline, this flag meant the tests
  never ran at all.
- **A JRE base, not a JDK.** The runtime does not compile anything.
- **A non-root user.** A process that only reads its own jar has no reason to be root
  inside the container.

`.dockerignore` keeps `target/`, `.git/`, the docs and — most importantly —
`application-local.properties` out of the build context.

---

## Continuous integration

[`.github/workflows/ci.yml`](../.github/workflows/ci.yml) runs on every push to `main`
and every pull request, cancelling in-progress runs when a new commit arrives.

| Job | What it does |
|---|---|
| **build** | `./mvnw verify` on JDK 21 with Maven caching, uploading the surefire reports as an artifact |
| **secrets** | Scans the **entire history** (`fetch-depth: 0`) for credential *formats* — `AKIA…`, private-key headers, `sk_live_…`, `ghp_…`, connection strings with a password — and separately asserts that `application-local.properties` never appears in the history |
| **image** | Builds the Docker image with Buildx and a layer cache. No push: the point is knowing the Dockerfile still works |

The secret scan prints the commit and the file, never the matching line — the run log is
public.

---

## The deployed environment

| Piece | Provider | Notes |
|---|---|---|
| API | **Render** | Docker runtime, free instance |
| Database | **Supabase** (PostgreSQL) | Reached through the session pooler |
| Images | **Cloudinary** | Free tier |
| E-mail | **Gmail SMTP** | App password, not the account password |
| Bot defence | **Google reCAPTCHA** | |
| Frontend | **Vercel** | Angular 22 — [pesca-brasil-ui](https://github.com/patrickpriebe/pesca-brasil-ui) |
| Frontend error tracking | **Sentry** | `@sentry/angular`, in the UI repository |

```
Browser ──► Vercel (Angular) ──► Render (Spring Boot) ──► Supabase (PostgreSQL)
                  │                      │
                  │                      ├──► Cloudinary
                  │                      ├──► Gmail SMTP
                  └──► reCAPTCHA ◄───────┘
```

No secret is in this repository; everything sensitive is set in Render's environment
panel. `/actuator/health` is public and is what a platform health check should point at.

> **Sentry runs in the frontend, not here.** `@sentry/angular` is a dependency of the UI
> project and reports browser-side errors. This API logs server-side exceptions through
> SLF4J, which on Render means the log stream and nowhere else. Aggregating them is
> [roadmap item 4](06-roadmap.md#4--backend-error-tracking); it is called out because it
> is easy to read the frontend's monitoring as the whole system's.

---

## Constraints that shaped the setup

Three things about running this for free only show up after deploying.

**Free Render instances sleep.** After a period without traffic the container is spun
down, and the next request pays a cold start — the JVM boots, Hibernate reconciles the
schema, the connection pool opens. First-request latency of a minute or more is normal,
and it is why the deployed API can appear to be down when it is merely asleep. There is
no scheduled ping in this repository.

**The connection goes through Supabase's pooler on port 6543**, with `prepareThreshold=0`
in the JDBC URL. That parameter disables the PostgreSQL JDBC driver's server-side
prepared statements, which the transaction pooler cannot support — a prepared statement
lives in a session, and the pooler hands out a different session per transaction. Without
it, queries fail intermittently with *"prepared statement already exists"* under any real
concurrency. It is one query parameter and it is not optional.

**The connection ceiling is low.** The free pooler accepts few connections, so
`DB_POOL_SIZE` defaults to 5; a larger pool means the second instance to start cannot
open its own and dies during schema reconciliation.

---

## Testing

**43 tests**, and none of them needs a database, a network or a Docker socket.

That last part is the design constraint rather than a coincidence.
`src/test/resources/application.properties` sits on the test classpath and replaces the
main one, so the suite does not activate the `local` profile, does not look for real
credentials, and points at in-memory H2. Before that file existed, the single test in the
project could only run on the machine that had the deployed database's credentials —
which is why the Docker build passed `-DskipTests` and why CI was impossible.

| Suite | Covers |
|---|---|
| `JwtUtilTest` | The token contract: a token signed with another key is refused, a tampered token is refused, a token does not work for another user, and a short secret refuses the boot |
| `UserServiceTest` | Registration, verification and reset: the password policy, the duplicate e-mail conflict, the account born disabled, expiry checked before correctness, the code destroyed on use, the five-attempt ceiling, and that unknown e-mail, wrong code and expired code are indistinguishable |
| `CatchRecordServiceTest` | Both branches of the spot resolution, the deduplication, the owner taken from the token, and that another person's record answers exactly like a missing one |
| `FishControllerSecurityTest` | The real filter chain: public reads, `401` anonymous, `403` for a fisher, `201` for an admin, the field-keyed validation body, and `400` for an unknown sort property |
| `PageableFactoryTest` | The sort allowlist, the direction parameter, and the ceilings |
| `BrazilApplicationTests` | The context still starts — a missing bean, a circular dependency, a `@Value` with no property behind it, an entity Hibernate cannot map |

Principles the suite holds to:

- **Every test that exists prevents a specific regression**, and most of them prevent one
  that was real. The five-attempt ceiling, the indistinguishable reset failures and the
  not-yours-is-not-found assertion are each a written-down version of a gap this project
  had.
- **The security tests exercise the real filter chain**, not a mock of it. An early
  version of `FishControllerSecurityTest` mocked `JwtAuthenticationFilter` itself; a
  Mockito mock of a `Filter` never calls `doFilter`, so every request died mid-chain and
  returned `200` with an empty body — and every assertion about authorization passed for
  the wrong reason.
- **No test needs the network.** What is still missing is the other side of that: the
  Cloudinary and reCAPTCHA integrations have no stubbed-HTTP coverage at all. That is
  [roadmap item 6](06-roadmap.md#6--widen-the-test-suite).
