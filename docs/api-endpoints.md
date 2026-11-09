# API Endpoints and Interactive Documentation

The OpenAPI document is available at `http://localhost:8080/v3/api-docs`; Swagger UI is at `http://localhost:8080/swagger-ui.html`. Swagger UI uses the same OpenAPI schemas and examples documented below. The docs resources are public, while application endpoints remain protected by the existing Spring Security rules.

## Use JWT authorization in Swagger UI

1. Call `POST /api/v1/auth/register` to create a USER account.
2. Call `POST /api/v1/auth/login` with that account's credentials.
3. Copy the `accessToken` value from the response.
4. Select **Authorize** in Swagger UI and paste the token value without adding `Bearer `.
5. Confirm authorization and call protected routes. Swagger UI adds the `Authorization: Bearer <token>` header.

Tokens expire according to `JWT_EXPIRATION_MS` (15 minutes by default). Registration and login do not require a token. Logging in as AGENT or ADMIN requires an account provisioned by an administrator; there are no demo credentials or seeded accounts.

## Roles and enums

| Enum | Values |
|---|---|
| `UserRole` | `USER`, `AGENT`, `ADMIN` |
| `TicketStatus` | `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED` |
| `TicketPriority` | `LOW`, `MEDIUM`, `HIGH`, `URGENT` |
| `TicketHistoryEventType` | `TICKET_CREATED`, `STATUS_CHANGED`, `ASSIGNED`, `UNASSIGNED`, `REASSIGNED` |

| Operation | USER | AGENT | ADMIN |
|---|---|---|---|
| Create tickets | Yes | No | Yes |
| Read tickets | Own | Assigned to self | All |
| Update details or priority | No | No | Any |
| Change status | No | Assigned to self | Any |
| Assign / reassign / unassign | No | No | Any; target must be an AGENT |
| Add comments / read comments and history | Own tickets | Assigned tickets | Any |
| Administer accounts and roles | No | No | Yes |

Identity for ticket creation, comment authorship, and history events comes from the authenticated account, never from a request body.

## Authentication

### `POST /api/v1/auth/register`

Publicly registers a USER account. Password is hashed and never returned.

Request:

```json
{"name":"Ari Example","email":"ari@example.com","password":"SecurePass123"}
```

Returns `201 Created` with `UserResponse`. Invalid input returns `400`; duplicate email returns `409`.

### `POST /api/v1/auth/login`

Publicly authenticates credentials and returns a JWT.

Request:

```json
{"email":"ari@example.com","password":"SecurePass123"}
```

Response (`200 OK`):

```json
{
  "accessToken":"<signed-jwt>",
  "tokenType":"Bearer",
  "expiresIn":900,
  "user":{"id":1,"name":"Ari Example","email":"ari@example.com","role":"USER","createdAt":"2026-11-09T10:00:00Z","updatedAt":"2026-11-09T10:00:00Z"}
}
```

Invalid credentials return a generic `401 Unauthorized` response.

## Tickets

| Method and path | Access | Request / result |
|---|---|---|
| `POST /api/v1/tickets` | USER, ADMIN | `CreateTicketRequest`; creator from JWT. Returns `TicketResponse`, `201`. |
| `GET /api/v1/tickets/{id}` | Creator, assigned agent, ADMIN | Returns `TicketResponse`, `200`. |
| `GET /api/v1/tickets` | Authenticated, role-scoped | `page`, `size` (max 100), `sort`, optional `status`, `priority`, `assigneeId`, `creatorId`, `createdFrom`, `createdTo`. Returns a page of summaries, `200`. |
| `PUT /api/v1/tickets/{id}` | ADMIN | Partial `title`, `description`, and/or `priority`; omitted fields stay unchanged. Returns `TicketResponse`, `200`. |
| `PATCH /api/v1/tickets/{id}/assign` | ADMIN | `{"agentId":42}` to assign or `{"agentId":null}` to unassign. Returns `TicketResponse`, `200`. |
| `PATCH /api/v1/tickets/{id}/status` | ADMIN, assigned AGENT | `{"status":"IN_PROGRESS"}`. Returns `TicketResponse`, `200`. |
| `GET /api/v1/tickets/created-by/{userId}` | Own USER id, ADMIN | Paged ticket summaries, `200`; page size max 100. |
| `GET /api/v1/tickets/assigned-to/{agentId}` | Own AGENT id, ADMIN | Paged ticket summaries, `200`; page size max 100. |

