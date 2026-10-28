-- Audit trail of every call the saga made to accounts. Append-only: the application user gets no UPDATE or DELETE.
create table transfer_step (
    id          bigserial   primary key,
    transfer_id uuid        not null references transfer (id),
    step        text        not null,
    outcome     text        not null check (outcome in ('SUCCESS', 'REFUSED', 'TRANSIENT_FAILURE')),
    http_status int,
    detail      text,
    created_at  timestamptz not null
);

create index transfer_step_transfer_idx on transfer_step (transfer_id, id);
