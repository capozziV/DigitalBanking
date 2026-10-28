package io.github.capozziv.digitalbanking.notification.application.port

import java.util.UUID

/** Local copy of account → customer, filled from `AccountOpened`. */
interface KnownAccountRepository {
    /** Idempotent: saving an account already known keeps the stored owner. */
    fun save(
        accountId: UUID,
        customerId: UUID,
    )

    fun customerOf(accountId: UUID): UUID?
}
