package io.github.capozziv.digitalbanking.accounts.application.fake

import io.github.capozziv.digitalbanking.accounts.application.port.TransactionRunner

class DirectTransactionRunner : TransactionRunner {
    var transactions = 0
        private set

    override fun <T> inTransaction(block: () -> T): T {
        transactions++
        return block()
    }
}
