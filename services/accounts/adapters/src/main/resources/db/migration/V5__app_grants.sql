-- Grants of the application user (${appUser}). Flyway runs as the owner of the tables.
-- No UPDATE or DELETE on ledger_entry: the database enforces the immutability of the ledger.
grant usage on schema public to ${appUser};
grant select, insert on ledger_entry to ${appUser};
grant select, insert, update on account, account_type, outbox to ${appUser};
grant usage on sequence outbox_position_seq to ${appUser};
