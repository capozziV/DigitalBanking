create table transfer (
    id                     uuid        primary key,
    source_account_id      uuid        not null,
    destination_account_id uuid        not null,
    amount                 bigint      not null check (amount > 0),
    description            varchar(140),
    idempotency_key        text        not null unique,
    status                 text        not null check (status in ('PENDING', 'COMPLETED', 'FAILED', 'COMPENSATED')),
    step                   text        not null,
    failure_reason         text,
    deadline_at            timestamptz not null,
    next_attempt_at        timestamptz,
    attempts               int         not null default 0,
    locked_until           timestamptz,
    lock_owner             text,
    correlation_id         text,
    created_at             timestamptz not null,
    updated_at             timestamptz not null,
    check (source_account_id <> destination_account_id)
);

-- The scheduler only looks at pending transfers; a null next_attempt_at (terminal) is never due.
create index transfer_due_idx on transfer (next_attempt_at) where status = 'PENDING';
