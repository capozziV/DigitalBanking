-- The customer of the bank (simulated, fictitious data). The id is the `sub` of the user who registered it.
create table customer (
    id         uuid        primary key,
    name       text        not null,
    email      text        not null,
    status     text        not null check (status in ('ACTIVE', 'BLOCKED')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
