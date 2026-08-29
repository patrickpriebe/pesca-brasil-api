# API reference

Forty-one endpoints across ten controllers. Base path `/api`.

- [Conventions](#conventions)
- [Errors](#errors)
- [Authentication](#authentication-apiauth)
- [Catch records](#catch-records-apicatch-records)
- [Rankings](#rankings)
- [Fish](#fish-apifishes)
- [Baits](#baits-apibaits)
- [Equipment](#equipment-apiequipments)
- [Rivers](#rivers-apirivers)
- [River species](#river-species-apiriver-species)
- [Fishing spots](#fishing-spots-apifishing-spots)
- [Fishing regulations](#fishing-regulations-apifishing-regulations)
- [Images](#images-apiimages)
- [OpenAPI](#openapi)

---

## Conventions

**Authentication.** A bearer token on the `Authorization` header:

```
Authorization: Bearer <jwt>
```

Every `GET` under `/api` is public. Everything else needs a valid token, and some
handlers need more than that:

| Marker | Requirement |
|---|---|
| *(none)* | Public |
| 🔒 | A valid bearer token |
| 🔒 owner | A valid token, and the record must belong to the caller (`ROLE_ADMIN` excepted) |
| 🛡 | A valid token **and** `ROLE_ADMIN` |

`ROLE_ADMIN` is not granted by any endpoint — registration always assigns
`ROLE_PESCADOR`. Promoting an account is a deliberate database operation; the statement
is in [Security](04-security.md#two-kinds-of-write-two-different-bars), along with the
reasoning behind the whole rule set.

**Pagination.** Every collection endpoint is paged, with the same parameters:

| Parameter | Default | Meaning |
|---|---|---|
| `page` | `0` | Zero-indexed page number. Negative is `400` |
| `size` | `10` | Rows per page, capped at **100** |
| `sortBy` | varies per resource | Property to sort on, from that resource's allowlist |
| `direction` | varies per resource | `ASC` or `DESC`, case-insensitive |

`sortBy` is validated against a set declared per controller. An unknown property answers
`400` naming the ones that exist, rather than failing somewhere inside the query — which
is what happened when the value went straight to `Sort.by(...)`. The `size` ceiling is
there so one request cannot ask for the whole table and hold a connection while the
response is assembled.

The default direction is descending for catch records, newest first, and ascending
everywhere else. It is now a parameter rather than a fixed choice, so listing the oldest
records first is possible.

Responses are Spring Data `Page` objects:

```json
{
  "content": [ ... ],
  "pageable": { "pageNumber": 0, "pageSize": 10, ... },
  "totalElements": 42,
  "totalPages": 5,
  "last": false,
  "first": true,
  "numberOfElements": 10,
  "empty": false
}
```

**Status codes actually used.**

| Code | When |
|---|---|
| `200` | Successful read, successful auth operation |
| `201` | Resource created |
| `204` | Deleted |
| `400` | Validation failure, a violated rule, bad credentials, a disabled account |
| `401` | Missing, expired or invalid token |
| `403` | Authenticated, but the handler requires `ROLE_ADMIN` |
| `404` | Not found — **and what somebody else's catch record answers** |
| `409` | E-mail already registered, account already verified, association already exists |
| `413` | Upload above the 5 MB multipart cap |
| `429` | Rate limit on `/api/auth/**`, with `Retry-After` |
| `500` | Anything unexpected; logged with its stack trace |

---

## Errors

Every failure returns the same envelope, built by `GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-08-27T14:03:11.204",
  "status": 404,
  "error": "Não Encontrado",
  "message": "Peixe não encontrado.",
  "path": "/api/catch-records"
}
```

Each domain exception carries its own status, so the client can act on the code instead
of reading the sentence:

| Exception | Status | Meaning |
|---|---|---|
| `NotFoundException` | `404` | The referenced resource does not exist — **or exists and is not yours** |
| `BusinessException` | `400` | Well-formed request, rejected rule |
| `ConflictException` | `409` | The current state forbids it: e-mail taken, account already verified, association already exists |
| `TooManyRequestsException` | `429` | Rate limit, with a `Retry-After` header |
| `AuthenticationException` | `400` | Bad credentials, disabled account |
| `AccessDeniedException` | `403` | Authenticated, but the handler wants `ROLE_ADMIN` |
| anything else | `500` | Logged with its stack trace; the body says nothing about the internals |

**Validation failures add a `fieldErrors` map**, so each message can be rendered next to
its input rather than as one concatenated sentence:

```json
{
  "timestamp": "2026-08-27T14:03:11.204",
  "status": 400,
  "error": "Erro de Validação",
  "message": "O peixe é obrigatório., A data e hora da captura são obrigatórias.",
  "path": "/api/catch-records",
  "fieldErrors": {
    "fishId": "O peixe é obrigatório.",
    "catchDate": "A data e hora da captura são obrigatórias."
  }
}
```

The field is omitted entirely when there is nothing to put in it.

> The previous version mapped *every* `RuntimeException` to `404`, because the
> overwhelmingly common case was "the id you referenced does not exist". A rule that
> made the common case right therefore also reported a genuine server fault as *not
> found*, and no client could tell the two apart.

**Error messages are in Portuguese.** They are written to be shown to the end user of a
Brazilian product, and the frontend displays them directly. Identifiers, table names
and this documentation are in English.

---

## Authentication · `/api/auth`

All five endpoints are public, and all five are **rate limited**: twenty requests per
minute per source address, answering `429` with `Retry-After` beyond that. See
[Security](04-security.md#rate-limiting).

Successes return a JSON object — `{"message": "..."}`, plus `"id"` on registration —
and failures return the error envelope. Neither is a bare string any more; the previous
version caught `RuntimeException` in each handler and wrote the message as plain text,
which collapsed conflict, validation and internal failure into one `400`.

### `POST /api/auth/register`

```json
{
  "name": "Patrick",
  "email": "fisher@example.com",
  "password": "...",
  "recaptchaToken": "03AGdBq26..."
}
```

Verifies the reCAPTCHA token with Google, requires a password of at least **eight
characters**, rejects a duplicate e-mail, hashes with BCrypt, creates the account
**disabled**, generates a six-digit code valid for fifteen minutes, and e-mails it.

```json
{ "message": "Usuário registrado com sucesso! Verifique o seu e-mail.", "id": "42" }
```

| Response | When |
|---|---|
| `200` | Created |
| `400` | Missing field, malformed e-mail, password under eight characters, captcha refused |
| `409` | E-mail already registered |

### `POST /api/auth/verify`

```json
{ "email": "fisher@example.com", "code": "482915" }
```

Enables the account and clears the code, its expiry and the attempt counter. Rejects an
already-verified account (`409`), an expired code and a wrong code — expiry first, for
the reason in [Security](04-security.md#the-registration-handshake).

**Five wrong codes destroy the code.** The sixth attempt fails even if it is correct,
because there is nothing left to match, and a new one has to be requested through
*forgot password*.

### `POST /api/auth/login`

```json
{ "email": "...", "password": "...", "recaptchaToken": "..." }
```

Verifies the captcha, authenticates through Spring Security's `AuthenticationManager`
— which refuses a disabled account — and issues a token valid for 24 hours.

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "name": "Patrick",
  "email": "fisher@example.com",
  "role": "ROLE_PESCADOR"
}
```

The `role` field is one role, chosen deterministically (alphabetically) when an account
holds more than one — `ROLE_ADMIN` sorts before `ROLE_PESCADOR`. It is a shortcut for
the frontend's rendering, and it is **not** what the server authorizes on: the server
reads the full set from the database on every request.

### `POST /api/auth/forgot-password`

```json
{ "email": "fisher@example.com", "recaptchaToken": "03AGdBq26..." }
```

Verifies the captcha, then writes a fresh six-digit code and e-mails it — **if the
account exists**. When it does not, nothing happens and the response is identical:

```
200 → "Se existir uma conta com este e-mail, enviamos um código de recuperação."
```

The only signal that an address is registered is the message that arrives in that
inbox. See [Security](04-security.md#the-reset-flow-does-not-confirm-who-exists).

### `POST /api/auth/reset-password`

```json
{ "email": "...", "code": "482915", "newPassword": "..." }
```

Requires a new password of at least eight characters, checks the expiry, then the code,
then re-hashes and clears the code, the expiry and the attempt counter.

An unknown e-mail, a wrong code and an expired code all return the identical message —
*"Código de segurança inválido ou expirado. Solicite um novo."* — so this endpoint
cannot be used as the membership check the previous one refuses to be. The same
five-attempt ceiling applies.

---

## Catch records · `/api/catch-records`

The centre of the product.

### `GET /api/catch-records`

Public. Paged, sorted by `catchDate` **descending** by default. Sortable on `catchDate`,
`weightInKg`, `lengthInCm` and `id`.

Returns every record from every fisher — the logbook is a shared feed, not a private
journal. The response carries `userName` and no other identifying field.

`?search=` filters by species common name or fishing-spot name, case-insensitively:

```
GET /api/catch-records?search=dourado&direction=ASC
```

Each item carries the bait, the fish and the spot as id **and** name, and the equipment
as id and `equipmentType` — the type used to be missing, which forced the client to
fetch the whole equipment list just to render one row of the logbook.

### `GET /api/catch-records/me` 🔒

The authenticated fisher's own records, same shape and same parameters. The identity
comes from the token, so there is no user id in the URL to tamper with.

### `POST /api/catch-records` 🔒

The one endpoint with real behaviour. Requires a token; the owner is taken from the
`SecurityContext` and the request body has no user field at all.

```json
{
  "fishId": 3,
  "riverId": 7,
  "latitude": -25.5163,
  "longitude": -54.5854,
  "spotName": "Remanso da ponte",
  "catchDate": "2026-08-24T06:30:00",
  "baitId": 12,
  "equipmentId": 4,
  "weightInKg": 6.2,
  "lengthInCm": 78.0,
  "weatherCondition": "CLOUDY",
  "moonPhase": "WAXING",
  "outcome": "RELEASED",
  "photoUrl": "https://res.cloudinary.com/.../catch.jpg",
  "notes": "Pegou na correnteza, logo depois do amanhecer."
}
```

| Field | Required | Notes |
|---|---|---|
| `fishId` | yes | Must exist |
| `catchDate` | yes | ISO-8601 local date-time |
| `fishingSpotId` | **one of the two** | An existing spot |
| `riverId` + `latitude` + `longitude` | **one of the two** | A pin on the map |
| `spotName` | no | Names the spot being created; defaults to `"Ponto no <river>"` |
| `baitId`, `equipmentId` | no | Must exist if sent |
| `weightInKg`, `lengthInCm` | no | Must be greater than zero if sent |
| everything else | no | |

**Two mutually exclusive modes**, decided by whether `fishingSpotId` is null:

- `fishingSpotId` **absent** → the coordinate *is* the spot. An existing spot on the
  same river within roughly 55 m is reused; otherwise a new `tb_fishing_spot` row is
  created in the same transaction and the record points at it.
- `fishingSpotId` **present** → it is resolved, and the coordinates are ignored — and
  no longer required, so the payload stops carrying fields the API discards.

Cross-field validation enforces that exactly one of the two paths is complete, and that
a coordinate is on the planet at all. Both messages arrive in `fieldErrors` under
`localizacaoInformada` and `coordenadaValida`.

`201` with the created record. Enum values are the literal names:
`SUNNY | CLOUDY | RAINY | WINDY`, `NEW | WAXING | FULL | WANING`, `RELEASED | KEPT`.

### `DELETE /api/catch-records/{id}` 🔒 owner

`204`. Requires a token, and the record must belong to the caller — the service loads
it and compares its owner's e-mail against the authenticated principal. `ROLE_ADMIN`
passes regardless.

**A record belonging to somebody else answers exactly like one that does not exist**:
`404` with *"Registro de captura não encontrado."* A distinct `403` would confirm the
record exists to whoever is walking the id space.

---

## Rankings

Three public leaderboards, all under `/api/catch-records`, all returning plain arrays
rather than pages — each is capped at ten rows by the query itself.

| Endpoint | Ranks by |
|---|---|
| `GET /api/catch-records/ranking/comprimento` | Longest fish, nulls excluded |
| `GET /api/catch-records/ranking/peso` | Heaviest fish, nulls excluded |
| `GET /api/catch-records/ranking/pescadores` | Fishers, by number of records |

The first two share a response shape:

```json
[
  {
    "posicao": 1,
    "pescador": "Patrick",
    "especie": "Dourado",
    "local": "Remanso da ponte",
    "medida": 78.0,
    "fotoUrl": "https://res.cloudinary.com/..."
  }
]
```

`medida` is centimetres or kilograms depending on which endpoint answered — the same
field carries a different unit, which the client must know from the URL it called.

The fisher ranking is a projection over an aggregate:

```json
[ { "posicao": 1, "pescador": "Patrick", "capturas": 37, "diasNaAgua": 12 } ]
```

`diasNaAgua` is `COUNT(DISTINCT c.catchDate)` — distinct *timestamps*, not distinct
calendar days. Two catches an hour apart on the same trip count as two. The field name
promises a day and the query counts a moment; the fix belongs in the query
(`CAST(c.catchDate AS date)`) and is on the roadmap.

The ranking positions are computed in Java by incrementing a counter over an ordered
list, so ties are broken arbitrarily by whatever order the database returned.

---

## Fish · `/api/fishes`

| Method | Path | Auth | Notes |
|---|---|---|---|
| `GET` | `/api/fishes` | public | `?name=` case-insensitive partial match; sorts on `commonName` |
| `GET` | `/api/fishes/{id}` | public | `404` if absent |
| `POST` | `/api/fishes` | 🛡 | Validated |
| `DELETE` | `/api/fishes/{id}` | 🛡 | `204` |

```json
{
  "commonName": "Dourado",
  "scientificName": "Salminus brasiliensis",
  "conservationStatus": "Vulnerável",
  "description": "...",
  "imageUrl": "https://...",
  "recommendedBaitIds": [1, 4],
  "recommendedEquipmentIds": [2]
}
```

`commonName` and `scientificName` are `@NotBlank`. The two id lists write the
recommendation join tables, and **an id that does not exist is refused with `404`** —
`findAllById` returns only what it finds, so an unknown id used to vanish and the client
got `201` with one recommendation fewer than it asked for.

The response returns them in full, which is what turns a catalogue into advice:

```json
{
  "id": 3,
  "commonName": "Dourado",
  "scientificName": "Salminus brasiliensis",
  "conservationStatus": "Vulnerável",
  "description": "...",
  "imageUrl": "https://...",
  "recommendedBaits": [
    { "id": 1, "name": "Colher giratória", "type": "ARTIFICIAL", "description": "..." }
  ],
  "recommendedEquipments": [
    { "id": 2, "type": "CARRETILHA", "recommendedLineWeight": "17–30 lb", "action": "Média-rápida" }
  ]
}
```

Sortable on `commonName`, `scientificName`, `conservationStatus` and `id`.

---

## Baits · `/api/baits`

| Method | Path | Auth |
|---|---|---|
| `GET` | `/api/baits` — `?name=` partial match, sorts on `name`, `type`, `id` | public |
| `GET` | `/api/baits/{id}` | public |
| `POST` | `/api/baits` | 🛡 |
| `DELETE` | `/api/baits/{id}` | 🛡 |

```json
{ "name": "Colher giratória", "type": "ARTIFICIAL", "description": "..." }
```

`type` is `ARTIFICIAL` or `NATURAL`, and both it and `name` are required.

---

## Equipment · `/api/equipments`

| Method | Path | Auth |
|---|---|---|
| `GET` | `/api/equipments` — sorts on `type`, `recommendedLineWeight`, `action`, `id` | public |
| `GET` | `/api/equipments/{id}` | public |
| `POST` | `/api/equipments` | 🛡 |
| `DELETE` | `/api/equipments/{id}` | 🛡 |

```json
{ "type": "CARRETILHA", "recommendedLineWeight": "17–30 lb", "action": "Média-rápida" }
```

`type` is `MOLINETE` or `CARRETILHA`, and it is required.

---

## Rivers · `/api/rivers`

| Method | Path | Auth |
|---|---|---|
| `GET` | `/api/rivers` — `?name=` partial match, sorts on `name` | public |
| `GET` | `/api/rivers/{id}` | public |
| `POST` | `/api/rivers` | 🛡 |
| `DELETE` | `/api/rivers/{id}` | 🛡 |

```json
{ "name": "Rio Paraná", "hydrographicBasin": "Bacia do Paraná", "description": "..." }
```

`hydrographicBasin` is free text and is the string that
[fishing regulations](#fishing-regulations-apifishing-regulations) are matched against.

---

## River species · `/api/river-species`

Which species live where, with how much of it and when.

| Method | Path | Auth |
|---|---|---|
| `GET` | `/api/river-species` — sorts on `id` | public |
| `GET` | `/api/river-species/river/{riverId}` | public |
| `POST` | `/api/river-species` | 🛡 |
| `DELETE` | `/api/river-species/{id}` | 🛡 |

```json
{ "riverId": 7, "fishId": 3, "abundance": "ALTA", "bestSeason": "Setembro a Novembro" }
```

`abundance` is `ALTA | MEDIA | BAIXA | RARA`, and river, fish and abundance are all
required. The response denormalises both sides — `riverName` and `fishCommonName` — so
the client renders the pairing without a second call.

**The pair is unique.** Associating a species with a river it is already associated with
answers `409`: "dourado is `ALTA` in the Paraná" is one fact, and two rows saying
different things about it make neither true. The service enforces it; the matching
`UNIQUE(river_id, fish_id)` needs the migration described in
[Roadmap item 1](06-roadmap.md#1--flyway-and-ddl-autovalidate), because a constraint
cannot be added to a table that already holds duplicates.

---

## Fishing spots · `/api/fishing-spots`

| Method | Path | Auth |
|---|---|---|
| `GET` | `/api/fishing-spots` — sorts on `name` | public |
| `GET` | `/api/fishing-spots/river/{riverId}` | public |
| `GET` | `/api/fishing-spots/{id}` | public |
| `POST` | `/api/fishing-spots` | 🛡 |
| `DELETE` | `/api/fishing-spots/{id}` | 🛡 |

```json
{
  "riverId": 7,
  "name": "Remanso da ponte",
  "latitude": -25.5163,
  "longitude": -54.5854,
  "accessType": "Barco",
  "description": "..."
}
```

This is the curated path into `tb_fishing_spot`. The other path is
`POST /api/catch-records` — see [Data model](02-data-model.md#geography).

---

## Fishing regulations · `/api/fishing-regulations`

Closed seasons — *piracema* — by hydrographic basin.

| Method | Path | Auth |
|---|---|---|
| `GET` | `/api/fishing-regulations` — `?basin=` partial match, sorts on `hydrographicBasin` | public |
| `POST` | `/api/fishing-regulations` | 🛡 |
| `DELETE` | `/api/fishing-regulations/{id}` | 🛡 |

```json
{
  "hydrographicBasin": "Bacia do Paraná",
  "startDate": "2026-11-01",
  "endDate": "2027-02-28",
  "notes": "Proibida a pesca de espécies nativas."
}
```

`endDate` must not precede `startDate` — a closed season that ends before it starts
never closes, and the search by basin would return it as a valid period.

The API stores and serves the periods. **It does not enforce them** — posting a catch
record dated inside a closed season is accepted. That is a product question before it is
a code one, and it is [roadmap item 2](06-roadmap.md#2--enforcing-the-closed-seasons):
refusing the record punishes honest reporting and will produce false dates, so accepting
and flagging is the likelier answer.

---

## Images · `/api/images`

### `POST /api/images/upload` 🔒

`multipart/form-data`, field name `file`. Uploads to Cloudinary and returns the URL.

```json
{ "url": "https://res.cloudinary.com/.../catch.jpg" }
```

| Constraint | Enforced by |
|---|---|
| Requires a bearer token | The filter chain — there is no `permitAll` for `/api/images/**` |
| Maximum **5 MB** | `spring.servlet.multipart.max-file-size` and `max-request-size` |
| `Content-Type` must start with `image/` | The handler |
| The file must not be empty | The handler |

| Response | When |
|---|---|
| `200` `{"url": "..."}` | Uploaded |
| `400` `{"error": "Nenhum arquivo foi enviado."}` | Empty file |
| `400` `{"error": "O arquivo enviado precisa ser uma imagem."}` | Wrong or missing content type |
| `500` `{"error": "Falha ao fazer upload da imagem"}` | `IOException` reaching Cloudinary |

The client is expected to upload first and then send the returned URL as `photoUrl` on
the catch record. Splitting the two means a slow upload does not hold a database
transaction open, and a failed upload does not lose the rest of the form.

The content type is a header the client chooses, so it is not proof of anything — it is
there to stop the endpoint becoming general-purpose file storage by accident. Checking
the actual bytes is Cloudinary's job.

---

## OpenAPI

`springdoc-openapi` 2.6.0 publishes:

- `/v3/api-docs` — the generated OpenAPI 3 document
- `/swagger-ui/index.html` — the interactive explorer

Both are gated by **`pescabrasil.openapi.public`**, which defaults to `false`. The local
example configuration turns it on, so the explorer is available at
`http://localhost:8080/swagger-ui/index.html` while developing; the deployed environment
leaves it closed, because publishing a complete route list is a decision rather than a
side effect of having the dependency on the classpath.

`OpenApiConfig` declares a `bearer-jwt` security scheme, so the explorer's *Authorize*
button works: paste a token from `POST /api/auth/login` and the protected endpoints
become callable. Without the scheme, springdoc would build every request without an
`Authorization` header and each protected route would answer `401` — which reads as a
broken API rather than a missing button.

Handlers are annotated with `@Tag` and `@Operation`, so the explorer groups them by
resource and states what each one needs. `@ApiResponse` examples are not written yet
([roadmap](06-roadmap.md#smaller-things-worth-fixing)).
