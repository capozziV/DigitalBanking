-- An admin ended by hand a transfer stuck in COMPENSATE (accounts kept refusing the compensation credit, a bug): who,
-- when and why. At most one per transfer. Append-only audit, like transfer_step: the application user gets no UPDATE or
-- DELETE.
create table transfer_manual_resolution (
    transfer_id uuid        primary key references transfer (id),
    resolved_by uuid        not null,
    reason      text        not null check (length(btrim(reason)) between 1 and 500),
    resolved_at timestamptz not null
);

grant select, insert on transfer_manual_resolution to ${appUser};
