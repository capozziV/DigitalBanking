-- Old rows: the eight step names above are exactly the names the domain persists, so the CHECK rejects no existing step.
-- A pending row without a next attempt is made due right away, so it satisfies the second CHECK instead of failing startup.
update transfer
set next_attempt_at = updated_at
where status = 'PENDING'
  and next_attempt_at is null;

-- The step column only holds the eight steps of the saga; an unknown value is a bug, not data.
alter table transfer
    add constraint transfer_step_known check (
        step in ('DEBIT', 'RESOLVE_DEBIT', 'CREDIT', 'RESOLVE_CREDIT', 'COMPENSATE', 'COMPLETED', 'FAILED', 'COMPENSATED')
    );

-- A pending transfer without a next attempt would never be picked up by the scheduler.
alter table transfer
    add constraint transfer_pending_has_next_attempt check (status <> 'PENDING' or next_attempt_at is not null);

