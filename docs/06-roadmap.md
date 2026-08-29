# Roadmap

What is missing, what was closed, and what is deliberately out of scope.

Every item here is referenced from somewhere else in the documentation. Nothing on this
list is a surprise to the code — the point of writing it down is that a limitation
nobody has named is a limitation nobody fixes.

- [Done](#done)
- [Blocked on a decision](#blocked-on-a-decision)
- [Next](#next)
- [Smaller things worth fixing](#smaller-things-worth-fixing)
- [Deliberately out of scope](#deliberately-out-of-scope)

---

## Done

Kept on the list rather than deleted, so the reasoning survives the fix.

### Authorization

| Was | Now |
|---|---|
| **Roles never enforced.** `ROLE_ADMIN` was seeded, assigned and returned on login, and no endpoint checked it | `@EnableMethodSecurity`, and `@PreAuthorize("hasRole('ADMIN')")` on all fourteen catalogue write handlers. Catch records stay writable by any verified fisher |
| **Catch-record deletion authenticated but not owned** | The service loads the record, compares its owner against the principal, and excepts `ROLE_ADMIN`. Not-yours answers as not-found |
| **Image upload open and unbounded** | Token required, 5 MB cap in configuration, `image/*` only, empty file refused |
| **Anonymous writes answered `403` with an empty body** | An `AuthenticationEntryPoint` answers `401` and an `AccessDeniedHandler` answers `403`, both in the API's error envelope — *authenticate first* and *you may not* stopped being the same response |

### Accounts

| Was | Now |
|---|---|
| **`jwt.secret` had a committed fallback** | `${JWT_SECRET}` with no default, so a missing variable fails the boot. `JwtUtil` refuses a secret shorter than 32 characters at construction |
| **`forgot-password` had no captcha and confirmed account existence** | Captcha added; identical response either way; `reset-password` collapsed to one message for unknown e-mail, wrong code and expired code |
| **OTP from `java.util.Random`, range excluded `999999`** | `SecureRandom` and `nextInt(1_000_000)` |
| **No attempt counter on the six-digit code** | Five wrong guesses destroy the code. The counter lives in `verification_attempts`, nullable so `ddl-auto=update` can add it to a populated table |
| **No password policy** | Minimum of eight characters, enforced in `UserService` — so it covers the reset flow too — and declared on the DTOs so the message lands on the right field |
| **Nothing rate limited** | Fixed window per source address over `/api/auth/**`, with `server.forward-headers-strategy=framework` so it sees the real client behind Render's proxy |

### Correctness

| Was | Now |
|---|---|
| **Every `RuntimeException` became `404`** | `NotFoundException`, `BusinessException`, `ConflictException` and `TooManyRequestsException`, each with its own status; anything unrecognised is a logged `500` |
| **Validation errors arrived comma-joined** | `fieldErrors`, a map from field to message, alongside the summary |
| **`sortBy` went straight to `Sort.by(...)`** | An allowlist per resource, a `direction` parameter, and a ceiling on `size` |
| **`@EnableAsync` was missing**, so `@Async` mail ran on the request thread inside the transaction | Enabled on `BrazilApplication` |
| **Every coordinate created a new fishing spot** | A spot within roughly 55 m on the same river is reused; the table grows once per place |
| **`diasNaAgua` counted distinct timestamps** | `COUNT(DISTINCT CAST(c.catchDate AS date))` |
| **`FishService.save` dropped unknown ids silently** | The resolved count is compared against the requested one and a missing id is refused |
| **`CatchRecordRequestDTO` demanded map fields even with `fishingSpotId`** | `@AssertTrue` cross-field checks: exactly one of the two paths must be complete, and coordinates must be on the planet |
| **Delete by id ignored whether the row existed** | Every `delete` checks first and answers `404` |
| **`RiverSpecies` accepted duplicate river + fish pairs** | Refused with `409`. The database constraint still needs the migration |
| **Recommendations were writable and unreadable** | `FishResponseDTO` carries `recommendedBaits` and `recommendedEquipments`; `CatchRecordResponseDTO` carries `equipmentType` |
| **`buscarComFiltro` was written and never called** | Wired to `GET /api/catch-records?search=` |
| **`findById` on baits and equipment was unrouted** | `GET /api/baits/{id}` and `GET /api/equipments/{id}` |
| **`e.printStackTrace()` in the JWT filter** | SLF4J, and the `401` bodies are JSON |

### Operations

| Was | Now |
|---|---|
| **One test, and it needed a live database** | 43 tests. `src/test/resources/application.properties` points the suite at H2, so it runs anywhere |
| **No CI** | `.github/workflows/ci.yml`: `./mvnw verify` on JDK 21, a credential-format scan over the whole history, and a Docker build |
| **Local development shared the deployed database** | `docker-compose.yml` with PostgreSQL on 5433, and `application-local.properties.example` pointing at it |
| **Swagger unreachable** | `pescabrasil.openapi.public` opens `/swagger-ui` and `/v3/api-docs`; the local example turns it on, the deployed default leaves it off. `OpenApiConfig` declares the bearer scheme so *Authorize* works |
| **No health endpoint** | Actuator with `health` exposed and `show-details=never`; the probe is public, the rest of Actuator is not |
| **`show-sql` on in every environment** | Off by default, on in the local example |
| **No `.dockerignore`; `COPY . .` shipped `target/`** | Added, and the Dockerfile resolves dependencies in their own layer and runs as a non-root user on a JRE base |
| **CORS origins hard-coded** | `pescabrasil.cors.allowed-origins`, defaulting to the two real origins |
| **`pom.xml` had empty metadata blocks; `HELP.md` was the untouched Initializr file** | Filled in; removed |

---

## Blocked on a decision

Not deferred for time. Each needs an answer that is not the code's to give.

### 1 · Flyway, and `ddl-auto=validate`

`ddl-auto=update` never drops, never alters, leaves no record of what ran, and gives the
application permission to reshape production's schema at boot. Replacing it means
baselining **the schema that actually exists in Supabase today**, not the one the
entities imply — those two have drifted by definition, because `update` only ever adds.

The safe sequence is: dump the live schema, commit it as `V1__baseline.sql`, set
`spring.flyway.baseline-on-migrate=true`, then switch `ddl-auto` to `validate` and watch
the boot fail until the baseline and the entities agree. That first failure is the point
of the exercise; doing it from a guessed baseline would put a wrong schema in the
repository and break the deploy.

It blocks three other items:

- renaming `catch_record` to `tb_catch_record`, so it matches its twelve `tb_*` siblings;
- moving `catch_date` from `LocalDateTime` to a zoned type;
- `UNIQUE(river_id, fish_id)` on `tb_river_species` — the service refuses duplicates now,
  but a constraint cannot be added to a table that already holds some.

### 2 · Enforcing the closed seasons

`tb_fishing_regulation` stores *piracema* periods and nothing consults them. The product
question comes before the code: refuse a catch record dated inside a closed season, or
accept it and mark it?

Refusing punishes honest reporting and will produce false dates — the fisher who logged
the truth is the one who gets blocked. Accepting and flagging keeps the record accurate
and still says what the rule was. That is the recommendation, and it is a call about
what the product is for.

It also needs the basin promoted from free text to its own table: matching
`tb_river.hydrographic_basin` against `tb_fishing_regulation.hydrographic_basin` by
string is what would make the check unreliable.

### 3 · Private logbooks

Every catch record is public, which is the product's premise and is right for the
leaderboards. A per-record visibility flag would let a fisher keep a spot to themselves —
the single most requested thing in any fishing community, because a good spot stops
being good once it is on a map.

It changes what the product *is*, and it changes the rankings: a private record that
still competes leaks the spot through the leaderboard anyway.

---

## Next

Ordinary work, in rough order.

### 4 · Backend error tracking

A server-side exception now reaches the logger rather than stdout, and Actuator answers
a health probe — but nothing aggregates or alerts. `sentry-spring-boot-starter` needs a
DSN, which needs an account decision; the frontend already has one, and pointing both at
the same project is the obvious move.

### 5 · Rate limiting at the edge

The in-process limiter is correct for one instance and honest about its limits: the
state is in memory, so it resets on restart and is not shared. From two instances the
effective limit doubles. Moving it to Render's platform controls or a gateway is the
fix, and the in-process one stays as the fallback.

### 6 · Widen the test suite

43 tests cover the JWT contract, the account flows, the catch-record branches, the
authorization rules on one controller and the pageable factory. What is not covered:
the other six controllers' authorization (the rule is identical, so a parameterised test
would do), the Cloudinary and reCAPTCHA integrations against a stubbed HTTP server, and
the derived queries and the ranking projection against a real PostgreSQL.

### 7 · N+1 on the fish catalogue

`FishResponseDTO` now carries the recommendations, and each fish loads its two
collections separately — a page of ten costs twenty extra queries. An `@EntityGraph`
would fix `findById`; for the paged list it would force pagination in memory, so the
answer there is either two batched queries or a list without recommendations.

### 8 · An admin path for the catalogue

`ROLE_ADMIN` is granted by a SQL statement. That is correct — a registration endpoint
able to hand out the role that guards the catalogue would not be guarding it — but there
is no path for an existing administrator to promote somebody, which is a different thing.

### 9 · The frontend's role guard

The Angular `authGuard` checks only that a session exists, so a `ROLE_PESCADOR` account
still reaches the management screens and gets `403` on the first write. Worse, the HTTP
interceptor logs out on `403` as well as `401`, so the person is signed out instead of
told. Both live in [pesca-brasil-ui](https://github.com/patrickpriebe/pesca-brasil-ui).

---

## Smaller things worth fixing

| Item | Where |
|---|---|
| `CatchRecord` declares no `equals`/`hashCode`, unlike all twelve other entities | `entity/CatchRecord.java` |
| The fish rankings break ties by whatever order the database returned | `CatchRecordService` |
| `RankingPeixeResponseDTO.medida` carries centimetres or kilograms depending on the URL called | `dto/response/ranking/` |
| A handful of Portuguese identifiers survive — `buscarComFiltro`, `RankingPescadorProjection`, `verificarFiltroAntiRobo` | across the codebase |
| The rate limiter's window is fixed, not sliding, so a burst can straddle two windows | `config/ratelimit/RateLimitFilter.java` |
| `@Operation` annotations exist; `@ApiResponse` examples do not | controllers |

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
