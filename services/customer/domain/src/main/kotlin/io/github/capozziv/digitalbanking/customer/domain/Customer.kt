package io.github.capozziv.digitalbanking.customer.domain

import java.time.Instant
import java.util.UUID

/** Whether the customer may operate: a [BLOCKED] customer cannot open an account or start a transfer. */
enum class CustomerStatus { ACTIVE, BLOCKED }

/**
 * A customer of the bank (simulated, fictitious data). Its [id] is the `sub` of the user who registered it:
 * one user has at most one customer, and every service knows the customer from the token alone.
 */
data class Customer(
    val id: UUID,
    val name: String,
    val email: String,
    val phone: PhoneNumber,
    val status: CustomerStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(name.isNotBlank() && name.length <= MAX_NAME_LENGTH) { "name must have 1 to $MAX_NAME_LENGTH characters" }
        require(email.length <= MAX_EMAIL_LENGTH && EMAIL.matches(email)) { "email is not valid" }
    }

    /** Sets [newStatus]. The same status is a no-op without event, so repeating the change is safe. */
    fun changeStatus(
        newStatus: CustomerStatus,
        now: Instant,
    ): Pair<Customer, CustomerEvent?> =
        when {
            newStatus == status -> this to null
            newStatus == CustomerStatus.BLOCKED -> copy(status = newStatus, updatedAt = now) to CustomerBlocked(id)
            else -> copy(status = newStatus, updatedAt = now) to CustomerUnblocked(id)
        }

    /** Sets [newPhone]. The same number is a no-op without event, so repeating the change is safe. */
    fun changePhone(
        newPhone: PhoneNumber,
        now: Instant,
    ): Pair<Customer, CustomerPhoneChanged?> =
        if (newPhone == phone) {
            this to null
        } else {
            copy(phone = newPhone, updatedAt = now) to CustomerPhoneChanged(id, phone, newPhone)
        }

    companion object {
        const val MAX_NAME_LENGTH = 120
        const val MAX_EMAIL_LENGTH = 254

        /** Anchored, since the web layer publishes it as the OpenAPI `pattern`, which is not implicitly anchored. */
        const val EMAIL_PATTERN = "^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$"

        private val EMAIL = Regex(EMAIL_PATTERN)

        /** A new, active customer with the id of the user, and the event that records it. */
        fun register(
            id: UUID,
            name: String,
            email: String,
            phone: PhoneNumber,
            now: Instant,
        ): Pair<Customer, CustomerRegistered> {
            val customer = Customer(id, name.trim(), email.trim(), phone, CustomerStatus.ACTIVE, now, now)
            return customer to CustomerRegistered(id, customer.name, customer.email, phone)
        }
    }
}

/** What happened to a customer; recorded in the outbox in the same transaction as the change. */
sealed interface CustomerEvent {
    val customerId: UUID
}

data class CustomerRegistered(
    override val customerId: UUID,
    val name: String,
    val email: String,
    val phone: PhoneNumber,
) : CustomerEvent

data class CustomerPhoneChanged(
    override val customerId: UUID,
    val previousPhone: PhoneNumber,
    val newPhone: PhoneNumber,
) : CustomerEvent

data class CustomerBlocked(
    override val customerId: UUID,
) : CustomerEvent

data class CustomerUnblocked(
    override val customerId: UUID,
) : CustomerEvent
