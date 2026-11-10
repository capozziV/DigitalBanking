-- Amounts in cents of data used to be written as JSON strings ("12345"); they are JSON numbers from now on.
-- Converts the rows written before, so every notification has the same format.
update notifications
set data = jsonb_set(data, '{amountCents}', to_jsonb((data ->> 'amountCents')::bigint))
where jsonb_typeof(data -> 'amountCents') = 'string';

update notifications
set data = jsonb_set(data, '{overdraftLimitCents}', to_jsonb((data ->> 'overdraftLimitCents')::bigint))
where jsonb_typeof(data -> 'overdraftLimitCents') = 'string';
