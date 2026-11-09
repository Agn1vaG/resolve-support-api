# API Endpoints

All endpoints use JSON and the `/api/v1` prefix. Registration, login, and `/actuator/health` are public. Other routes require `Authorization: Bearer <accessToken>`.

## Role permissions

| Operation | USER | AGENT | ADMIN |
|---|---|---|---|
| Create ticket | Yes | No | Yes |
| Read tickets | Own | Assigned to self | All |
| Change ticket details or priority | No | No | Any |
| Change status | No | Assigned to self | Any |
| Assign or unassign | No | No | Any; assignee must be AGENT |
| Comment / read history | Own tickets | Assigned tickets | Any |
| Administer users and roles | No | No | Yes |

Actor identity comes from the authenticated account. Request bodies cannot set the ticket creator, comment author, or history actor.

## Authentication

### Register — `POST /api/v1/auth/register`

Request: `{"name":"Ari Example","email":"ari@example.com","password":"SecurePass123"}`

Returns a safe user response with `201 Created`; public registrations always receive `USER`. Duplicate email returns `409`, invalid input `400`.

### Login — `POST /api/v1/auth/login`

Request: `{"email":"ari@example.com","password":"SecurePass123"}`. Success returns `200 OK` with `accessToken`, `tokenType`, `expiresIn`, and safe user information. Invalid credentials return a generic `401`.

## Users

| Method and endpoint | Permission | Request / result |
|---|---|---|
| `POST /api/v1/users` | ADMIN | Create account with `name`, `email`, `password`, optional role. `201`, safe `UserResponse`. |
| `GET /api/v1/users/{id}` | Own user or ADMIN | Safe `UserResponse`; `200`. |
| `GET /api/v1/users?page=0&size=20&sort=createdAt,desc` | ADMIN | Paginated safe user responses; `200`. |
| `GET /api/v1/admin/users?page=0&size=20` | ADMIN | Paginated user management list; `200`. |
| `GET /api/v1/admin/users/{id}` | ADMIN | Safe user response; `200`. |
| `PATCH /api/v1/admin/users/{id}/role` | ADMIN | `{"role":"AGENT"}` or `{"role":"USER"}`; returns safe user response, `200`. ADMIN cannot be assigned here. |

## Tickets

| Method and endpoint | Permission | Request / result |
|---|---|---|
| `POST /api/v1/tickets` | USER or ADMIN | `title` (max 200), `description` (max 10000), optional `priority`, optional admin-only `assigneeId`; status starts `OPEN`. `201` with `TicketResponse`. |
| `GET /api/v1/tickets/{id}` | Creator, assigned agent, or ADMIN | `TicketResponse`; `200`. |
| `GET /api/v1/tickets` | Authenticated; role scoped | Paginated ticket summaries; `200`. |
| `PUT /api/v1/tickets/{id}` | ADMIN | Partial `title` (max 200), `description` (max 10000), and/or `priority`; omitted values are preserved. `200`. |
| `PATCH /api/v1/tickets/{id}/assign` | ADMIN | Assign: `{"agentId":42}`; unassign: `{"agentId":null}`. Returns updated ticket, `200`. |
| `PATCH /api/v1/tickets/{id}/status` | ADMIN or assigned AGENT | `{"status":"IN_PROGRESS"}`; returns updated ticket, `200`. |
| `GET /api/v1/tickets/created-by/{userId}` | Own user or ADMIN | Paginated ticket summaries; `200`. |
| `GET /api/v1/tickets/assigned-to/{agentId}` | Own agent queue or ADMIN | Paginated ticket summaries; `200`. |

Ticket list query parameters include `page`, `size`, repeatable `sort=createdAt,desc`, `status`, `priority`, `assigneeId`, `creatorId`, `createdFrom`, and `createdTo` (ISO-8601 instants). Filters are applied in the database. USER and AGENT results remain scoped to their own tickets regardless of query parameters. Defaults are page 0, size 20, sort by `createdAt` ascending; requested page sizes are capped at 100.

Status transitions: `OPEN -> IN_PROGRESS`; `IN_PROGRESS -> RESOLVED` or `OPEN`; `RESOLVED -> CLOSED` or `IN_PROGRESS`. `CLOSED` is terminal. Repeating the current status succeeds as a no-op without a duplicate history entry. Invalid transitions return `400`; inaccessible operations return `403`; missing resources return `404`.

Assignment rejects missing accounts (`404`) and non-AGENT targets (`400`). Reassignment and explicit unassignment are allowed. Each actual change updates `updatedAt` and creates an audit event.

## Comments and history

| Method and endpoint | Permission | Request / result |
|---|---|---|
| `POST /api/v1/tickets/{ticketId}/comments` | Ticket creator, assigned agent, or ADMIN | `{"message":"I restarted the device."}` (nonblank, max 5000 chars); author comes from token. `201` with author and creation time. |
| `GET /api/v1/tickets/{ticketId}/comments` | Ticket creator, assigned agent, or ADMIN | Chronological comments; `200`. |
| `GET /api/v1/tickets/{ticketId}/history` | Ticket creator, assigned agent, or ADMIN | Append-only history events; `200`. |

The history includes `TICKET_CREATED` (`oldStatus: null`, `newStatus: OPEN`), `STATUS_CHANGED` with actor and before/after status, and `ASSIGNED`, `REASSIGNED`, or `UNASSIGNED` with prior/new agent values. Status and assignment writes plus their history row are transactional. There are no public history write/edit endpoints.

## Errors

Errors contain `timestamp`, `status`, `error`, `message`, `path`, and optional `validationErrors`. API responses do not include stack traces, database internals, secrets, or token contents. Expected statuses include `400`, `401`, `403`, `404`, and `409`.
