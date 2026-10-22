-- One account per customer; the balance lives in the ledger entries, not here.
create table account (
    id                      uuid        primary key,
    customer_id             uuid        not null constraint account_customer_id_key unique,
    type                    text        not null constraint account_type_fkey references account_type (type),
    opening_idempotency_key text        not null constraint account_opening_idempotency_key_key unique,
    created_at              timestamptz not null default now(),
    updated_at              timestamptz not null default now()
);
