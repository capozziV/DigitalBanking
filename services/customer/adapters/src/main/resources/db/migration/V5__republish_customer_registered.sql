-- Republishes CustomerRegistered v2 for every existing customer (issue 232).
-- Why: the notification service learns the names of the other party of a transfer from CustomerRegistered, through its own
-- queue (notification-customer.fifo, ADR 0025). Customers registered before that queue and its subscription existed had their
-- event published to a topic with no subscriber, so the projection never saw them and the notifications said "outro cliente".
-- The notification consumer is an idempotent upsert, so a repeated event is harmless; the relay sends these rows like any other.
-- Precondition: the queue and its subscription to customer-events.fifo exist before this customer version starts (the local
-- ready.d scripts and the Terraform create them before the services). Events published while no queue exists are lost to it.
-- Same envelope and payload as OutboxEventAppender writes on registration: the aggregate id is the customer id (FIFO group),
-- the phone is the current one, and there is no correlation id (nothing triggered it). An empty database writes nothing.
insert into outbox (event_id, aggregate_id, event_type, event_version, payload, occurred_at)
select gen_random_uuid(),
       c.id::text,
       'CustomerRegistered',
       2,
       jsonb_build_object('customerId', c.id::text, 'name', c.name, 'email', c.email, 'phone', c.phone),
       now()
from customer c
order by c.created_at, c.id;
