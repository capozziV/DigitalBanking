-- Grants of the application user (${appUser}). Flyway runs as the owner of the tables.
-- Every new table needs its own grant here: there is no ALTER DEFAULT PRIVILEGES. No DELETE anywhere.
grant usage on schema public to ${appUser};
grant select, insert, update on customer, outbox to ${appUser};
grant usage on sequence outbox_position_seq to ${appUser};
