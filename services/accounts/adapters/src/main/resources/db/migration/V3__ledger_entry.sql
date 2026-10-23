-- Immutable ledger: the application user has no UPDATE or DELETE on this table (V5).
create table ledger_entry (
    id              uuid         primary key,
    account_id      uuid         not null constraint ledger_entry_account_id_fkey references account (id),
    sequence        bigint       not null constraint ledger_entry_sequence_check check (sequence >= 1),
    kind            text         not null constraint ledger_entry_kind_check check (kind in ('CREDIT', 'DEBIT')),
    amount          bigint       not null constraint ledger_entry_amount_check check (amount > 0),
    balance_after   bigint       not null,
    description     varchar(140),
    idempotency_key text         not null,
    created_at      timestamptz  not null,
    -- Also serves the "last entry" lookup (ORDER BY sequence DESC LIMIT 1).
    constraint ledger_entry_account_id_sequence_key unique (account_id, sequence),
    constraint ledger_entry_account_id_idempotency_key_key unique (account_id, idempotency_key)
);
