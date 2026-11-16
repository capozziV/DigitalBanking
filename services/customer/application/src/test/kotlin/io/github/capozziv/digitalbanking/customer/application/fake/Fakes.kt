package io.github.capozziv.digitalbanking.customer.application.fake

import io.github.capozziv.digitalbanking.customer.application.port.CustomerRepository
import io.github.capozziv.digitalbanking.customer.application.port.EventOutbox
import io.github.capozziv.digitalbanking.customer.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.customer.domain.Customer
import io.github.capozziv.digitalbanking.customer.domain.CustomerEvent
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class InMemoryCustomerRepository : CustomerRepository {
    private val customers = mutableMapOf<UUID, Customer>()

    var locks = 0
        private set

    override fun insertIfAbsent(customer: Customer): Boolean = customers.putIfAbsent(customer.id, customer) == null

    override fun findById(id: UUID): Customer? = customers[id]

    override fun lockById(id: UUID): Customer? {
        locks++
        return customers[id]
    }

    override fun update(customer: Customer) {
        check(customer.id in customers) { "customer ${customer.id} does not exist" }
        customers[customer.id] = customer
    }

    fun all(): List<Customer> = customers.values.toList()
}

class RecordingEventOutbox : EventOutbox {
    private val recorded = mutableListOf<CustomerEvent>()

    val events: List<CustomerEvent> get() = recorded.toList()

    override fun append(event: CustomerEvent) {
        recorded += event
    }
}

class DirectTransactionRunner : TransactionRunner {
    var transactions = 0
        private set

    var readOnlyTransactions = 0
        private set

    override fun <T> inTransaction(block: () -> T): T {
        transactions++
        return block()
    }

    override fun <T> inReadOnlyTransaction(block: () -> T): T {
        readOnlyTransactions++
        return block()
    }
}

class MutableClock(
    private var now: Instant,
) : Clock() {
    override fun getZone() = ZoneOffset.UTC

    override fun withZone(zone: java.time.ZoneId?): Clock = this

    override fun instant(): Instant = now

    fun advance(seconds: Long) {
        now = now.plusSeconds(seconds)
    }
}
