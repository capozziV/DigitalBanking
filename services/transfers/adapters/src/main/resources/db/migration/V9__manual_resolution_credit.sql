-- A manual compensation names the credit the admin recorded on the source account in accounts (its idempotency key),
-- checked against accounts before the transfer ends COMPENSATED. One credit puts back the value of one transfer only:
-- the key is unique. No row predates this column outside development databases, so it is required right away.
alter table transfer_manual_resolution
    add column credit_idempotency_key text not null
        check (length(btrim(credit_idempotency_key)) between 1 and 255);

alter table transfer_manual_resolution
    add constraint transfer_manual_resolution_credit_key unique (credit_idempotency_key);
