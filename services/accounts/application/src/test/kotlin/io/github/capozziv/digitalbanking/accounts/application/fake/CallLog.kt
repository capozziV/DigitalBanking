package io.github.capozziv.digitalbanking.accounts.application.fake

import io.github.capozziv.digitalbanking.accounts.application.port.AccountRepository
import io.github.capozziv.digitalbanking.accounts.application.port.AccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.application.port.EventOutbox
import io.github.capozziv.digitalbanking.accounts.application.port.LedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.accounts.domain.AccountEvent
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.AccountTypeLimit
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntry
import io.github.capozziv.digitalbanking.accounts.domain.OverdraftLimit
import java.time.Instant
import java.util.UUID

/**
 * Records, in order, the port calls made by a use case and the transaction boundaries around them.
 * Each port wrapper delegates to a real fake, so behavior is unchanged.
 */
class CallLog {
    private val recorded = mutableListOf<String>()

    val calls: List<String> get() = recorded.toList()

    fun record(call: String) {
        recorded += call
    }

    fun transactions(delegate: TransactionRunner): TransactionRunner =
        object : TransactionRunner {
            override fun <T> inTransaction(block: () -> T): T {
                record("begin")
                try {
                    return delegate.inTransaction(block)
                } finally {
                    record("end")
                }
            }
        }

    fun accounts(delegate: AccountRepository): AccountRepository =
        object : AccountRepository by delegate {
            override fun findById(id: UUID): Account? = delegate.findById(id).also { record("accounts.findById") }

            override fun lockById(id: UUID): Account? = delegate.lockById(id).also { record("accounts.lockById") }

            override fun updateType(account: Account) {
                delegate.updateType(account)
                record("accounts.updateType")
            }
        }

    fun entries(delegate: LedgerEntryRepository): LedgerEntryRepository =
        object : LedgerEntryRepository {
            override fun findLast(accountId: UUID): LedgerEntry? = delegate.findLast(accountId).also { record("entries.findLast") }

            override fun findByIdempotencyKey(
                accountId: UUID,
                key: String,
            ): LedgerEntry? = delegate.findByIdempotencyKey(accountId, key).also { record("entries.findByIdempotencyKey") }

            override fun insert(entry: LedgerEntry) {
                delegate.insert(entry)
                record("entries.insert")
            }
        }

    fun types(delegate: AccountTypeRepository): AccountTypeRepository =
        object : AccountTypeRepository by delegate {
            override fun findLimit(type: AccountType): OverdraftLimit = delegate.findLimit(type).also { record("types.findLimit") }

            override fun updateLimit(
                type: AccountType,
                limit: OverdraftLimit,
                now: Instant,
            ): AccountTypeLimit = delegate.updateLimit(type, limit, now).also { record("types.updateLimit") }
        }

    fun outbox(delegate: EventOutbox): EventOutbox =
        object : EventOutbox {
            override fun append(event: AccountEvent) {
                delegate.append(event)
                record("outbox.append")
            }
        }
}
