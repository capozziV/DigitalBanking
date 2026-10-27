package io.github.capozziv.digitalbanking.transfers.adapters.web

import io.github.capozziv.digitalbanking.outbox.OutboxEventEnvelope
import io.github.capozziv.digitalbanking.transfers.adapters.web.dto.RequestTransferDTO
import io.github.capozziv.digitalbanking.transfers.adapters.web.dto.TransferDTO
import io.github.capozziv.digitalbanking.transfers.application.usecase.GetTransferUseCase
import io.github.capozziv.digitalbanking.transfers.application.usecase.RequestTransferCommand
import io.github.capozziv.digitalbanking.transfers.application.usecase.RequestTransferOutcome
import io.github.capozziv.digitalbanking.transfers.application.usecase.RequestTransferUseCase
import io.github.capozziv.digitalbanking.transfers.domain.Money
import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import io.github.capozziv.digitalbanking.transfers.domain.TransferStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.slf4j.MDC
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/transfers")
class TransferController(
    private val requestTransfer: RequestTransferUseCase,
    private val getTransfer: GetTransferUseCase,
) {
    // The Idempotency-Key header is declared before the body: a missing key is reported (400
    // missing-idempotency-key) before the body is read and validated.

    @Operation(summary = "Request a transfer; the saga runs in the request")
    @ApiResponses(
        ApiResponse(
            responseCode = "201",
            description = "Transfer completed",
            headers = [Header(name = "Location", description = "/transfers/{id}")],
            content = [Content(mediaType = "application/json", schema = Schema(implementation = TransferDTO::class))],
        ),
        ApiResponse(
            responseCode = "202",
            description =
                "Transfer still pending (accounts did not answer in time); the saga goes on in the background, " +
                    "poll GET /transfers/{id}",
            headers = [Header(name = "Location", description = "/transfers/{id}")],
            content = [Content(mediaType = "application/json", schema = Schema(implementation = TransferDTO::class))],
        ),
        ApiResponse(
            responseCode = "200",
            description =
                "Retry with the same key: the transfer as it is now (any status but FAILED); or the first answer " +
                    "of a transfer that was compensated",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = TransferDTO::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description =
                "/problems/missing-idempotency-key: no Idempotency-Key header. " +
                    "/problems/validation-error: invalid header or body fields, listed in `errors`.",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "422",
            description =
                "/problems/insufficient-available-balance: the source account cannot cover the transfer (`transferId`; also on " +
                    "retries). /problems/transfer-failed: refused by accounts for another `reason` (`transferId`). " +
                    "/problems/same-account-transfer: source and destination are the same account. " +
                    "/problems/account-not-found: an account does not exist (`accountId`); nothing is stored. " +
                    "/problems/idempotency-key-reused: the key was used for another source, destination or amount.",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "503",
            description =
                "/problems/accounts-unavailable: accounts did not answer the account check; nothing is stored, " +
                    "retry after `Retry-After` seconds",
            headers = [Header(name = "Retry-After", description = "Seconds to wait before retrying (1)")],
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @PostMapping
    fun request(
        @Parameter(description = ApiDocs.IDEMPOTENCY_KEY)
        @RequestHeader(IdempotencyKeyHeader.NAME) idempotencyKey: String,
        @Valid @RequestBody request: RequestTransferDTO,
    ): ResponseEntity<TransferDTO> {
        val command =
            RequestTransferCommand(
                sourceAccountId = checkNotNull(request.sourceAccountId),
                destinationAccountId = checkNotNull(request.destinationAccountId),
                amount = Money(checkNotNull(request.amount)),
                description = request.normalizedDescription(),
                idempotencyKey = IdempotencyKeyHeader.require(idempotencyKey),
                correlationId = MDC.get(OutboxEventEnvelope.CORRELATION_ID_MDC_KEY),
            )
        return when (val outcome = requestTransfer.execute(command)) {
            is RequestTransferOutcome.Created -> created(outcome.transfer)
            is RequestTransferOutcome.Existing -> existing(outcome.transfer)
            RequestTransferOutcome.SameAccount -> throw SameAccountTransferException()
            is RequestTransferOutcome.AccountNotFound -> throw TransferAccountNotFoundException(outcome.accountId)
            RequestTransferOutcome.AccountsUnavailable -> throw AccountsUnavailableException()
        }
    }

    @Operation(summary = "Get a transfer with its current status")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "The transfer",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = TransferDTO::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "/problems/validation-error: the id is not a UUID",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "/problems/transfer-not-found",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @GetMapping("/{id}")
    fun get(
        @PathVariable id: UUID,
    ): TransferDTO = TransferDTO.from(getTransfer.execute(id) ?: throw TransferNotFoundException(id))

    /** First answer: 201 completed, 202 pending, 200 compensated, 422 failed. */
    private fun created(transfer: Transfer): ResponseEntity<TransferDTO> {
        val body = TransferDTO.from(transfer)
        val location = URI.create("/transfers/${transfer.id}")
        return when (transfer.status) {
            TransferStatus.COMPLETED -> ResponseEntity.created(location).body(body)
            TransferStatus.PENDING -> ResponseEntity.accepted().location(location).body(body)
            TransferStatus.COMPENSATED -> ResponseEntity.ok(body)
            TransferStatus.FAILED -> throw TransferFailedException(transfer)
        }
    }

    /** Repetition with the same key: 200 with the current state, or the same 422 for a failed transfer. */
    private fun existing(transfer: Transfer): ResponseEntity<TransferDTO> {
        if (transfer.status == TransferStatus.FAILED) throw TransferFailedException(transfer)
        return ResponseEntity.ok(TransferDTO.from(transfer))
    }
}
