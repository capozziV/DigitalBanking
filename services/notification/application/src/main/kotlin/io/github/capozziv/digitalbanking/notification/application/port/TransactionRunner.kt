package io.github.capozziv.digitalbanking.notification.application.port

interface TransactionRunner {
    fun <T> inTransaction(block: () -> T): T
}
