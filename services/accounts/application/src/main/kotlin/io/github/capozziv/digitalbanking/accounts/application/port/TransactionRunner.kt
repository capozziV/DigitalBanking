package io.github.capozziv.digitalbanking.accounts.application.port

interface TransactionRunner {
    fun <T> inTransaction(block: () -> T): T

    /** Runs a block that only reads, so every read sees the same committed state. */
    fun <T> inReadOnlyTransaction(block: () -> T): T
}
