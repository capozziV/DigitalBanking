package io.github.capozziv.digitalbanking.testing.architecture.samples.bad.domain

import java.sql.Connection

class DomainWithSql(
    val connection: Connection,
)
