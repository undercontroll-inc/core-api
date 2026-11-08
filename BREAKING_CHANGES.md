# Breaking API changes

This document lists every contract change from the REST semantics refactor. Use it to update consumers (especially `frontend`). Frontend call sites already identified:

- `frontend/src/services/UserService.js`
- `frontend/src/services/RepairService.js`

Bruno: `Bruno Collection/`. Yaak: `Yaak Collection/` (File → Open Workspace).

## Status codes (all resources)

| Situation | Old | New |
|---|---|---|
| `DELETE` success | `200` on most resources (`204` only on orders) | Always `204 No Content` |
| `GET` collection with no results | `204` on users, order-items, components, demands | Always `200` with `[]` |
| Single-resource `GET` missing | `404` (unchanged), except latest announcement used `204` | Always `404` |

Error responses now always include a stable `code` field (for example `USER_NOT_FOUND`, `VALIDATION_ERROR`, `INTERNAL_SERVER_ERROR`). Validation failures (`400`) also include `errors: [{ field, message }]`. Messages are in English. `timestamp` is now UTC (`ZoneOffset.UTC`).

---

## CSRF and CORS (every browser request)

These apply on localhost too (`http://localhost:3000` / `5173` talking to `http://localhost:8080`). Different ports are different origins.

### CSRF (not required on `/v1/api/**`)

The REST API (`/v1/api/**`) does not require CSRF. Auth is `Authorization: Bearer` plus refresh in the JSON body. There is no session cookie, so the browser does not send credentials on its own; CORS already blocks cross-site reads of the response.

Do not send `X-XSRF-TOKEN`. Do not call `GET /v1/api/auth` (that warmup endpoint does not exist). Axios should omit `withCredentials` (or set it to `false`).

### CORS (stricter)

Old: `applyPermitDefaultValues()` allowed any origin (`*`), plus `TRACE` / `HEAD`.

New:

