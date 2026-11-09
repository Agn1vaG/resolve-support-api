ALTER TABLE ticket_history
    ADD COLUMN event_type VARCHAR(30) NOT NULL DEFAULT 'STATUS_CHANGED',
    ADD COLUMN old_assignee_id BIGINT,
    ADD COLUMN new_assignee_id BIGINT;

ALTER TABLE ticket_history
    ALTER COLUMN new_status DROP NOT NULL;

ALTER TABLE ticket_history
    ADD CONSTRAINT fk_ticket_history_old_assignee
        FOREIGN KEY (old_assignee_id) REFERENCES users (id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_ticket_history_new_assignee
        FOREIGN KEY (new_assignee_id) REFERENCES users (id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_ticket_history_event_type CHECK (
        event_type IN ('TICKET_CREATED', 'STATUS_CHANGED', 'ASSIGNED', 'UNASSIGNED', 'REASSIGNED')
    );

CREATE INDEX idx_ticket_history_event_type ON ticket_history (event_type);
