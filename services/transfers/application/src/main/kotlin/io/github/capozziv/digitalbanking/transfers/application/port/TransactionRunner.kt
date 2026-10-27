package io.github.capozziv.digitalbanking.transfers.application.port

interface TransactionRunner {
    fun <T> inTransaction(block: () -> T): T
}
