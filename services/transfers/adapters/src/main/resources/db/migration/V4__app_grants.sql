-- Grants of the application user (${appUser}). Flyway runs as the owner of the tables.
-- No UPDATE or DELETE on transfer_step: the database enforces the immutability of the audit trail.
grant usage on schema public to ${appUser};
grant select, insert, update on transfer, outbox to ${appUser};
grant select, insert on transfer_step to ${appUser};
grant usage on sequence outbox_position_seq to ${appUser};
grant usage on sequence transfer_step_id_seq to ${appUser};
