package io.github.capozziv.digitalbanking.accounts.adapters.customer

import io.github.capozziv.digitalbanking.accounts.adapters.customer.dto.CustomerStatusResponseDTO
import io.github.capozziv.digitalbanking.accounts.application.CustomersUnavailableException
import io.github.capozziv.digitalbanking.accounts.application.port.CustomerGateway
import io.github.capozziv.digitalbanking.accounts.application.port.CustomerStatus
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import tools.jackson.core.JacksonException
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.util.UUID

/**
 * Asks the `customer` service for the status of a customer (`GET /internal/customers/{id}`), with the machine token
 * and the correlation id that the [RestClient] interceptors add.
 *
 * - 200 with `ACTIVE` or `BLOCKED` → that status; 404 → null (no such customer).
 * - Anything else is [CustomersUnavailableException]: a timeout, a refused connection, no machine token, a 5xx, a
 *   401/403 (a bad token is a configuration fault, not a missing customer) or a 200 with an unexpected body.
 */
class HttpCustomerGateway(
    private val restClient: RestClient,
) : CustomerGateway {
    private val mapper = jsonMapper { addModule(kotlinModule()) }

    override fun findStatus(customerId: UUID): CustomerStatus? {
        val (status, body) =
            try {
                restClient
                    .get()
                    .uri("/internal/customers/{id}", customerId)
                    .exchange { _, response -> response.statusCode.value() to response.body.readAllBytes() }
            } catch (e: RestClientException) {
                throw unavailable(customerId, "request failed: ${e.javaClass.simpleName}", e)
            }
        return when (status) {
            HttpStatus.OK.value() -> parse(customerId, body)
            HttpStatus.NOT_FOUND.value() -> null
            else -> throw unavailable(customerId, "answered $status")
        }
    }

    private fun parse(
        customerId: UUID,
        body: ByteArray,
    ): CustomerStatus {
        val dto =
            try {
                mapper.readValue(body, CustomerStatusResponseDTO::class.java)
            } catch (e: JacksonException) {
                throw unavailable(customerId, "answered 200 with a body that is not JSON", e)
            }
        if (dto.id != customerId) throw unavailable(customerId, "answered 200 for another customer")
        return CustomerStatus.entries.find { it.name == dto.status }
            ?: throw unavailable(customerId, "answered 200 with an unknown status")
    }

    private fun unavailable(
        customerId: UUID,
        reason: String,
        cause: Throwable? = null,
    ): CustomersUnavailableException {
        log.warn("customer service unavailable for customer {}: {}", customerId, reason)
        return CustomersUnavailableException("customer service $reason", cause)
    }

    private companion object {
        val log = LoggerFactory.getLogger(HttpCustomerGateway::class.java)
    }
}
