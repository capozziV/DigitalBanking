package io.github.capozziv.digitalbanking.accounts.application.port

interface TransactionRunner {
    fun <T> inTransaction(block: () -> T): T
}
