# Roadmap

What is missing, roughly in the order it should be built, and what is deliberately out
of scope.

Every item here is referenced from somewhere else in the documentation. Nothing on this
list is a surprise to the code — the point of writing it down is that a limitation
nobody has named is a limitation nobody fixes.

- [Done](#done)
- [Correctness and safety first](#correctness-and-safety-first)
- [Then the foundations](#then-the-foundations)
- [Then the product](#then-the-product)
- [Smaller things worth fixing](#smaller-things-worth-fixing)
- [Deliberately out of scope](#deliberately-out-of-scope)

---

## Done

Kept on the list rather than deleted, so the reasoning survives the fix.

| Was | Now |
|---|---|
| **Roles never enforced.** `ROLE_ADMIN` was seeded, assigned and returned on login, and no endpoint checked it | `@EnableMethodSecurity` on `SecurityConfig`, `@PreAuthorize("hasRole('ADMIN')")` on all fourteen catalogue write handlers. Catch records stay writable by any verified fisher |
| **Catch-record deletion authenticated but not owned** | The service loads the record, compares its owner against the principal, and excepts `ROLE_ADMIN`. Not-yours answers as not-found |
| **`jwt.secret` had a committed fallback** | `${JWT_SECRET}` with no default, so a missing variable fails the boot. `JwtUtil` refuses a secret shorter than 32 characters at construction |
| **Image upload open and unbounded** | Token required, 5 MB cap in configuration, `image/*` only, empty file refused |
| **`forgot-password` had no captcha and confirmed account existence** | Captcha added; identical response whether or not the account exists; `reset-password` collapsed to one failure message |
| **OTP from `java.util.Random`, range excluded `999999`** | `SecureRandom` and `nextInt(1_000_000)` |

> **`ROLE_ADMIN` is not granted by any endpoint**, and the frontend's management screens
> are guarded by `authGuard` alone — so a `ROLE_PESCADOR` account now gets `403` from
> those screens' writes. Promote the accounts that need it:
>
> ```sql
> INSERT INTO tb_user_roles (user_id, role_id)
> SELECT u.id, r.id FROM tb_user u, tb_role r
> WHERE u.email = 'you@example.com' AND r.name = 'ROLE_ADMIN';
> ```
>
> The frontend's HTTP interceptor logs the user out on `403` as well as `401`, so a
> non-admin hitting a management screen is signed out rather than shown a message.
> Adding a role check to the Angular guard is the matching frontend change.

---

## Correctness and safety first

### 1 · Rate limiting

Nothing is limited. Login, registration and password reset can each be automated
against as fast as the network allows, and the free tiers behind them (SMTP quota,
Cloudinary storage, Supabase connections) are the things that actually break first.

The right layer is the edge rather than a counter inside a service — Render's platform
controls, or a gateway in front. Bucket4j on the auth endpoints is the in-process
fallback if the edge is not available.

This one has not simply been deferred for time. An in-process limiter behind Render's
proxy sees one source address for every request unless `X-Forwarded-For` is parsed, and
a limiter that gets that wrong throttles the entire user base as though it were a
single client — a worse failure than the gap it closes. The decision is which layer,
not whether.

### 2 · An attempt counter on the reset code

The reset flow no longer leaks who exists, and the code now comes from `SecureRandom` —
but a six-digit code has a million values and fifteen minutes of life, and nothing
counts how many guesses arrive inside that window.

A per-account counter that invalidates the code after a handful of wrong attempts is
the fix, and it is independent of item 1: rate limiting bounds the request rate, and
this bounds the total guesses against one code.

### 3 · A password policy

Nothing checks length or composition today, so a one-character password is accepted.
A minimum length is the whole of the useful part; composition rules mostly produce
worse passwords.

---

## Then the foundations

### 4 · Flyway, and `ddl-auto=validate`

`ddl-auto=update` never drops, never alters, leaves no record of what ran, and gives
the application permission to reshape production's schema at boot.

Baseline the current schema as `V1__baseline.sql`, own every change as a numbered
script, and switch to `validate` so a mismatch fails the boot instead of silently
migrating. This unblocks two things that are otherwise impossible: renaming
`catch_record` to `tb_catch_record`, and moving `catch_date` from `LocalDateTime` to
`OffsetDateTime`.

### 5 · A real test suite

One test exists — `contextLoads` — and it needs a live database. The shape it should
grow into is set out in [Running and deploying](05-deployment.md#testing). The single
most valuable first test is the JWT contract, because that is the only place where a
silent regression is an authentication bypass rather than a broken screen.

### 6 · CI

There is no pipeline in this repository. A GitHub Actions workflow running
`./mvnw verify` on JDK 21 for every push and pull request, plus a credential-format
scan over the history, is a morning's work and stops the next regression from reaching
Render.

### 7 · A local database, and a `docker-compose.yml`

`application-local.properties` currently points at the same Supabase instance the
deployed API uses, so local development writes to production data. A compose file with
PostgreSQL, and a local profile pointing at it, removes an entire category of accident.

### 8 · Backend error tracking

Sentry is in the frontend. A server-side exception appears in Render's log stream and
nowhere else. Add `sentry-spring-boot-starter`, and Spring Boot Actuator with a health
endpoint — which Render can use as a health check, and which is the prerequisite for
any monitoring at all.

While there: turn off `spring.jpa.show-sql` outside local, and replace
`e.printStackTrace()` in `JwtAuthenticationFilter` with a logger.

### 9 · An exception hierarchy

`GlobalExceptionHandler` maps every `RuntimeException` to `404`, so a genuine server
fault is reported as "not found". Introduce `NotFoundException`,
`BusinessException` and `ConflictException`, map each to its own status, and let
anything unrecognised be a `500` — which is what it is.

Bean-validation failures should return a field-keyed map rather than a
comma-joined string, so the frontend can put each message next to its input.

---

## Then the product

### 10 · Expose Swagger

`springdoc-openapi` is on the classpath and both `/v3/api-docs` and
`/swagger-ui/**` fall through to `anyRequest().authenticated()`, so the explorer cannot
load its own document. Permitting them is one line — the actual decision is whether the
deployed environment should publish its full route list, or whether the explorer should
be local-only.

Then annotate: `@Operation`, `@ApiResponse`, and a `SecurityScheme` for the bearer
token so the *Authorize* button works.

### 11 · Return what the API already stores

Two relationships are writable and not readable:

- `FishResponseDTO` carries no baits or equipment, so the recommendations written
  through `POST /api/fishes` are invisible through the API.
- `CatchRecordResponseDTO` carries `equipmentId` but no equipment name, while bait,
  fish and spot all carry both.

Neither needs a schema change.

### 12 · Deduplicate fishing spots

Every catch record posted with coordinates creates a new `tb_fishing_spot` row. Two
records from the same rock produce two spots, and the table grows once per catch rather
than once per place.

Match against existing spots within a small radius before inserting — a few dozen
metres is the right order — and reuse the match. This is the direct cost of the
project's central decision, and the decision is still right; the deduplication is the
part that was deferred.

### 13 · Enforce the closed seasons

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

### 14 · Fix `diasNaAgua`

The fisher ranking counts `COUNT(DISTINCT c.catchDate)` — distinct *timestamps*. Two
catches an hour apart on one trip count as two days. `COUNT(DISTINCT CAST(c.catchDate
AS date))` is the fix.

### 15 · Private logbooks

Every catch record is public, which is the product's premise and is right for the
leaderboards. A per-record visibility flag would let a fisher keep a spot to themselves
without leaving the app — the single most requested thing in any fishing community,
because a good spot stops being good once it is on a map.

### 16 · Sorting, properly

`sortBy` is passed straight to `Sort.by(...)`, so an unknown property becomes a runtime
failure rather than a `400`, and the direction is fixed per endpoint rather than chosen
by the caller. Validate the property against an allowlist per resource and accept a
`direction` parameter.

---

## Smaller things worth fixing

| Item | Where |
|---|---|
| `@EnableAsync` is missing, so `EmailService`'s `@Async` methods run on the request thread | `BrazilApplication` |
| `CatchRecord` has no `@Table`, so it is `catch_record` while every sibling is `tb_*` | needs item 4 first |
| `catch_date` is `LocalDateTime` — no time zone | needs item 4 first |
| `CatchRecord` declares no `equals`/`hashCode`, unlike all twelve other entities | `entity/CatchRecord.java` |
| `RiverSpeciesRepository` has no uniqueness constraint on `(river_id, fish_id)` | duplicate pairings are accepted |
| `FishService.save` drops unknown bait and equipment ids silently — `findAllById` returns what it finds | `service/FishService.java` |
| `CatchRecordRequestDTO` requires `riverId`, `latitude` and `longitude` even when `fishingSpotId` is sent and they are ignored | needs cross-field validation |
| `BaitService.findById` and `EquipmentService.findById` exist and are not routed | add `GET /{id}`, or delete them |
| `buscarComFiltro` on `CatchRecordRepository` is written and never called | wire it to a `?search=` parameter, or delete it |
| No `.dockerignore`, so `COPY . .` ships `target/` into the build stage | root |
| The `pom.xml` has empty `<name>`, `<description>`, `<licenses>` and `<scm>` blocks | root |
| `HELP.md` is the untouched Spring Initializr file | root |

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
