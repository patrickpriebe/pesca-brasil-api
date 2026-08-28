# Running and deploying

- [Running locally](#running-locally)
- [Configuration](#configuration)
- [Docker](#docker)
- [The deployed environment](#the-deployed-environment)
- [Constraints that shaped the setup](#constraints-that-shaped-the-setup)
- [Testing](#testing)

---

## Running locally

**Requirements:** JDK 21 and a PostgreSQL database. There is no `docker-compose.yml` —
the local setup points at the same managed Postgres as the deployed environment, which
is convenient and is a limitation, discussed below.

```bash
git clone https://github.com/patrickpriebe/pesca-brasil-api.git
cd pesca-brasil-api
```

Create `src/main/resources/application-local.properties`. The file is git-ignored, so it
will not be committed:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/pescabrasil
spring.datasource.username=postgres
spring.datasource.password=postgres

cloudinary.cloud-name=your-cloud
cloudinary.api-key=your-key
cloudinary.api-secret=your-secret

spring.mail.username=you@gmail.com
spring.mail.password=your-app-password

google.recaptcha.secret=your-recaptcha-secret
jwt.secret=a-secret-of-at-least-32-characters-for-HS256
```

```bash
./mvnw spring-boot:run
```

The application listens on **8080**. `spring.profiles.active=local` is set in
`application.properties`, so the local file is picked up without a flag.

Hibernate creates the schema on first boot — see
[Data model](02-data-model.md#schema-ownership) for what `ddl-auto=update` does and
does not do. `RoleDataLoader` seeds `ROLE_ADMIN` and `ROLE_PESCADOR` on every boot,
idempotently.

A first request that needs no data:

```bash
curl "http://localhost:8080/api/fishes?size=5"
```

### What you need for each flow

`jwt.secret` is required to boot at all. The rest may be unset, and the flows that
need them fail at call time rather than at startup:

| To exercise | You need |
|---|---|
| Booting | `jwt.secret`, at least 32 characters |
| Any read | PostgreSQL |
| Registration, login and password reset | reCAPTCHA secret **and** SMTP credentials |
| Photo upload | Cloudinary credentials, and a token |
| Catalogue writes | An account holding `ROLE_ADMIN` — see [Granting `ROLE_ADMIN`](#granting-role_admin) |

Registration is the awkward one locally: without a working reCAPTCHA secret the
endpoint refuses every request, because the service
[fails closed](04-security.md#recaptcha). Google's documented test keys are the usual
way through this, and inserting a pre-verified user directly into `tb_user` with a
BCrypt hash is the other.

`spring.jpa.show-sql=true` and `format_sql=true` are on, so every statement Hibernate
issues is printed. Useful locally, noisy in production — see the roadmap.

---

## Configuration

Every value the application reads, and where it comes from.

| Property | Environment variable | Default | Required |
|---|---|---|---|
| `spring.datasource.url` | `DB_URL` | *(empty)* | yes |
| `spring.datasource.username` | `DB_USER` | *(empty)* | yes |
| `spring.datasource.password` | `DB_PASSWORD` | *(empty)* | yes |
| `cloudinary.cloud-name` | `CLOUDINARY_CLOUD_NAME` | *(empty)* | for uploads |
| `cloudinary.api-key` | `CLOUDINARY_API_KEY` | *(empty)* | for uploads |
| `cloudinary.api-secret` | `CLOUDINARY_API_SECRET` | *(empty)* | for uploads |
| `spring.mail.username` | `MAIL_USER` | *(empty)* | for registration |
| `spring.mail.password` | `MAIL_PASSWORD` | *(empty)* | for registration |
| `google.recaptcha.secret` | `RECAPTCHA_SECRET` | *(empty)* | for register/login/reset |
| `jwt.secret` | `JWT_SECRET` | **none** | yes — the boot fails without it |

Fixed in `application.properties` and not overridable per environment: the Postgres
dialect, `ddl-auto=update`, SQL logging, Gmail's SMTP host, port and 5-second timeouts,
and the 5 MB multipart cap (`spring.servlet.multipart.max-file-size` and
`max-request-size`).

> **`JWT_SECRET` has no default.** `${JWT_SECRET}` with no `:` means Spring cannot
> resolve the placeholder when the variable is absent, and the context fails to start —
> deliberately, so a forgotten variable is a failed deploy rather than tokens signed
> with a string anybody can read in this repository. `JwtUtil` additionally refuses a
> secret shorter than 32 characters. See [Security](04-security.md#secrets).

### Granting `ROLE_ADMIN`

Catalogue writes require `ROLE_ADMIN`, and no endpoint hands it out — registration
always assigns `ROLE_PESCADOR`. Promoting an account is a database operation:

```sql
INSERT INTO tb_user_roles (user_id, role_id)
SELECT u.id, r.id FROM tb_user u, tb_role r
WHERE u.email = 'you@example.com' AND r.name = 'ROLE_ADMIN';
```

`RoleDataLoader` guarantees both rows exist in `tb_role` after any boot, so this can be
run against a freshly created database without seeding anything first.

---

## Docker

A two-stage build. Maven compiles inside the image, and only the JAR crosses into the
runtime layer:

```dockerfile
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
docker build -t pesca-brasil-api .
docker run -p 8080:8080 --env-file .env pesca-brasil-api
```

Two things about this file are worth knowing rather than assuming:

- **`-DskipTests`.** The image build never runs the suite. Nothing else runs it either
  — there is no CI pipeline in this repository — so today the tests are run by hand or
  not at all.
- **`COPY . .` copies the whole working directory** into the build stage, `target/`
  included, and there is no `.dockerignore`. It works, and it makes the build slower
  and the layer cache less useful than it should be.

Both are on the [roadmap](06-roadmap.md).

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
panel.

> **Sentry runs in the frontend, not here.** `@sentry/angular` is a dependency of the
> UI project and reports browser-side errors. This API has no Sentry SDK, no Actuator
> and no metrics endpoint — a server-side exception appears in Render's log stream and
> nowhere else. Adding backend error tracking is on the roadmap; it is called out
> because it is easy to read the frontend's monitoring as the whole system's.

---

## Constraints that shaped the setup

Three things about running this for free are worth stating, because they only show up
after deploying.

**Free Render instances sleep.** After a period without traffic the container is spun
down, and the next request pays a cold start — the JVM boots, Hibernate reconciles the
schema, the connection pool opens. First-request latency of a minute or more is normal,
and it is why the deployed API can appear to be down when it is merely asleep. The
usual fix is a scheduled ping; there is none in this repository today.

**The database is shared between local and deployed.** `application-local.properties`
points at the same Supabase instance the deployed API uses, so development happens
against production data. It is convenient and it means a local experiment can write
rows real users see. A local Postgres in Docker — and a `docker-compose.yml` to bring
it up — is the fix, and it is on the roadmap.

**The connection goes through Supabase's pooler on port 6543**, with
`prepareThreshold=0` in the JDBC URL. That parameter disables the PostgreSQL JDBC
driver's server-side prepared statements, which the transaction pooler cannot support —
a prepared statement lives in a session, and the pooler hands out a different session
per transaction. Without it, queries fail intermittently with *"prepared statement
already exists"* under any real concurrency. It is one query parameter and it is not
optional.

---

## Testing

**One test.** `BrazilApplicationTests.contextLoads` starts the Spring context and
asserts that it starts.

That is worth being exact about rather than dressing up. What the test does catch is a
real class of failure — a missing bean, a circular dependency, a `@Value` with no
property behind it, an entity that Hibernate cannot map — and those are precisely the
errors that only appear at boot. What it does not catch is any behaviour: not a
validation rule, not the ownership logic on catch records, not the token filter, not
the spot-creation branch.

It also needs a reachable database and the configured properties, because a
`@SpringBootTest` with no slicing starts everything. So the one test that exists cannot
run in an environment that has not been configured — which is why the Docker build
skips it.

The intended shape, in order:

1. `@WebMvcTest` per controller with the service mocked — status codes, validation
   messages, and that `POST /api/catch-records` refuses an anonymous caller.
2. Plain JUnit over the services with mocked repositories — the two branches of
   `CatchRecordService.save`, and every `orElseThrow`.
3. `@DataJpaTest` against Testcontainers for the derived queries and the ranking
   projection, which are the parts a mock cannot verify.
4. A test that pins the JWT contract: a token signed with a different key is rejected,
   an expired token answers `401` with the expired-token message specifically.

Item 4 is the one worth writing first. It is the only place where a silent regression
would be an authentication bypass rather than a broken screen.
