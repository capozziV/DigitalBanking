-- Customer that owned the source account when the transfer was requested, as accounts answered. Only that customer
-- (or an admin) sees the transfer. Null for transfers requested before stage 7: those only an admin sees.
alter table transfer add column source_customer_id uuid;

-- The table grant of V4 already covers a new column; repeated here so the column's access is explicit.
grant select, insert, update on transfer to ${appUser};