- Allowed origins come from `undercontroll.cors.allowed-origins` (`CORS_ALLOWED_ORIGINS`). Defaults: `http://localhost:3000`, `http://localhost:5173`. Production **must** set the real frontend origin.
- Methods: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` only.
- Request headers allowed: `Authorization`, `Content-Type`, `Accept`. Extra custom headers will fail preflight.
- Exposed response headers: `Content-Type`.
- `Access-Control-Allow-Credentials: false` — there is no auth cookie.

---

## Auth (was under Users)

| Old | New | Notes |
|---|---|---|
| `POST /v1/api/users/auth` | `POST /v1/api/auth` | Body now includes `provider`. Password login: `{ "provider": "PASSWORD", "email", "password" }`. **Frontend:** `UserService.auth` |
| `POST /v1/api/users/auth/google` | `POST /v1/api/auth` | Same path as password login. Body: `{ "provider": "GOOGLE", "email", "token" }`. Google ID token is verified with Firebase. **Frontend:** `UserService.googleAuth` |
| `POST /v1/api/users/auth/refresh` | `POST /v1/api/auth/refresh` | Same body (`RefreshTokenRequest`). Login and refresh both return `accessToken` (login no longer uses `token`). |

Login response is now `{ "accessToken", "refreshToken", "user" }`. Refresh remains `{ "accessToken", "refreshToken" }`. Persist `accessToken` from both.

Expired access JWTs return `401` with `code=TOKEN_EXPIRED` and **do not** abort `POST /v1/api/auth/refresh`. Clients should retry once after refresh; a reused refresh token returns `401` with `code=REFRESH_TOKEN_REUSED` and invalidates every session for that user.

`POST /v1/api/auth/login` and `POST /v1/api/auth/google` no longer exist.

`AuthProvider` is `PASSWORD` or `GOOGLE` (`INTERNAL` is not a value). `password` is required for `PASSWORD`; `token` is required for `GOOGLE`.

Rate limiting for login now applies to `/v1/api/auth` and `/v1/api/auth/refresh`.

---

## Users

| Old | New | Notes |
|---|---|---|
| `GET /v1/api/users` | `GET /v1/api/users` | Unchanged path. Empty list is now `200 []` instead of `204`. |
| `GET /v1/api/users/customers` | `GET /v1/api/users?type=CUSTOMER` | **ADMIN only** (same as listing users). Previously any authenticated caller could hit `/customers` via the fallback matcher. |
| `GET /v1/api/users/customers/emails` | `GET /v1/api/users?type=CUSTOMER&hasEmail=true` | ADMIN only. |
| `GET /v1/api/users/customers/{customerId}` | `GET /v1/api/users/{userId}` | Same `UserDto`. |
| `PUT /v1/api/users/{userId}` | `PATCH /v1/api/users/{userId}` | Partial update. **Frontend:** `UserService.updateUser`. |
| `PATCH /v1/api/users/reset-password/{userId}` | `PATCH /v1/api/users/{userId}/password` | Same body (`ResetPasswordRequest`). **Frontend:** `UserService.resetPassword`. |
| `DELETE /v1/api/users/{userId}` | same path | Status `200` → `204`. |

---

## Orders

| Old | New | Notes |
|---|---|---|
| `GET /v1/api/orders?page=&size=` | `GET /v1/api/orders?userId=&page=&size=` | Listing all orders is still admin-only in practice: customers **must** pass their own `userId` or the API returns `403`. |
| `GET /v1/api/orders/filter?userId=` | `GET /v1/api/orders?userId={id}` | **Frontend:** `RepairService.getUserRepairs`. Response is now the paginated `GetAllOrdersResponse` (`data`, `totalElements`, `totalPages`, `page`, `size`), not the old `GetOrdersByUserIdResponse`. |
| `PATCH /v1/api/orders/{id}` | `PATCH /v1/api/orders/{orderId}` | Path variable renamed only. **Frontend:** `RepairService.patchRepair` already uses `/orders/{id}`. |
| `GET /v1/api/orders/export/{orderId}` | `GET /v1/api/orders/{orderId}/export` | **Frontend:** `RepairService.exportOrder`. Filename header is now `report.pdf`. |
| `DELETE /v1/api/orders/{orderId}` | same path | Already `204`. |

`GET /v1/api/orders` is now allowed for `CUSTOMER` **and** `ADMINISTRATOR` (was admin-only). Customers can only request their own `userId`.

---

## Order items (nested under orders)

| Old | New | Notes |
|---|---|---|
| `POST /v1/api/order-items` | `POST /v1/api/orders/{orderId}/items` | `orderId` is in the path, not the body. |
| `GET /v1/api/order-items` | `GET /v1/api/orders/{orderId}/items` | Now scoped to one order. Empty list is `200 []`. |
| `GET /v1/api/order-items/{orderItemId}` | `GET /v1/api/orders/{orderId}/items/{orderItemId}` | `404` if the item does not belong to that order. |
| `PUT /v1/api/order-items` (id in body) | `PATCH /v1/api/orders/{orderId}/items/{orderItemId}` | `id` removed from `UpdateOrderItemRequest`. |
| `DELETE /v1/api/order-items/{orderItemId}` | `DELETE /v1/api/orders/{orderId}/items/{orderItemId}` | Status `200` → `204`. **Frontend:** `RepairService.deleteOrderItem`. |

---

## Demands (nested under orders)

| Old | New | Notes |
|---|---|---|
| `POST /v1/api/demands` | `POST /v1/api/orders/{orderId}/demands` | `orderId` removed from `CreateDemandRequest`. Body is now `{ componentPartId, quantity }`. |
| `GET /v1/api/demands/order/{orderId}` | `GET /v1/api/orders/{orderId}/demands` | Empty list is `200 []`. |
| `GET /v1/api/demands/order/{orderId}/component/{componentId}` | `GET /v1/api/orders/{orderId}/demands?componentId=` | Always returns a **list** (`200`, 0 or 1 item). No longer `200` single object / `404`. |
| `DELETE /v1/api/demands/{demandId}` | `DELETE /v1/api/orders/{orderId}/demands/{demandId}` | Status `200` → `204`. |
| `DELETE /v1/api/demands/order/{orderId}` | `DELETE /v1/api/orders/{orderId}/demands` | Bulk delete of the collection. Status `200` → `204`. |

---

## Components

| Old | New | Notes |
|---|---|---|
| `GET /v1/api/components` | same path | Empty list is `200 []`. |
| `GET /v1/api/components/category/{category}` | `GET /v1/api/components?category=` | |
| `GET /v1/api/components/name/{name}` | `GET /v1/api/components?name=` | Both filters can be combined. |
| `DELETE /v1/api/components/{componentId}` | same path | Status `200` → `204`. |

---

## Announcements

| Old | New | Notes |
|---|---|---|
| `GET /v1/api/announcements/last` | `GET /v1/api/announcements/latest` | Missing announcement is now `404` (was `204`). |
| `DELETE /v1/api/announcements/{announcementId}` | same path | Status `200` → `204`. |

---

## Error body

Old (often without `code`):

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "...",
  "path": "/v1/api/...",
  "timestamp": "2025-11-23T14:30:00"
}
```

New:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/v1/api/components",
  "timestamp": "2025-11-23T14:30:00",
  "code": "VALIDATION_ERROR",
  "errors": [
    { "field": "item", "message": "must not be blank" }
  ]
}
```

`errors` is omitted when there are no field-level validation failures. Unexpected exceptions now also use this envelope with `code=INTERNAL_SERVER_ERROR` instead of Spring Boot's default error JSON.

---

## Frontend checklist

Cross-cutting:

1. `apiClient` in `frontend/src/providers/api.js`: do **not** set `withCredentials`. Do **not** send `X-XSRF-TOKEN` or warm CSRF with `GET /auth`.
2. Keep sending `Authorization: Bearer …` as today. On `401`, refresh via `POST /auth/refresh` and retry with the new access token. Add nothing else unless you introduce custom headers (CORS will reject them).

Update these calls first:

1. `UserService.auth` → `POST /auth` with `{ provider: "PASSWORD", email, password }`. Persist `accessToken` (not `token`).
2. `UserService.googleAuth` → same `POST /auth` with `{ provider: "GOOGLE", email, token }`. Persist `accessToken` on `authToken`.
3. `UserService.updateUser` → `PATCH /users/{id}` (not `PUT`).
4. `UserService.resetPassword` → `PATCH /users/{userId}/password`.
5. `RepairService.getUserRepairs` → `GET /orders?userId={id}` and read `response.data.data` (paginated wrapper).
6. `RepairService.exportOrder` → `GET /orders/{id}/export`.
7. `RepairService.deleteOrderItem` → `DELETE /orders/{orderId}/items/{id}` (needs the parent order id).

Also stop treating empty `GET` collections as HTTP `204`.

---

## Order lifecycle (status model, budget approval link, photos)

### New status model

| Old | New |
|---|---|
| `PENDING`, `IN_ANALYSIS` | `RECEIVED` (Em avaliação) |
| – | `AWAITING_APPROVAL` (Aguardando aprovação) |
| – | `IN_REPAIR` (Em conserto) |
| `COMPLETED` | `READY_FOR_PICKUP` (Pronto para retirada) + `outcome: REPAIRED` |
| `DELIVERED` | `DELIVERED` + `outcome: REPAIRED` |

`outcome` (`REPAIRED` / `NOT_REPAIRED`) is set when the order reaches `READY_FOR_PICKUP` and kept on `DELIVERED`. Existing data is migrated by `V7__order_lifecycle.sql` (prod) or the `LegacyOrderStatusBackfill` runner (dev, no Flyway). Deploy as stop → migrate → start: old instances can't read the new values.

Valid transitions (anything else is `409 ORDER_INVALID_TRANSITION`):

- `RECEIVED → AWAITING_APPROVAL | READY_FOR_PICKUP (NOT_REPAIRED)`
- `AWAITING_APPROVAL → IN_REPAIR (approved) | READY_FOR_PICKUP (declined, NOT_REPAIRED) | RECEIVED (redo budget)`
- `IN_REPAIR → READY_FOR_PICKUP (REPAIRED by default, NOT_REPAIRED allowed)`
- `READY_FOR_PICKUP → DELIVERED`

### Changed endpoints

| Endpoint | Old | New |
|---|---|---|
| `PATCH /v1/api/orders/{id}` | accepted `status`; `200` empty body | `status` → `400 VALIDATION_ERROR` (use `/transitions`); returns `200` with the order; total recalculated |
| `GET /v1/api/orders/{id}`, `/{id}/export` | any authenticated user could read any order | a CUSTOMER only sees their own orders; others → `404 ORDER_NOT_FOUND` |
| Order `deadline` | always `null` (value was stored as `completedTime`) | real `yyyy-MM-dd` |
| Appliances `volt` / `series` | mixed with `voltage` / `serial` | `voltage` / `serial` in create, update and output; `volt` / `series` still accepted on input for one version |
| `parts[]` in order responses | no quantity on the order | adds `demandQuantity` (`quantity` stays as the component stock) |
| Dashboard `status` filter | `ONGOING`, `COMPLETED`, `DELIVERED`, `ALL` | `ONGOING` (RECEIVED+AWAITING_APPROVAL+IN_REPAIR), `READY`, `DELIVERED`, `ALL`; `COMPLETED` is an alias of `READY` for one version |
| Dashboard revenue/profit/avg price/top components | counted every order | exclude `outcome = NOT_REPAIRED` (no fee) |
| Dashboard average repair time | `deadline − receivedAt` | `completedAt − receivedAt`, `REPAIRED` only |

Order responses add `outcome`, `trackingUrl`, `budgetSentAt`, `budgetTotal`, `approvalDecision`, `decidedAt`, `decidedVia`, `completedAt`, `deliveredAt`, and (single-order responses only; `null` in lists) `statusHistory[]` and `photos[]`.

### New endpoints (ADMINISTRATOR)

- `POST /v1/api/orders/{id}/transitions` `{to, outcome?, note?}`
- `POST /v1/api/orders/{id}/budget/send` → `{trackingUrl, whatsappText, order}`; `400 ORDER_BUDGET_EMPTY`
- `POST /v1/api/orders/{id}/budget/decision` `{decision: APPROVED|DECLINED, note?}` (idempotent)
- `POST /v1/api/orders/{id}/tracking-link` → `{trackingUrl, whatsappText}`
- `GET /v1/api/orders/lookup?token=` → `{id}`
- `POST /v1/api/orders/label-reading` (multipart `file`) → `{isLabel, type, brand, model, serial, voltage, confidence}`; `400 LABEL_READING_INVALID`, `503 LABEL_READING_UNAVAILABLE`
- `POST /v1/api/orders/{id}/photos` (multipart `file` + `kind`), `GET /v1/api/orders/{id}/photos`, `DELETE /v1/api/orders/{id}/photos/{photoId}`; `409 ORDER_PHOTO_LIMIT` (6)

Concurrent edits of the same order: the loser gets `409 ORDER_CONCURRENT_UPDATE`.

### Public customer page (no login, 30 requests/minute per IP)

- `GET /p/{token}` HTML page, `POST /p/{token}/decision` form (303 back to the page), `GET /v1/public/orders/{token}` JSON.
- Unknown link, or 60 days after delivery: friendly 404 page / `404 TRACKING_NOT_FOUND`.
- Shows only the customer's first name, device, values and status.

### Events and config

- RabbitMQ `notification.events`: `order.status.changed`, `order.budget.decided` (after commit).
- Config: `undercontroll.shop.*` (`SHOP_NAME`, `SHOP_PHONE`, `SHOP_WHATSAPP`, `SHOP_ADDRESS`, `SHOP_HOURS`, `SHOP_WARRANTY_DAYS`), `undercontroll.public.base-url` (`PUBLIC_BASE_URL`), `undercontroll.ai.label-reading.*` (`LABEL_READING_PROVIDER`, falls back to `INSIGHTS_PROVIDER`), `rate-limit.public-requests-per-minute`.
- Redis cache prefix bumped to `v5:`.

---

## File uploads: direct to storage through presigned URLs

Files no longer go through the API as multipart. Every upload is three steps:

1. `POST …/uploads` with `{contentType, sizeBytes}` → `201 {uploadId, uploadUrl, method: "PUT", headers, expiresAt}` (valid for 5 minutes).
2. `PUT uploadUrl` with the raw file bytes and **exactly** the returned `headers` (`Content-Type`, `Content-Length`, and `x-amz-server-side-encryption` in production). No `Authorization` header: the signature in the URL is the authorization. A different type or size fails with `403 SignatureDoesNotMatch` from the storage.
3. Call the feature endpoint with the `uploadId`. The backend checks the stored size and the file signature (magic bytes); a mismatch deletes the object and returns `400 UPLOAD_INVALID` (or the feature's own code).

| Feature | Old | New |
|---|---|---|
| Order photos | `POST /v1/api/orders/{id}/photos` multipart `file` + `kind` | `POST /v1/api/orders/{id}/photos/uploads` `{kind, contentType, sizeBytes}` → PUT → `POST /v1/api/orders/{id}/photos/uploads/{uploadId}/complete` → `201 OrderPhotoDto` (idempotent). The limit of 6 counts pending uploads. |
| Label reading | `POST /v1/api/orders/label-reading` multipart `file` | `POST /v1/api/orders/label-reading/uploads` `{contentType, sizeBytes}` → PUT → `POST /v1/api/orders/label-reading` `{uploadId}` |
| Stock import | `POST /v1/api/stock-imports` multipart `file` | `POST /v1/api/stock-imports/uploads` `{contentType, sizeBytes, fileName}` → PUT → `POST /v1/api/stock-imports` `{uploadId}` → `202` (idempotent per `uploadId`) |
| Announcement image | presigned PUT without signed type or size | Same endpoints. The PUT now signs the `contentType`/`sizeBytes` the request already sends (JPEG/PNG/WEBP, ≤ 5MB, else `400 UPLOAD_INVALID`); the response adds `method` and `headers`. |

Other changes:

- Read URLs are presigned per area: order photos 15 minutes, announcement images 60 minutes, PDF exports 5 minutes. The same URL is returned while more than half of its validity remains, so client image caches work. Responses carry `Cache-Control: private, max-age=…, immutable`.
- A multipart (or any unsupported `Content-Type`) request now gets `415` with `code: VALIDATION_ERROR` instead of `500`.
- Storage failures return `503 STORAGE_UNAVAILABLE`. Before, failed writes were silently ignored.
- Global multipart limit back to 8MB/9MB (only transcription still uses multipart).
- Config: `undercontroll.storage.*` (`STORAGE_ENDPOINT`, `STORAGE_PUBLIC_ENDPOINT`, `STORAGE_ACCESS_KEY`, `STORAGE_SECRET_KEY`, `STORAGE_REGION`, `STORAGE_PATH_STYLE`, `STORAGE_SSE`, `S3_UPLOAD_BUCKET`, `S3_EXPORT_BUCKET`). The old `aws.*` keys are gone; `ACCESS_KEY_ID` / `SECRET_ACCESS_KEY` / `SESSION_TOKEN` are still read as fallbacks. With no keys, the AWS default credential chain (IAM role) is used.
- Local development uses MinIO from `docker compose` (API `:9000`, console `:9001`, `minioadmin`/`minioadmin`).
- Migration `V8__storage_uploads.sql`: `storage_upload` table, `stock_import.storage_key`, `stock_import.file_purged_at`. Imports created before V8 keep reading their file from `stock_import_file` until it is purged.
