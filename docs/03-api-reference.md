# API reference

Thirty-eight endpoints across ten controllers. Base path `/api`.

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

**Pagination.** Every collection endpoint is paged, with the same four parameters:

| Parameter | Default | Meaning |
|---|---|---|
| `page` | `0` | Zero-indexed page number |
| `size` | `10` | Rows per page |
| `sortBy` | varies per resource | Property name to sort on |
| direction | — | Fixed per endpoint, not a parameter |

The direction is not configurable: catch records sort **descending** (newest first),
everything else **ascending**. That is a deliberate simplification, and its cost is
recorded in [Roadmap](06-roadmap.md) along with the fact that `sortBy` is passed
straight to `Sort.by(...)`.

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
| `400` | Bean validation failure, or any failure inside `/api/auth/**` |
| `401` | Missing, expired or invalid token |
| `403` | Authenticated, but the handler requires `ROLE_ADMIN` |
| `404` | Not found — and, see below, most other business failures. Also what a catch record belonging to somebody else answers |

---

## Errors

Bean validation failures and uncaught `RuntimeException`s are handled by
`GlobalExceptionHandler` and return the same envelope:

```json
{
  "timestamp": "2026-08-27T14:03:11.204",
  "status": 400,
  "error": "Erro de Validação",
  "message": "O peixe é obrigatório.",
  "path": "/api/catch-records"
}
```

Validation errors are joined into a single `message`, comma-separated, rather than
returned as a field-keyed map.

> **A known rough edge.** The handler maps *every* `RuntimeException` to `404`, because
> the overwhelmingly common case is "the id you referenced does not exist". A rule that
> exists to make the common case right therefore also answers `404` for a genuine
> server fault. Replacing it with a small exception hierarchy — `NotFoundException`,
> `BusinessException` — is on the [roadmap](06-roadmap.md).
>
> The auth endpoints are unaffected: `AuthController` catches `RuntimeException`
> itself and returns `400` with the message as a plain string body.

**Error messages are in Portuguese.** They are written to be shown to the end user of a
Brazilian product, and the frontend displays them directly. Identifiers, table names
and this documentation are in English.

---

## Authentication · `/api/auth`

All five endpoints are public. All five return a plain string body on failure, not the
error envelope.

### `POST /api/auth/register`

```json
{
  "name": "Patrick",
  "email": "fisher@example.com",
  "password": "...",
  "recaptchaToken": "03AGdBq26..."
}
```

Verifies the reCAPTCHA token with Google, rejects a duplicate e-mail, hashes the
password with BCrypt, creates the account **disabled**, generates a six-digit code
valid for fifteen minutes, and e-mails it.

`200` → `"Usuário registrado com sucesso! ID: 42"`
`400` → the reason, as a string

### `POST /api/auth/verify`

```json
{ "email": "fisher@example.com", "code": "482915" }
```

Enables the account and clears the code and its expiry. Rejects an already-verified
account, an expired code and a wrong code, in that order.

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

The `role` field is the *first* role on the user, which is exact today because every
account has exactly one. It is a shortcut, and it is the frontend's only source for
what to render.

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

Checks the expiry, then the code, then re-hashes and clears both columns.

An unknown e-mail, a wrong code and an expired code all return the same string —
*"Código de segurança inválido ou expirado. Solicite um novo."* — so this endpoint
cannot be used as the membership check the previous one refuses to be.

---

## Catch records · `/api/catch-records`

The centre of the product.

### `GET /api/catch-records`

Public. Paged, sorted by `catchDate` **descending** by default.

Returns every record from every fisher — the logbook is a shared feed, not a private
journal. The response carries `userName` and no other identifying field.

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
| `riverId` | yes | Must exist |
| `latitude`, `longitude` | yes | *"Marque o ponto no mapa."* |
| `catchDate` | yes | ISO-8601 local date-time |
| `fishingSpotId` | no | **Send this instead** to attach to an existing spot |
| `spotName` | no | Names the spot being created; defaults to `"Ponto no <river>"` |
| `baitId`, `equipmentId` | no | Must exist if sent |
| everything else | no | |

**Two mutually exclusive modes**, decided by whether `fishingSpotId` is null:

- `fishingSpotId` **absent** and `riverId` + `latitude` present → a new
  `tb_fishing_spot` row is created in the same transaction and the record points at it.
- `fishingSpotId` **present** → it is resolved, and the coordinates are ignored.

Bean validation marks `riverId`, `latitude` and `longitude` as required, so the second
mode still needs them in the payload even though it discards them. That inconsistency
is on the [roadmap](06-roadmap.md).

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
recommendation join tables — but note that **`FishResponseDTO` does not return them**,
so what you write here is not visible through the API.

---

## Baits · `/api/baits`

| Method | Path | Auth |
|---|---|---|
| `GET` | `/api/baits` — `?name=` partial match, sorts on `name` | public |
| `POST` | `/api/baits` | 🛡 |
| `DELETE` | `/api/baits/{id}` | 🛡 |

```json
{ "name": "Colher giratória", "type": "ARTIFICIAL", "description": "..." }
```

`type` is `ARTIFICIAL` or `NATURAL`. There is no `GET /{id}` — `BaitService.findById`
exists but no controller exposes it.

---

## Equipment · `/api/equipments`

| Method | Path | Auth |
|---|---|---|
| `GET` | `/api/equipments` — sorts on `id` | public |
| `POST` | `/api/equipments` | 🛡 |
| `DELETE` | `/api/equipments/{id}` | 🛡 |

```json
{ "type": "CARRETILHA", "recommendedLineWeight": "17–30 lb", "action": "Média-rápida" }
```

`type` is `MOLINETE` or `CARRETILHA`. As with baits, `findById` exists on the service
and is not routed.

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

`abundance` is `ALTA | MEDIA | BAIXA | RARA`. The response denormalises both sides —
`riverName` and `fishCommonName` — so the client renders the pairing without a second
call.

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

The API stores and serves the periods. **It does not enforce them** — posting a catch
record dated inside a closed season is accepted. Deciding what to do about that is a
product question, and it is on the [roadmap](06-roadmap.md).

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

`springdoc-openapi` 2.6.0 is on the classpath, which publishes:

- `/v3/api-docs` — the generated OpenAPI 3 document
- `/swagger-ui/index.html` — the interactive explorer

**Neither is reachable without a token.** `SecurityConfig` permits `/api/auth/**`,
`/api/images/**` and any `GET /api/**`; both springdoc paths fall outside all three and
land on `anyRequest().authenticated()`. Since the only way to get a token is
`POST /api/auth/login`, Swagger UI cannot load its own document in a browser without
one.

Running locally the same rule applies. Getting the explorer back is one line in
`SecurityConfig` and is on the [roadmap](06-roadmap.md) — deliberately, because opening
it also means deciding whether the deployed environment should publish its full route
list.
