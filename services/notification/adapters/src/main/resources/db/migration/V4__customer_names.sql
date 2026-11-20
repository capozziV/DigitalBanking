-- Local copy of customer -> full name, filled from CustomerRegistered (customer-events.fifo). Read with
-- known_accounts to tell a transfer with the name of the other party; the text is built when it is read, so the
-- notifications written before keep working without any change.
create table customer_names (
    customer_id uuid primary key,
    name        text not null
);

-- Upsert: the last registration of the customer wins. No DELETE, like the other tables.
grant select, insert, update on customer_names to ${appUser};
