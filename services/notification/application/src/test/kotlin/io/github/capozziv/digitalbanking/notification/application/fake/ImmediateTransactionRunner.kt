package io.github.capozziv.digitalbanking.notification.application.fake

import io.github.capozziv.digitalbanking.notification.application.port.TransactionRunner

/** Runs the block right away; the other fakes use [open] to prove every write happens inside a transaction. */
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
