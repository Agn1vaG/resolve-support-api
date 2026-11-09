# Entity Relationships

## Entities and responsibilities

- **`User`** stores account identity and role. Its Java properties are `name`, `email`, and `password`; `name` maps to `display_name`, and `password` maps to `password_hash`. The password property is ignored by Jackson serialization. `UserService` stores only BCrypt hashes; authentication is handled by the security layer.
- **`Ticket`** stores the support request, including title, description, status, priority, creator, and optional assignee. `title` maps to the database column `subject`. Its `version` field uses JPA optimistic locking to help detect concurrent updates.
- **`Comment`** stores one public conversation message tied to a ticket and author. Its Java property `message` maps to `body`.
- **`TicketHistory`** records ticket creation, status transitions, and assignment changes, including the optional actor and before/after values. A null actor is allowed for system-generated events.

The three enums `UserRole`, `TicketStatus`, and `TicketPriority` use `@Enumerated(EnumType.STRING)` so PostgreSQL stores readable labels rather than ordinal numbers. The current status set is `OPEN`, `IN_PROGRESS`, `RESOLVED`, and `CLOSED`.

## Relationships and ownership

All relationships are intentionally unidirectional from the dependent entity. This avoids keeping two in-memory references synchronized and prevents recursive entity serialization. Parent collections described in the domain model can be queried through repositories when needed; no `@OneToMany` collections are required yet.

| Relationship | JPA mapping and owning side | Foreign key | Nullability |
|---|---|---|---|
| User creates tickets | `Ticket.creator` is `@ManyToOne(fetch = LAZY, optional = false)` | `tickets.created_by_id` | Required |
| User is assigned tickets | `Ticket.assignee` is `@ManyToOne(fetch = LAZY)` | `tickets.assigned_agent_id` | Optional |
| Ticket has comments | `Comment.ticket` is `@ManyToOne(fetch = LAZY, optional = false)` | `comments.ticket_id` | Required |
| User writes comments | `Comment.author` is `@ManyToOne(fetch = LAZY, optional = false)` | `comments.author_id` | Required |
| Ticket has status history | `TicketHistory.ticket` is `@ManyToOne(fetch = LAZY, optional = false)` | `ticket_history.ticket_id` | Required |
| User acts in history | `TicketHistory.actor` is `@ManyToOne(fetch = LAZY)` | `ticket_history.actor_id` | Optional for system events |
| Prior/new assignee in history | `TicketHistory.oldAssignee` / `newAssignee` are `@ManyToOne(fetch = LAZY)` | `ticket_history.old_assignee_id` / `new_assignee_id` | Optional for creation/status events or unassignment |

Each `@ManyToOne` side owns its foreign key through `@JoinColumn`. Since no inverse collection is mapped, there is no `mappedBy` side. Each entity has a protected no-argument constructor for JPA and uses field access because `@Id` is on a field.

## Foreign keys and deletion

The V2 and V3 migrations create restrictive foreign keys (`ON DELETE RESTRICT`) for creator, assignee, comment author/ticket, and history actor/assignees/ticket. No JPA cascade is configured. Deleting a referenced user or ticket cannot silently delete related conversation or audit data. Account deactivation and ticket soft deletion remain future workflow decisions; the current schema has no `active` or `deleted_at` property yet.

## Fetch behavior and serialization

To-one relationships default to eager loading in JPA, so each relationship explicitly uses `FetchType.LAZY`. Collections are not mapped, which avoids accidental eager collection loading. Callers should load the needed association within a transaction and later map it to a DTO rather than returning entities from HTTP endpoints.

The relationships are unidirectional, so serializing a ticket does not loop through creator-to-tickets-to-creator. The `User.password` field and getter are annotated `@JsonIgnore` as an additional safeguard. DTOs remain the intended API boundary.

## Java and relational mapping

`@Entity` marks a class as persisted and `@Table` selects its table. `@Id` and `@GeneratedValue(IDENTITY)` map generated PostgreSQL identity primary keys. `@Column` aligns Java fields with SQL names, nullability, and lengths. `@Enumerated(STRING)` stores enum names. `@ManyToOne` plus `@JoinColumn` maps associations to foreign-key columns. Lifecycle callbacks set timestamps on insert/update, and `@Version` enables Hibernate's optimistic-lock check for ticket updates.

Flyway V2 creates the tables, constraints, and indexes; Hibernate is configured to validate the schema, not modify it. There is no `CascadeType.ALL`: persistence and deletion of associated objects remain explicit.
