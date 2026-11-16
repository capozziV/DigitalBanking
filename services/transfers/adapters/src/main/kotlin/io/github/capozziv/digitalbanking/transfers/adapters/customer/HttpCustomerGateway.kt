package io.github.capozziv.digitalbanking.transfers.adapters.customer

import io.github.capozziv.digitalbanking.transfers.adapters.customer.dto.CustomerProblemDTO
import io.github.capozziv.digitalbanking.transfers.adapters.customer.dto.CustomerStatusResponseDTO
import io.github.capozziv.digitalbanking.transfers.application.port.CustomerGateway
import io.github.capozziv.digitalbanking.transfers.application.port.CustomerLookup
import org.slf4j.LoggerFactory
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import tools.jackson.core.JacksonException
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.io.IOException
import java.util.UUID

/**
 * Asks `customer` for the status of a customer (`GET /internal/customers/{id}`), every time, with no cache.
 *
 * Only two answers are taken as they come: a 200 with `ACTIVE` or `BLOCKED`, and a 404 `customer-not-found`.
 * Anything else (timeout, cut connection, no machine token, 5xx, another 4xx, a 404 of another kind, an unknown
 * status, a body that is not a customer) is [CustomerLookup.UNAVAILABLE]: the request is answered with 503 and
 * nothing is created, instead of guessing.
 */
class HttpCustomerGateway(
    private val restClient: RestClient,
) : CustomerGateway {
    private val mapper: JsonMapper = jsonMapper { addModule(kotlinModule()) }

    override fun lookupCustomer(id: UUID): CustomerLookup {
        val (status, body) =
            try {
                restClient
                    .get()
                    .uri("/internal/customers/{id}", id)
                    .exchange { _, res -> res.statusCode.value() to res.body.readNBytes(MAX_BODY_BYTES) }
            } catch (e: RestClientException) {
                return unavailable(id, "no response: ${e.javaClass.simpleName}")
            } catch (e: IOException) {
                return unavailable(id, "no response: ${e.javaClass.simpleName}")
            }
        return when (status) {
            200 -> {
                when (read<CustomerStatusResponseDTO>(body)?.status) {
                    ACTIVE -> CustomerLookup.ACTIVE
                    BLOCKED -> CustomerLookup.BLOCKED
                    else -> unavailable(id, "status 200 without a known customer status")
                }
            }

            404 -> {
                if (read<CustomerProblemDTO>(body)?.type?.substringAfterLast('/') == CUSTOMER_NOT_FOUND) {
                    CustomerLookup.NOT_FOUND
                } else {
                    unavailable(id, "status 404 that is not $CUSTOMER_NOT_FOUND")
                }
            }

            else -> {
                unavailable(id, "status $status")
            }
        }
    }

    private fun unavailable(
        id: UUID,
        why: String,
    ): CustomerLookup {
        log.warn("customer could not tell the status of customer {}: {}", id, why)
        return CustomerLookup.UNAVAILABLE
    }

    private inline fun <reified T : Any> read(body: ByteArray): T? =
        try {
            mapper.readValue(body, T::class.java)
        } catch (_: JacksonException) {
            null
        }

    private companion object {
        const val ACTIVE = "ACTIVE"
        const val BLOCKED = "BLOCKED"
        const val CUSTOMER_NOT_FOUND = "customer-not-found"
        const val MAX_BODY_BYTES = 64 * 1024
        val log = LoggerFactory.getLogger(HttpCustomerGateway::class.java)
    }
}
