-- Copied from libs/outbox/src/main/resources/outbox/outbox-table.sql (index added with the relay).
create index outbox_stuck_idx on outbox (aggregate_id, position) where published_at is null and attempts > 0;
