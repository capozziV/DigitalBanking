package io.github.capozziv.digitalbanking.notification.application.fake

import io.github.capozziv.digitalbanking.notification.application.port.KnownAccountRepository
import java.util.UUID

class InMemoryKnownAccountRepository(
    private val transactions: ImmediateTransactionRunner,
) : KnownAccountRepository {
    val owners = linkedMapOf<UUID, UUID>()

    override fun save(
        accountId: UUID,
        customerId: UUID,
    ) {
        check(transactions.open) { "save outside a transaction" }
        owners.putIfAbsent(accountId, customerId)
    }

    override fun customerOf(accountId: UUID): UUID? = owners[accountId]
}