Ticket request example:

```json
{"title":"Cannot sign in","description":"Login fails after password reset","priority":"HIGH"}
```

Ticket response example:

```json
{
  "id":42,"title":"Cannot sign in","description":"Login fails after password reset",
  "status":"OPEN","priority":"HIGH",
  "creator":{"id":1,"name":"Ari Example","role":"USER"},
  "assignee":null,"createdAt":"2026-11-09T10:00:00Z","updatedAt":"2026-11-09T10:00:00Z"
}
```

The status transition rules are `OPEN -> IN_PROGRESS`, `IN_PROGRESS -> RESOLVED|OPEN`, and `RESOLVED -> CLOSED|IN_PROGRESS`; `CLOSED` is terminal. Repeating the current status is a no-op. Invalid transitions return `400`. Invalid enum and date values also return `400`. USER and AGENT list queries remain scoped to their own tickets even when filters are supplied.

## Comments

| Method and path | Access | Request / result |
|---|---|---|
| `POST /api/v1/tickets/{ticketId}/comments` | Ticket creator, assigned agent, ADMIN | `{"message":"I restarted the device and can sign in now."}`; nonblank, max 5000 chars. Returns `CommentResponse`, `201`. |
| `GET /api/v1/tickets/{ticketId}/comments` | Ticket creator, assigned agent, ADMIN | Chronological comment list, `200`. |

The author is always the authenticated user. A response includes `id`, `message`, `ticketId`, `author`, `createdAt`, and `updatedAt`.

## Ticket History

| Method and path | Access | Request / result |
|---|---|---|
| `GET /api/v1/tickets/{ticketId}/history` | Ticket creator, assigned agent, ADMIN | Chronological read-only audit entries, `200`. |

History records ticket creation, status transitions, and assignment changes, including actor, before/after status or agent, and timestamp. There are no public write or edit endpoints for history.

## Administration

| Method and path | Access | Request / result |
|---|---|---|
| `POST /api/v1/users` | ADMIN | Create an account using `CreateUserRequest`; returns safe `UserResponse`, `201`. |
| `GET /api/v1/users/{id}` | Own profile, ADMIN | Safe `UserResponse`, `200`. |
| `GET /api/v1/users` | ADMIN | Paged safe user list, `200`; page size max 100. |
| `GET /api/v1/admin/users` | ADMIN | Paged safe user list, `200`; page size max 100. |
| `GET /api/v1/admin/users/{id}` | ADMIN | Safe `UserResponse`, `200`. |
| `PATCH /api/v1/admin/users/{id}/role` | ADMIN | `{"role":"AGENT"}` or `{"role":"USER"}`; returns safe `UserResponse`, `200`. Cannot assign ADMIN. |

No user response includes a password or password hash. Role update rejects ADMIN; a sole administrator cannot demote their own account.

## Error response

All errors use the existing `ApiErrorResponse` format:

```json
{
  "timestamp":"2026-11-09T10:00:00Z",
  "status":400,
  "error":"Bad Request",
  "message":"Request validation failed.",
  "path":"/api/v1/tickets",
  "validationErrors":{"title":"must not be blank"}
}
```

| Status | Meaning |
|---|---|
| `400 Bad Request` | Invalid body, parameter, enum, or business rule |
| `401 Unauthorized` | Missing/invalid/expired JWT or invalid login credentials |
| `403 Forbidden` | Authenticated caller lacks role or resource access |
| `404 Not Found` | Requested resource does not exist |
| `409 Conflict` | Duplicate email or conflicting database constraint |
| `500 Internal Server Error` | Unexpected failure; details are kept out of the response |

Errors do not expose stack traces, database messages, password hashes, secrets, or JWT values.
