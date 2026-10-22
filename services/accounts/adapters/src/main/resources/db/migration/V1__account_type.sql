-- Overdraft limit of each account type. The list of types lives in the AccountType enum of the code;
-- a new type is a new enum value plus a migration seeding its row.
create table account_type (
    type            text        primary key,
    overdraft_limit bigint      not null constraint account_type_overdraft_limit_check check (overdraft_limit >= 0),
    updated_at      timestamptz not null default now()
);

-- Limits in cents.
insert into account_type (type, overdraft_limit) values
    ('FREE', 0),
    ('STANDARD', 50000),
    ('GOLD', 200000),
    ('BLACK', 1000000),
    ('DIAMOND', 5000000);
