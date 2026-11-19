-- Brazilian mobile phone of the customer in E.164 (+55DD9XXXXXXXX), unique among customers, not verified.
alter table customer add column phone varchar(14);

-- Customers that already exist get a fictitious, unique number (+55119 and a sequence of eight digits), so the column can be not null.
update customer c
set phone = '+55119' || lpad(n.sequence::text, 8, '0')
from (select id, row_number() over (order by created_at, id) as sequence from customer) n
where c.id = n.id;

alter table customer alter column phone set not null;
alter table customer add constraint customer_phone_format check (phone ~ '^\+55[1-9][0-9]9[0-9]{8}$');
create unique index customer_phone_key on customer (phone);
