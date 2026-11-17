-- Status of the account: ACTIVE takes debits and credits, BLOCKED takes credits only, CLOSED takes neither.
-- Existing accounts are ACTIVE.
alter table account
    add column status text not null default 'ACTIVE'
        constraint account_status_check check (status in ('ACTIVE', 'BLOCKED', 'CLOSED'));

-- One account per customer becomes one account NOT CLOSED per customer: after closing, the customer may open another.
-- A blocked account still counts. The insert of the service uses ON CONFLICT DO NOTHING (no target), which also
-- covers this partial unique index.
alter table account drop constraint account_customer_id_key;
create unique index account_customer_id_not_closed_key on account (customer_id) where status <> 'CLOSED';

-- The application user (${appUser}) also changes the status of an account (with its updated_at, already granted by V7).
grant update (status) on account to ${appUser};
