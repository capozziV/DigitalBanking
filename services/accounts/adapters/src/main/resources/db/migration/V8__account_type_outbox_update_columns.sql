-- The application user (${appUser}) only changes the overdraft limit of an account type (with its updated_at),
-- and, in the outbox, the relay only records the publication or a failed attempt. The other columns are written
-- once, on insert. The table-wide UPDATE of V5 becomes column grants.
revoke update on account_type from ${appUser};
grant update (overdraft_limit, updated_at) on account_type to ${appUser};

revoke update on outbox from ${appUser};
grant update (published_at, attempts, last_error) on outbox to ${appUser};
