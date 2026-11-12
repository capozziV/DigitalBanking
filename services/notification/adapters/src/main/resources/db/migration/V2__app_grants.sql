-- Grants of the application user (${appUser}). Flyway runs as the owner of the tables.
-- No DELETE anywhere: notifications are only marked as read; processed_events and known_accounts are append-only.
grant usage on schema public to ${appUser};
grant select, insert, update on notifications to ${appUser};
grant select, insert on known_accounts to ${appUser};
grant select, insert on processed_events to ${appUser};
