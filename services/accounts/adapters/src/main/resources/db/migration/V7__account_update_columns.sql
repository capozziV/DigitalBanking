-- The application user (${appUser}) only changes the type of an account (with its updated_at); the other
-- columns are written once, on insert. The table-wide UPDATE of V5 becomes a column grant.
revoke update on account from ${appUser};
grant update (type, updated_at) on account to ${appUser};
