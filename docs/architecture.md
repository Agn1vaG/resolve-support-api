# Architecture

## Design overview

Use a layered monolith: one deployable Spring Boot service with one PostgreSQL database. This keeps deployment and transactions straightforward while the domain is small, and leaves clear seams between HTTP, business rules, and persistence. Separate services, brokers, and caches would add operational cost without a requirement that justifies them.

Organize packages by feature first, then by layer. This keeps ticket-related code together while retaining familiar controller/service/repository responsibilities. Shared concerns such as security, configuration, and error handling live in focused common packages.

## Layers and responsibilities

- **Controller:** maps HTTP routes and status codes, binds request/response DTOs, and invokes a service. It does not implement workflow rules or expose JPA entities.
- **DTO:** defines the API contract. Request validation belongs here; response DTOs prevent persistence details from leaking to clients.
- **Mapper:** translates between DTOs and domain/persistence objects. Keep mapping explicit and simple until duplication warrants a library.
- **Service:** coordinates transactions and enforces business rules such as who may change a ticket and which status transitions are allowed.
- **Repository:** Spring Data JPA access to persisted entities. It should not decide authorization or workflow policy.
- **Entity:** JPA persistence model and relationships. Avoid putting HTTP concerns in entities.
- **Security/config:** authenticates stateless bearer JWTs, configures BCrypt, and defines URL and method-level role checks.
- **OpenAPI documentation:** Springdoc derives the specification from controller contracts, DTO schemas, security annotations, and operation descriptions; Swagger UI provides an interactive client for local exploration.
- **Exception handling:** a centralized advice maps known failures to consistent HTTP responses without leaking stack traces.

### Request lifecycle

1. Spring Security permits authentication/health routes or validates the bearer token and establishes the database-backed actor and role.
2. The controller validates and binds the incoming request into a DTO.
3. The service applies authorization and business rules, then uses repositories within a transaction.
4. Repositories load or persist entities; database constraints provide a final integrity guard.
5. A mapper creates a response DTO, and the controller returns the appropriate HTTP status and JSON.
6. Central exception handling converts validation, missing-resource, and rule failures to a stable error shape.

This sequence makes responsibilities testable and prevents controllers from becoming the only place where rules are enforced.

## Proposed package structure

```text
  com.resolve.support
  account/
    controller/
    service/
    repository/
    entity/
    dto/
    mapper/
  ticket/
    controller/
    service/
    repository/
    entity/
    dto/
    mapper/
  comment/
    controller/
    service/
    repository/
    entity/
    dto/
    mapper/
  history/
    controller/
    service/
    entity/
    repository/
    dto/
    mapper/
  config/
  security/
    dto/
  exception/
```

`com.resolve.support` is the application root package, matching the Maven group and project identity. Features are implemented in the same package-by-feature structure. Shared exceptions live in `exception/`; the security package remains a future phase concern. Avoid empty packages and interfaces that have only one implementation without a meaningful seam.

## Key design decisions

- **DTOs at the API boundary:** clients should not be coupled to JPA mappings or accidentally receive password hashes and internal fields.
- **Service-owned rules:** the same workflow and access rules apply regardless of which controller invokes them.
- **PostgreSQL constraints plus validation:** request validation gives useful feedback; database constraints protect against invalid persisted references and races.
- **Append-only history:** record changes rather than overwriting audit evidence. Ticket deletion should therefore be soft deletion.
- **No premature generic framework:** use ordinary Spring services and repositories; introduce abstractions only when a real second implementation or test seam appears.
- **DTO mapping inside a transaction-safe boundary:** controllers exchange DTOs only. Repository entity graphs load the to-one data needed by mappers, because Open EntityManager in View is disabled.

## API boundary and security

Controllers expose the documented `/api/v1` routes and delegate to services. Jakarta Bean Validation checks request shapes; the global advice returns one error format. Pagination and filtering are pushed into repository queries. Springdoc publishes the OpenAPI document at `/v3/api-docs` and Swagger UI at `/swagger-ui.html`; only those documentation resources are public for unauthenticated exploration.

Authentication uses a signed JWT with a configurable secret and expiry. The filter verifies the signature and expiry, then reloads the current user and authorities from PostgreSQL. Roles are therefore not trusted solely from claims. BCrypt hashes passwords before storage. Older Phase 4 plaintext values do not match BCrypt and must be reset; they are never accepted as valid passwords.

Public registration always creates a `USER`; user administration is restricted to `ADMIN`. Ticket creator, comment author, and history actor are derived from the authenticated user. The service layer checks ownership and assignment: users see tickets they created, agents see tickets assigned to them, and admins can access all. Agents can update status only on assigned tickets; only admins assign agents.

Authentication and authorization are not applied to public registration/login and health routes. Other routes require a valid bearer JWT. Security failures are serialized by Spring Security's entry point and access-denied handler; application validation and resource errors use the common REST exception advice.

`TicketService` enforces the agreed status transition matrix and applies ownership/assignment checks before mutations. Status changes and assignment changes are written to append-only history in the same transaction. Ticket list predicates are composed as JPA specifications so role scope and optional filters are applied by PostgreSQL before pagination.

## Important business rules and edge cases

- A `USER` may only access their own tickets. An `AGENT` may only work with tickets assigned to them unless an admin explicitly grants wider access.
- Only active accounts with the `AGENT` role may receive assignments. Removing or demoting an assigned agent requires a policy: reassign first or clear the assignment.
- A ticket creator is fixed after creation. Changing ownership should be a deliberate admin operation and should generate history.
- The allowed transition matrix is `OPEN -> IN_PROGRESS`, `IN_PROGRESS -> RESOLVED|OPEN`, and `RESOLVED -> CLOSED|IN_PROGRESS`; `CLOSED` is terminal.
- A `CLOSED` or soft-deleted ticket should not accept ordinary comments or edits. Reopening and administrative correction need explicit rules.
- Concurrent updates can overwrite each other. Consider optimistic versioning (`@Version`) on tickets when implementation begins.
- Deleting an account with tickets/comments/history can break attribution. Prefer deactivation and retain the account record; establish retention rules before any physical deletion.
- Reject invalid pagination sizes and sort fields instead of passing arbitrary client input to database queries.
- Use UTC instants for persisted timestamps and return timestamps with timezone information.
- Decide whether comments can be edited/deleted, whether internal agent notes exist, and how abuse or oversized content is handled before those endpoints are built.
