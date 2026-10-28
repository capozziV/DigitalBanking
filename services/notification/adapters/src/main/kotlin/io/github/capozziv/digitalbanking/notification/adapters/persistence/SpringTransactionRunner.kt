package io.github.capozziv.digitalbanking.notification.adapters.persistence

import io.github.capozziv.digitalbanking.notification.application.port.TransactionRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

/** Runs the block in a transaction of the default TransactionTemplate; any exception rolls it back. */
@Component
class SpringTransactionRunner(
    private val transactionTemplate: TransactionTemplate,
) : TransactionRunner {
    override fun <T> inTransaction(block: () -> T): T {
        var result: Result<T>? = null
        transactionTemplate.executeWithoutResult { result = Result.success(block()) }
        return checkNotNull(result) { "transaction finished without running the block" }.getOrThrow()
    }
}
