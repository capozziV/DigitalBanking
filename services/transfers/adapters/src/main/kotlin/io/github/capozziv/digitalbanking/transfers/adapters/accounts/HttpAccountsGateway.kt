package io.github.capozziv.digitalbanking.transfers.adapters.accounts

import io.github.capozziv.digitalbanking.outbox.OutboxEventEnvelope.CORRELATION_ID_MDC_KEY
import io.github.capozziv.digitalbanking.transfers.adapters.accounts.dto.AccountsAccountResponseDTO
import io.github.capozziv.digitalbanking.transfers.adapters.accounts.dto.AccountsEntryRequestDTO
import io.github.capozziv.digitalbanking.transfers.adapters.accounts.dto.AccountsEntryResponseDTO
import io.github.capozziv.digitalbanking.transfers.adapters.accounts.dto.AccountsProblemDTO
import io.github.capozziv.digitalbanking.transfers.application.port.AccountLookup
import io.github.capozziv.digitalbanking.transfers.application.port.AccountsGateway
import io.github.capozziv.digitalbanking.transfers.application.port.CallResult
import io.github.capozziv.digitalbanking.transfers.domain.AccountsCall
import io.github.capozziv.digitalbanking.transfers.domain.StepOutcome
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.io.IOException
import java.util.UUID

/**
 * Calls `accounts` over HTTP and classifies every answer.
 *
 * [perform] never throws for anything the network or the peer can do (timeout, cut connection, HTML from a proxy, a
 * 2xx whose body is not what `accounts` sends, an unexpected 4xx): those are [StepOutcome.TransientFailure] or a
 * classified [StepOutcome.Refused], so the saga retries instead of skipping a step or compensating.
 *
 * [lookupAccount] follows the same rule for the network and the peer (they become [AccountLookup.UNAVAILABLE]), but
 * throws [IllegalStateException] on a 4xx other than "account not found": that is a bug in this client or a
 * misrouted request, and it must not be taken for a missing account.
 */
class HttpAccountsGateway(
    private val restClient: RestClient,
    private val classifier: AccountsResponseClassifier,
) : AccountsGateway {
    private val mapper: JsonMapper = jsonMapper { addModule(kotlinModule()) }

    override fun lookupAccount(id: UUID): AccountLookup {
        val result =
            send(
                describe = "lookup account $id",
                request = restClient.get().uri("/accounts/{id}", id).headers(::addCorrelationId),
                isValidSuccessBody = { body -> parses<AccountsAccountResponseDTO>(body) },
            )
        return when (val outcome = result.outcome) {
            StepOutcome.Success -> {
                AccountLookup.EXISTS
            }

            StepOutcome.TransientFailure -> {
                AccountLookup.UNAVAILABLE
            }

            is StepOutcome.Refused -> {
                if (outcome.reason == ACCOUNT_NOT_FOUND) {
                    AccountLookup.NOT_FOUND
                } else {
                    // Any other 4xx on this read means a bug in this client or a misrouted request, not a missing account.
                    error("unexpected response to GET /accounts/$id: status ${result.httpStatus}, reason ${outcome.reason}")
                }
            }
        }
    }

    override fun perform(call: AccountsCall): CallResult =
        when (call) {
            is AccountsCall.Debit -> {
                record(call.accountId, "debits", call.idempotencyKey, call.amount.cents, call.description)
            }

            is AccountsCall.Credit -> {
                record(call.accountId, "credits", call.idempotencyKey, call.amount.cents, call.description)
            }

            is AccountsCall.FindEntry -> {
                send(
                    describe = "find entry ${call.idempotencyKey} of account ${call.accountId}",
                    request =
                        restClient
                            .get()
                            .uri("/accounts/{id}/entries?idempotencyKey={key}", call.accountId, call.idempotencyKey)
                            .headers(::addCorrelationId),
                    isValidSuccessBody = { body -> parses<AccountsEntryResponseDTO>(body) },
                )
            }
        }

    private fun record(
        accountId: UUID,
        path: String,
        idempotencyKey: String,
        cents: Long,
        description: String,
    ): CallResult =
        send(
            describe = "$path of account $accountId (key $idempotencyKey)",
            request =
                restClient
                    .post()
                    .uri("/accounts/{id}/$path", accountId)
                    .headers(::addCorrelationId)
                    .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(AccountsEntryRequestDTO(cents, description)),
            isValidSuccessBody = { body -> parses<AccountsEntryResponseDTO>(body) },
        )

    private fun addCorrelationId(headers: HttpHeaders) {
        val correlationId = MDC.get(CORRELATION_ID_MDC_KEY)
        if (!correlationId.isNullOrBlank()) headers.set(CORRELATION_ID_HEADER, correlationId)
    }

    private fun send(
        describe: String,
        request: RestClient.RequestHeadersSpec<*>,
        isValidSuccessBody: (ByteArray) -> Boolean,
    ): CallResult {
        val response: RawResponse =
            try {
                request.exchange { _, res -> RawResponse(res.statusCode.value(), res.body.readNBytes(MAX_BODY_BYTES)) }
            } catch (e: RestClientException) {
                return noResponse(describe, e)
            } catch (e: IOException) {
                return noResponse(describe, e)
            }

        val problemType = if (response.status in SUCCESS_RANGE) null else problemType(response.body)
        val outcome = classifier.classify(response.status, problemType)
        if (outcome == StepOutcome.Success && !isValidSuccessBody(response.body)) {
            log.warn(
                "accounts answered {} to {} with a body that is not the expected JSON; treating as transient",
                response.status,
                describe,
            )
            return CallResult(StepOutcome.TransientFailure, response.status, "unexpected response body")
        }
        if (outcome == StepOutcome.TransientFailure) {
            log.warn("transient failure calling accounts ({}): status {}", describe, response.status)
        }
        return CallResult(outcome, response.status, problemType ?: "http-${response.status}".takeIf { outcome != StepOutcome.Success })
    }

    private fun noResponse(
        describe: String,
        cause: Exception,
    ): CallResult {
        log.warn("no response from accounts ({}): {}", describe, cause.javaClass.simpleName)
        return CallResult(classifier.classify(null, null), null, "no response: ${cause.javaClass.simpleName}")
    }

    /** Last segment of the Problem Details `type`, or null when the body is not a Problem Details JSON. */
    private fun problemType(body: ByteArray): String? =
        try {
            AccountsResponseClassifier.problemSlug(mapper.readValue(body, AccountsProblemDTO::class.java).type)
        } catch (_: tools.jackson.core.JacksonException) {
            null
        }

    private inline fun <reified T : Any> parses(body: ByteArray): Boolean =
        try {
            mapper.readValue(body, T::class.java) != null
        } catch (_: tools.jackson.core.JacksonException) {
            false
        }

    private class RawResponse(
        val status: Int,
        val body: ByteArray,
    )

    private companion object {
        const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"
        const val CORRELATION_ID_HEADER = "X-Correlation-Id"
        const val ACCOUNT_NOT_FOUND = "account-not-found"
        const val MAX_BODY_BYTES = 64 * 1024
        val SUCCESS_RANGE = 200..299
        val log = LoggerFactory.getLogger(HttpAccountsGateway::class.java)
    }
}
