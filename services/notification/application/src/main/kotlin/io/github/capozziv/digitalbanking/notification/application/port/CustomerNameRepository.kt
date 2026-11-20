package io.github.capozziv.digitalbanking.notification.application.port

import java.util.UUID

/**
 * Local copy of customer → full name, filled from `CustomerRegistered`, read together with the account → customer
 * copy of [KnownAccountRepository] to tell a transfer with the name of the other party.
 */
interface CustomerNameRepository {
    /** Upsert: the last registration of the customer wins. */
    fun save(
        customerId: UUID,
        name: String,
    )

    /**
     * Account id → full name of its owner, for the accounts whose owner and name are both known; the others are
     * missing from the map. One query for a whole page.
     */
    fun namesOfAccountOwners(accountIds: Set<UUID>): Map<UUID, String>
}
