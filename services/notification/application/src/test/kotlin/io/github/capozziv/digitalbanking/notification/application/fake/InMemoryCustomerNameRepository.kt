package io.github.capozziv.digitalbanking.notification.application.fake

import io.github.capozziv.digitalbanking.notification.application.port.CustomerNameRepository
import java.util.UUID

/** Joins its names with the account owners of [accounts], as the SQL adapter joins the two tables. */
class InMemoryCustomerNameRepository(
    private val transactions: ImmediateTransactionRunner,
    private val accounts: InMemoryKnownAccountRepository,
) : CustomerNameRepository {
    val names = linkedMapOf<UUID, String>()

    override fun save(
        customerId: UUID,
        name: String,
    ) {
        check(transactions.open) { "save outside a transaction" }
        names[customerId] = name
    }

    override fun namesOfAccountOwners(accountIds: Set<UUID>): Map<UUID, String> =
        accountIds
            .mapNotNull { account -> accounts.owners[account]?.let { names[it] }?.let { account to it } }
            .toMap()
}
