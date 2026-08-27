# Roadmap

What is missing, roughly in the order it should be built, and what is deliberately out
of scope.

Every item here is referenced from somewhere else in the documentation. Nothing on this
list is a surprise to the code — the point of writing it down is that a limitation
nobody has named is a limitation nobody fixes.

- [Correctness and safety first](#correctness-and-safety-first)
- [Then the foundations](#then-the-foundations)
- [Then the product](#then-the-product)
- [Smaller things worth fixing](#smaller-things-worth-fixing)
- [Deliberately out of scope](#deliberately-out-of-scope)

---

## Correctness and safety first

### 1 · Enforce roles on the server

`ROLE_ADMIN` and `ROLE_PESCADOR` are seeded, assigned, loaded into the security context
and returned on login — and never checked. Every write endpoint accepts any verified
account, so the admin distinction lives only in what the frontend chooses to render.

The fix is `@EnableMethodSecurity` plus `@PreAuthorize("hasRole('ADMIN')")` on
catalogue writes — fish, baits, equipment, rivers, river species and regulations —
leaving catch records writable by any fisher. It is small, and it is first because
everything else on this list is a smaller problem.

### 2 · Ownership on catch-record deletion

`POST /api/catch-records` takes the owner from the token and ignores the body, which is
right. `DELETE /api/catch-records/{id}` checks only that the caller is authenticated.
Load the record, compare the owner against the authenticated principal, and let
`ROLE_ADMIN` through as the exception rather than the rule.

While there: **not-yours and not-found should answer the same.** A distinct `403`
confirms the record exists to whoever is probing ids.

### 3 · Remove the `jwt.secret` fallback

```properties
jwt.secret=${JWT_SECRET:ChaveTemporaria...}
```

The default exists so a fresh clone boots. The consequence is that an environment
missing `JWT_SECRET` also boots — and signs tokens with a value published in this
repository, so anyone could mint a valid token for any e-mail address.

A secret should have no default, for the same reason `DB_URL` has none: a missing
variable must break the boot loudly. Combine with a startup check that refuses to
start if the key is shorter than 32 characters, so the failure is a clear message
rather than a `WeakKeyException` at the first login.

### 4 · Close the image endpoint

`POST /api/images/upload` is `permitAll`, with no size limit and no content-type
allowlist, writing into the project's Cloudinary account. Require authentication, cap
the multipart size in configuration, and accept only image content types.

### 5 · Rate limiting

Nothing is limited. Login, registration and password reset can each be automated
against as fast as the network allows, and the free tiers behind them (SMTP quota,
Cloudinary storage, Supabase connections) are the things that actually break first.

The right layer is the edge rather than a counter inside a service — Render's platform
controls, or a gateway in front. Bucket4j on the auth endpoints is the in-process
fallback if the edge is not available.

### 6 · Harden the password-reset flow

Three separate problems on one endpoint:

- **No captcha**, unlike register and login.
- **It confirms whether an account exists** — *"Não encontramos uma conta com este
  e-mail"* — which is e-mail enumeration, and free. Always answer *"if an account
  exists, a code has been sent"*.
- **The OTP comes from `java.util.Random`**, a predictable PRNG. Use `SecureRandom`.
  While there, `nextInt(999999)` never produces `999999` — the correct range is
  `nextInt(1_000_000)`.

Add a per-account attempt counter, so a six-digit code cannot be brute-forced inside
its fifteen-minute window.

### 7 · A password policy

Nothing checks length or composition today, so a one-character password is accepted.
A minimum length is the whole of the useful part; composition rules mostly produce
worse passwords.

---

## Then the foundations

### 8 · Flyway, and `ddl-auto=validate`

`ddl-auto=update` never drops, never alters, leaves no record of what ran, and gives
the application permission to reshape production's schema at boot.

Baseline the current schema as `V1__baseline.sql`, own every change as a numbered
script, and switch to `validate` so a mismatch fails the boot instead of silently
migrating. This unblocks two things that are otherwise impossible: renaming
`catch_record` to `tb_catch_record`, and moving `catch_date` from `LocalDateTime` to
`OffsetDateTime`.

### 9 · A real test suite

One test exists — `contextLoads` — and it needs a live database. The shape it should
grow into is set out in [Running and deploying](05-deployment.md#testing). The single
most valuable first test is the JWT contract, because that is the only place where a
silent regression is an authentication bypass rather than a broken screen.

### 10 · CI

There is no pipeline in this repository. A GitHub Actions workflow running
`./mvnw verify` on JDK 21 for every push and pull request, plus a credential-format
scan over the history, is a morning's work and stops the next regression from reaching
Render.

### 11 · A local database, and a `docker-compose.yml`

`application-local.properties` currently points at the same Supabase instance the
deployed API uses, so local development writes to production data. A compose file with
PostgreSQL, and a local profile pointing at it, removes an entire category of accident.

### 12 · Backend error tracking

Sentry is in the frontend. A server-side exception appears in Render's log stream and
nowhere else. Add `sentry-spring-boot-starter`, and Spring Boot Actuator with a health
endpoint — which Render can use as a health check, and which is the prerequisite for
any monitoring at all.

While there: turn off `spring.jpa.show-sql` outside local, and replace
`e.printStackTrace()` in `JwtAuthenticationFilter` with a logger.

### 13 · An exception hierarchy

`GlobalExceptionHandler` maps every `RuntimeException` to `404`, so a genuine server
fault is reported as "not found". Introduce `NotFoundException`,
`BusinessException` and `ConflictException`, map each to its own status, and let
anything unrecognised be a `500` — which is what it is.

Bean-validation failures should return a field-keyed map rather than a
comma-joined string, so the frontend can put each message next to its input.

---

## Then the product

### 14 · Expose Swagger

`springdoc-openapi` is on the classpath and both `/v3/api-docs` and
`/swagger-ui/**` fall through to `anyRequest().authenticated()`, so the explorer cannot
load its own document. Permitting them is one line — the actual decision is whether the
deployed environment should publish its full route list, or whether the explorer should
be local-only.

Then annotate: `@Operation`, `@ApiResponse`, and a `SecurityScheme` for the bearer
token so the *Authorize* button works.

### 15 · Return what the API already stores

Two relationships are writable and not readable:

- `FishResponseDTO` carries no baits or equipment, so the recommendations written
  through `POST /api/fishes` are invisible through the API.
- `CatchRecordResponseDTO` carries `equipmentId` but no equipment name, while bait,
  fish and spot all carry both.

Neither needs a schema change.

### 16 · Deduplicate fishing spots

Every catch record posted with coordinates creates a new `tb_fishing_spot` row. Two
records from the same rock produce two spots, and the table grows once per catch rather
than once per place.

Match against existing spots within a small radius before inserting — a few dozen
metres is the right order — and reuse the match. This is the direct cost of the
project's central decision, and the decision is still right; the deduplication is the
part that was deferred.

### 17 · Enforce the closed seasons

`tb_fishing_regulation` stores *piracema* periods and nothing consults them. Posting a
catch record dated inside a closed season is accepted silently.

The product question comes before the code: refuse the record, or accept it with a
warning flag? Refusing punishes honest reporting and will produce false dates. The
better answer is almost certainly to accept, mark it, and show the fisher what the rule
was.

This also needs the basin promoted from free text to its own table, because matching
`tb_river.hydrographic_basin` against
`tb_fishing_regulation.hydrographic_basin` by string is what makes the check
unreliable.

### 18 · Fix `diasNaAgua`

The fisher ranking counts `COUNT(DISTINCT c.catchDate)` — distinct *timestamps*. Two
catches an hour apart on one trip count as two days. `COUNT(DISTINCT CAST(c.catchDate
AS date))` is the fix.

### 19 · Private logbooks

Every catch record is public, which is the product's premise and is right for the
leaderboards. A per-record visibility flag would let a fisher keep a spot to themselves
without leaving the app — the single most requested thing in any fishing community,
because a good spot stops being good once it is on a map.

### 20 · Sorting, properly

`sortBy` is passed straight to `Sort.by(...)`, so an unknown property becomes a runtime
failure rather than a `400`, and the direction is fixed per endpoint rather than chosen
by the caller. Validate the property against an allowlist per resource and accept a
`direction` parameter.

---

## Smaller things worth fixing

| Item | Where |
|---|---|
| `@EnableAsync` is missing, so `EmailService`'s `@Async` methods run on the request thread | `BrazilApplication` |
| `CatchRecord` has no `@Table`, so it is `catch_record` while every sibling is `tb_*` | needs item 8 first |
| `catch_date` is `LocalDateTime` — no time zone | needs item 8 first |
| `CatchRecord` declares no `equals`/`hashCode`, unlike all twelve other entities | `entity/CatchRecord.java` |
| `RiverSpeciesRepository` has no uniqueness constraint on `(river_id, fish_id)` | duplicate pairings are accepted |
| `FishService.save` drops unknown bait and equipment ids silently — `findAllById` returns what it finds | `service/FishService.java` |
| `CatchRecordRequestDTO` requires `riverId`, `latitude` and `longitude` even when `fishingSpotId` is sent and they are ignored | needs cross-field validation |
| `BaitService.findById` and `EquipmentService.findById` exist and are not routed | add `GET /{id}`, or delete them |
| `buscarComFiltro` on `CatchRecordRepository` is written and never called | wire it to a `?search=` parameter, or delete it |
| No `.dockerignore`, so `COPY . .` ships `target/` into the build stage | root |
| The `pom.xml` has empty `<name>`, `<description>`, `<licenses>` and `<scm>` blocks | root |
| `HELP.md` is the untouched Spring Initializr file | root |
| CORS allows `pescabrasil.vercel.app`; the deployed frontend is `pesca-brasil-ui.vercel.app` | `SecurityConfig` |

---

## Deliberately out of scope

Not oversights. Decisions.

**Microservices.** One aggregate is written often and the rest is a catalogue that
changes rarely. Splitting it would multiply the operational cost without removing a
coupling — a catch record needs the fish, the river and the spot to be consistent at
the moment it is written, which is exactly what a single transaction gives for free.
The sibling project [TicketFlow](https://github.com/patrickpriebe/ticketflow) is where
that architecture is explored, because that domain genuinely needs it.

**Federated login.** An e-mail round trip proves the one thing this product needs from
identity. Adding Google would add consent screens, refresh flows and a second identity
model to reconcile, in exchange for saving one screen.

**Real-time anything.** Nothing here changes while somebody is watching. A fishing log
is written after the trip.

**Native mobile.** The Angular frontend is responsive and the API is the same either
way.

**A weather API on the server.** `weatherCondition` and `moonPhase` are recorded as
the fisher observed them, not as a service reported them — which is the more useful
fact, since the fisher was there. The frontend shows a live forecast for planning; that
is a different question and belongs where it is.
