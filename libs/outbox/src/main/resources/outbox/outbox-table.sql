-- Model migration for the Transactional Outbox table.
-- Each service copies this file into its own Flyway migrations.
create table outbox (
    position       bigserial   primary key,
    event_id       uuid        not null unique,
    aggregate_id   text        not null,
    event_type     text        not null,
    event_version  int         not null,
    payload        jsonb       not null,
    correlation_id text,
    occurred_at    timestamptz not null,
    published_at   timestamptz null,
    attempts       int         not null default 0,
    last_error     text
);

create index outbox_unpublished_idx on outbox (position) where published_at is null;
