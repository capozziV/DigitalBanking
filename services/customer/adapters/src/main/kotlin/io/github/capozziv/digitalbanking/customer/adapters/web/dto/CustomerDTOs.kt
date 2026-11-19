package io.github.capozziv.digitalbanking.customer.adapters.web.dto

import io.github.capozziv.digitalbanking.customer.domain.Customer
import io.github.capozziv.digitalbanking.customer.domain.CustomerStatus
import io.github.capozziv.digitalbanking.customer.domain.PhoneNumber
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

// Request fields are nullable so that a missing value reaches bean validation and becomes a field error.

data class RegisterCustomerDTO(
    @field:NotBlank @field:Size(max = Customer.MAX_NAME_LENGTH) val name: String?,
    @field:NotBlank
    @field:Size(max = Customer.MAX_EMAIL_LENGTH)
    @field:Pattern(regexp = Customer.EMAIL_PATTERN, message = "must be a valid email address")
    val email: String?,
    @field:NotBlank
    @field:Pattern(regexp = PhoneNumber.PATTERN, message = RegisterCustomerDTO.PHONE_MESSAGE)
    val phone: String?,
) {
    companion object {
        const val PHONE_MESSAGE = "must be a Brazilian mobile number in E.164, such as +5511987654321"
    }
}

data class ChangePhoneDTO(
    @field:NotBlank
    @field:Pattern(regexp = PhoneNumber.PATTERN, message = RegisterCustomerDTO.PHONE_MESSAGE)
    val phone: String?,
)

data class ChangeCustomerStatusDTO(
    @field:NotNull val status: CustomerStatus?,
)

data class CustomerDTO(
    val id: UUID,
    val name: String,
    val email: String,
    val phone: String,
    val status: CustomerStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(customer: Customer) =
            CustomerDTO(
                id = customer.id,
                name = customer.name,
                email = customer.email,
                phone = customer.phone.value,
                status = customer.status,
                createdAt = customer.createdAt,
                updatedAt = customer.updatedAt,
            )
    }
}

/** What the other services need to know about a customer: only the id and whether it may operate. */
data class CustomerLookupDTO(
    val id: UUID,
    val status: CustomerStatus,
) {
    companion object {
        fun from(customer: Customer) = CustomerLookupDTO(customer.id, customer.status)
    }
}

/** What a customer learns from a phone number: who owns it (the full name) and its id. */
data class CustomerLookupByPhoneDTO(
    val customerId: UUID,
    val fullName: String,
) {
    companion object {
        fun from(customer: Customer) = CustomerLookupByPhoneDTO(customer.id, customer.name)
    }
}
