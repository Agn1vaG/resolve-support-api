# Requirements

## Purpose

Resolve is a REST API for handling support requests from creation through resolution. It gives customers one place to report an issue and follow its status, and gives support staff a shared queue for assigning, responding to, and resolving those issues. A recorded history makes changes traceable.

The first release is a single Spring Boot application backed by PostgreSQL. It is intended to support a conventional web or mobile client; user interface work is outside this API's scope.

## Functional requirements

### Accounts and access

- A person can register an account and later authenticate. Passwords must be stored as one-way hashes, never as plaintext.
- Accounts have one of three roles: `USER`, `AGENT`, or `ADMIN`.
- A user can read and manage their own profile. Administrative account and role management is restricted to admins.
- Authentication is token-based, with role and ownership checks applied to protected operations. The concrete JWT design is deferred to the security phase.

### Tickets

- An authenticated user can create a ticket with a subject, description, and priority.
- A user can view and track their own tickets; an agent can view tickets assigned to them; an admin can view all tickets.
- Authorized actors can update ticket fields. Only admins can assign tickets to eligible agents.
- Tickets have a status and priority. Initial values are proposed in `database-design.md`.
- An admin can remove a ticket from active use. The recommended implementation is soft deletion so history and references remain intact.
- Ticket lists support pagination, sorting, and filters such as status, priority, owner, assignee, and creation date.

### Comments and history

- Authorized participants can add comments to a ticket.
- Ticket changes that affect workflow or ownership are recorded with actor, time, changed field, and before/after values.
- Users can read their ticket's comments and history; agents can read those for tickets assigned to them; admins can read all.

## Non-functional requirements

- **Security:** enforce least privilege, validate authorization on every resource access, hash passwords, validate inputs, and avoid exposing secrets or sensitive internals in errors.
- **Data integrity:** use relational constraints and transactions so ticket ownership, assignment, comments, and history do not contradict one another.
- **Maintainability:** use a layered monolith organized by domain feature; keep business rules in services and persistence concerns in repositories.
- **Usability for API clients:** expose predictable JSON resources, standard HTTP semantics, validation feedback, and documented endpoints.
- **Performance:** paginate collection endpoints and index common ownership, status, priority, assignment, and chronological lookups.
- **Reliability:** persist data in PostgreSQL and use Flyway migrations as the versioned source of schema changes.
- **Testability:** cover domain rules in unit tests and important HTTP/database/security flows in integration tests.
- **Operability:** support configuration through environment-specific settings and provide containerized local startup in a later phase.

## Out of scope for the initial design

Email notifications, file attachments, live chat, service-level agreement timers, multiple organizations/tenants, external identity providers, and analytics dashboards are not included. They can be considered after the core workflow is agreed.
