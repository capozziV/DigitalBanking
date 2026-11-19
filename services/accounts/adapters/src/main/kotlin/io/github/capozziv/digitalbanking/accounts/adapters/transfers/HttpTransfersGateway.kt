package io.github.capozziv.digitalbanking.accounts.adapters.transfers

import io.github.capozziv.digitalbanking.accounts.adapters.transfers.dto.PendingTransfersResponseDTO
import io.github.capozziv.digitalbanking.accounts.application.TransfersUnavailableException
import io.github.capozziv.digitalbanking.accounts.application.port.TransfersGateway
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import tools.jackson.core.JacksonException
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.util.UUID

/**
 * Asks the `transfers` service how many transfers leaving an account are PENDING
 * (`GET /internal/accounts/{id}/pending-transfers` → `{"count": N}`), with the machine token and the correlation id
 * that the [RestClient] interceptors add.
 *
 * Only a 200 with a count of zero or more is an answer. Anything else is [TransfersUnavailableException]: a timeout, a
 * refused connection, no machine token, any other status (a 404 means the route is missing, not "no transfers") or a
 * body without a valid count.
 */
class HttpTransfersGateway(
    private val restClient: RestClient,
) : TransfersGateway {
    private val mapper = jsonMapper { addModule(kotlinModule()) }

    override fun pendingOutgoing(accountId: UUID): Int {
        val (status, body) =
            try {
                restClient
                    .get()
                    .uri("/internal/accounts/{id}/pending-transfers", accountId)
                    .exchange { _, response -> response.statusCode.value() to response.body.readAllBytes() }
            } catch (e: RestClientException) {
                throw unavailable(accountId, "request failed: ${e.javaClass.simpleName}", e)
            }
        if (status != HttpStatus.OK.value()) throw unavailable(accountId, "answered $status")
        val dto =
            try {
                mapper.readValue(body, PendingTransfersResponseDTO::class.java)
            } catch (e: JacksonException) {
                throw unavailable(accountId, "answered 200 with a body that is not JSON", e)
            }
        val count = dto.count ?: throw unavailable(accountId, "answered 200 without a count")
        if (count < 0) throw unavailable(accountId, "answered 200 with a negative count")
        return count
    }

    private fun unavailable(
        accountId: UUID,
        reason: String,
        cause: Throwable? = null,
    ): TransfersUnavailableException {
        log.warn("transfers service unavailable for account {}: {}", accountId, reason)
        return TransfersUnavailableException("transfers service $reason", cause)
    }

    private companion object {
        val log = LoggerFactory.getLogger(HttpTransfersGateway::class.java)
    }
}
