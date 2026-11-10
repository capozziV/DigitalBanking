create table notifications (
    id              uuid        primary key,
    customer_id     uuid        not null,
    kind            text        not null,
    title           text        not null,
    body            text        not null,
    data            jsonb       not null default '{}'::jsonb,
    source_event_id uuid        not null,
    created_at      timestamptz not null,
    read_at         timestamptz,
    -- Safety net: the same event never notifies the same customer twice with the same kind.
    unique (source_event_id, customer_id, kind)
);

create index notifications_customer_page_idx on notifications (customer_id, created_at desc, id desc);

create table known_accounts (
    account_id  uuid primary key,
    customer_id uuid not null
);

create table processed_events (
    event_id     uuid        primary key,
    event_type   text        not null,
    processed_at timestamptz not null
);
