-- The welcome bonus check looks for another account of the same customer in any status (closed ones included).
-- Since V9 the only index on customer_id is partial (status <> 'CLOSED'), which cannot serve that lookup, so every
-- account opening scanned the table. An index needs no grant: the application user already reads account.
create index account_customer_id_idx on account (customer_id);
