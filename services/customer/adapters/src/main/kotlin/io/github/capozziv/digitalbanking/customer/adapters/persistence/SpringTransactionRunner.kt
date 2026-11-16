package io.github.capozziv.digitalbanking.customer.adapters.persistence

import io.github.capozziv.digitalbanking.customer.application.port.TransactionRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

/**
 * Runs the block in a transaction of the default TransactionTemplate; any exception rolls it back. Read-only
 * blocks run in a copy of it that is read-only.
 */
@Component
class SpringTransactionRunner(
    private val transactionTemplate: TransactionTemplate,
) : TransactionRunner {
    private val readOnlyTemplate =
        TransactionTemplate(checkNotNull(transactionTemplate.transactionManager), transactionTemplate)
            .apply { isReadOnly = true }

    override fun <T> inTransaction(block: () -> T): T = run(transactionTemplate, block)

    override fun <T> inReadOnlyTransaction(block: () -> T): T = run(readOnlyTemplate, block)

    private fun <T> run(
        template: TransactionTemplate,
        block: () -> T,
    ): T {
        var result: Result<T>? = null
        template.executeWithoutResult { result = Result.success(block()) }
        return checkNotNull(result) { "transaction finished without running the block" }.getOrThrow()
    }
}
