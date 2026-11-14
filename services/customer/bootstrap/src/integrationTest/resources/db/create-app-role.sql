-- Application role used by the integration tests; Flyway runs as the container's owner user.
CREATE ROLE customer_app WITH LOGIN PASSWORD 'app';
