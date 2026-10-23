package io.github.capozziv.digitalbanking.accounts.application.fake

import io.github.capozziv.digitalbanking.accounts.application.AccountBusyException
import io.github.capozziv.digitalbanking.accounts.application.port.AccountRepository
import io.github.capozziv.digitalbanking.accounts.application.port.InsertAccountResult
import io.github.capozziv.digitalbanking.accounts.domain.Account
import java.util.UUID

class InMemoryAccountRepository : AccountRepository {
    private val accounts = mutableMapOf<UUID, Account>()
    private val openingKeys = mutableMapOf<String, UUID>()

    /** Accounts whose lock times out: lockById throws AccountBusyException for them. */
    val busyAccounts = mutableSetOf<UUID>()

    override fun insert(
        account: Account,
        openingIdempotencyKey: String,
    ): InsertAccountResult {
        if (openingIdempotencyKey in openingKeys) return InsertAccountResult.DUPLICATE_OPENING_KEY
        if (accounts.values.any { it.customerId == account.customerId }) return InsertAccountResult.DUPLICATE_CUSTOMER
        accounts[account.id] = account
        openingKeys[openingIdempotencyKey] = account.id
        return InsertAccountResult.INSERTED
    }

    override fun findById(id: UUID): Account? = accounts[id]

    override fun lockById(id: UUID): Account? {
        if (id in busyAccounts) throw AccountBusyException(id)
        return accounts[id]
    }

    override fun findByOpeningIdempotencyKey(key: String): Account? = openingKeys[key]?.let { accounts[it] }

    override fun updateType(account: Account) {
        accounts[account.id] = account
    }

    fun all(): List<Account> = accounts.values.toList()
}
