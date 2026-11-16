package io.github.capozziv.digitalbanking.transfers.adapters.web

import io.github.capozziv.digitalbanking.security.Caller
import io.github.capozziv.digitalbanking.security.currentCaller
import io.github.capozziv.digitalbanking.transfers.adapters.web.dto.CompensateManuallyDTO
import io.github.capozziv.digitalbanking.transfers.adapters.web.dto.ManualCompensationDTO
import io.github.capozziv.digitalbanking.transfers.adapters.web.dto.StuckTransferDTO
import io.github.capozziv.digitalbanking.transfers.adapters.web.dto.TransferDTO
import io.github.capozziv.digitalbanking.transfers.application.usecase.CompensateManuallyOutcome
import io.github.capozziv.digitalbanking.transfers.application.usecase.CompensateTransferManuallyUseCase
import io.github.capozziv.digitalbanking.transfers.application.usecase.ListStuckTransfersUseCase
import io.github.capozziv.digitalbanking.transfers.application.usecase.RetryTransferOutcome
import io.github.capozziv.digitalbanking.transfers.application.usecase.RetryTransferUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.validation.Valid
import org.springframework.http.ProblemDetail
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * The admin's way out of a transfer stuck in `COMPENSATE` (the compensation credit refused by `accounts`, a bug the
 * saga retries forever): list them, retry the compensation now, or compensate by hand with a reason. Admins only: the
 * security library guards every route under `/admin`, [TransfersSecurityConfiguration] lists the routes, and [requireAdmin] refuses
 * any other caller that got this far.
 */
@RestController
@RequestMapping("/admin/transfers")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
class TransferAdminController(
    private val listStuckTransfers: ListStuckTransfersUseCase,
    private val retryTransfer: RetryTransferUseCase,
    private val compensateManually: CompensateTransferManuallyUseCase,
) {
    @Operation(summary = "List the transfers stuck in COMPENSATE: their last compensation credit was refused (admin)")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Oldest first, at most $MAX_STUCK; empty when none is stuck",
            content = [
                Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = StuckTransferDTO::class))),
            ],
        ),
        ApiResponse(
            responseCode = "403",
            description = "/problems/forbidden: the caller is not an admin",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @GetMapping("/stuck")
    fun stuck(): List<StuckTransferDTO> {
        requireAdmin()
        return listStuckTransfers.execute(MAX_STUCK).map(StuckTransferDTO::from)
    }

    @Operation(
        summary = "Run the saga of a pending transfer now, in the request (admin)",
        description =
            "Same run as the scheduler's, under the transfer's correlation id: a stuck compensation is tried again " +
                "right away instead of after the longest backoff. Safe to repeat: every call to accounts keeps its " +
                "idempotency key.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "The transfer after the run: COMPENSATED (or another status) when it ended, PENDING when refused again",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = TransferDTO::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "/problems/validation-error: the id is not a UUID",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "403",
            description = "/problems/forbidden: the caller is not an admin",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "/problems/transfer-not-found: no such transfer",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "409",
            description =
                "/problems/transfer-not-pending: the transfer already ended (`status`). " +
                    "/problems/transfer-busy: a saga run holds the transfer now; retry in a few seconds.",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @PostMapping("/{id}/retry")
    fun retry(
        @PathVariable id: UUID,
    ): TransferDTO {
        requireAdmin()
        return when (val outcome = retryTransfer.execute(id)) {
            is RetryTransferOutcome.Ran -> TransferDTO.from(outcome.transfer)
            RetryTransferOutcome.NotFound -> throw TransferNotFoundException(id)
            is RetryTransferOutcome.NotPending -> throw TransferNotPendingException(outcome.transfer)
            RetryTransferOutcome.Busy -> throw TransferBusyException(id)
        }
    }

    @Operation(
        summary = "Compensate by hand a transfer stuck in COMPENSATE (admin)",
        description =
            "For when accounts keeps refusing the compensation credit: the admin made the source account whole " +
                "outside the saga and says how in `reason`. The transfer ends COMPENSATED without calling accounts, " +
                "the TransferCompensated event is published and the daily limit reservation is released. Who, when " +
                "and why are recorded. Repeating it answers the first resolution and changes nothing.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "The transfer, now COMPENSATED, with who resolved it, when and why",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = ManualCompensationDTO::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "/problems/validation-error: `reason` missing, blank or over 500 characters, or the id is not a UUID",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "403",
            description = "/problems/forbidden: the caller is not an admin",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "/problems/transfer-not-found: no such transfer",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "409",
            description =
                "/problems/transfer-not-compensating: the transfer is not in COMPENSATE (`step`). " +
                    "/problems/transfer-busy: a saga run holds the transfer now; retry in a few seconds.",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @PostMapping("/{id}/manual-compensation")
    fun compensate(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CompensateManuallyDTO,
    ): ManualCompensationDTO {
        val admin = requireAdmin()
        val reason = checkNotNull(request.reason).trim()
        return when (val outcome = compensateManually.execute(id, admin.userId, reason)) {
            is CompensateManuallyOutcome.Compensated -> ManualCompensationDTO.from(outcome.transfer, outcome.resolution)
            CompensateManuallyOutcome.NotFound -> throw TransferNotFoundException(id)
            is CompensateManuallyOutcome.NotCompensating -> throw TransferNotCompensatingException(outcome.transfer)
            CompensateManuallyOutcome.Busy -> throw TransferBusyException(id)
        }
    }

    private fun requireAdmin(): Caller.Admin = currentCaller() as? Caller.Admin ?: throw AccessDeniedException("admins only")

    private companion object {
        const val MAX_STUCK = 100
    }
}
