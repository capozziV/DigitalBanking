package io.github.capozziv.digitalbanking.transfers.application.fake

import io.github.capozziv.digitalbanking.transfers.application.port.TransactionRunner

/** Runs the block right away and records whether the gateway was ever called while a transaction was open. */
class ImmediateTransactionRunner : TransactionRunner {
    var open = false
        private set
    var transactions = 0
        private set

    override fun <T> inTransaction(block: () -> T): T {
        check(!open) { "nested transaction" }
        open = true
        transactions++
        try {
            return block()
        } finally {
            open = false
        }
    }
}
