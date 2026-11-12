-- Daily limit of transfers (stage 6). The transfer table is the source of truth: the reservations of an account on a
-- day are its transfers with that limit_day, limit_reserved = true and status PENDING or COMPLETED.

-- limit_day is the São Paulo calendar day of the request, fixed for the transfer. Old rows get it from created_at.
alter table transfer add column limit_day date;

update transfer
set limit_day = (created_at at time zone 'America/Sao_Paulo')::date
where limit_day is null;

alter table transfer alter column limit_day set not null;

-- Old transfers never reserved: they keep false and are not counted nor released.
alter table transfer add column limit_reserved boolean not null default false;

-- Sum of an account's reservations on a day.
create index transfer_source_limit_day_idx on transfer (source_account_id, limit_day);

-- RESERVE_LIMIT is the new first step of the saga.
alter table transfer drop constraint transfer_step_known;
alter table transfer
    add constraint transfer_step_known check (
        step in ('RESERVE_LIMIT', 'DEBIT', 'RESOLVE_DEBIT', 'CREDIT', 'RESOLVE_CREDIT', 'COMPENSATE', 'COMPLETED', 'FAILED',
                 'COMPENSATED')
    );

-- Bumped by every reservation made in PostgreSQL while Redis is down: a Redis counter seeded with another epoch is stale
-- and is rebuilt before the next reservation. No row means epoch 0.
create table daily_limit_epoch (
    account_id uuid   not null,
    limit_day  date   not null,
    epoch      bigint not null,
    primary key (account_id, limit_day)
);

grant select, insert, update on daily_limit_epoch to ${appUser};
