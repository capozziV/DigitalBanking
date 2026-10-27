package io.github.capozziv.digitalbanking.testing.architecture.samples.good.adapters.persistence

import java.sql.Connection

class JdbcOrderRepository(
    private val connection: Connection,
)
