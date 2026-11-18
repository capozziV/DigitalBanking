package io.github.capozziv.digitalbanking.transfers.adapters.web

import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI
import java.util.UUID

/** The transfer already ended, so there is no saga to run. */
class TransferNotPendingException(
    val transfer: Transfer,
) : RuntimeException("transfer ${transfer.id} is ${transfer.status}, not pending")

/** Only a transfer in `COMPENSATE` can be compensated by hand. */
class TransferNotCompensatingException(
    val transfer: Transfer,
) : RuntimeException("transfer ${transfer.id} is in step ${transfer.step.name}, not COMPENSATE")

/** A saga run holds the transfer's lease right now. */
class TransferBusyException(
    val id: UUID,
) : RuntimeException("transfer $id is being processed by a saga run; retry in a few seconds")

/** `accounts` has no entry under the key on the source account: nothing proves the value went back. */
class CompensationCreditNotFoundException(
    val creditIdempotencyKey: String,
) : RuntimeException("accounts has no entry under the key on the source account")

/** The entry under the key is not a credit of the transfer's amount on the source account. */
class CompensationCreditMismatchException(
    reason: String,
) : RuntimeException("the entry is not the compensation credit: $reason")

/** The credit under the key already compensated another transfer by hand. */
class CompensationCreditAlreadyUsedException(
    val creditIdempotencyKey: String,
) : RuntimeException("the credit already compensated another transfer")

/**
 * Problem Details of the errors that only [TransferAdminController] raises. Ahead of [ProblemDetailsExceptionHandler]
 * (whose catch-all would answer them with 500); everything else, such as 404 and validation, falls through to it.
 */
@RestControllerAdvice(assignableTypes = [TransferAdminController::class])
@Order(Ordered.HIGHEST_PRECEDENCE)
class TransferAdminExceptionHandler {
    @ExceptionHandler
    fun handle(e: TransferNotPendingException) =
        problem(HttpStatus.CONFLICT, "transfer-not-pending", "Transfer not pending", e.message)
            .also { it.setProperty("transferStatus", e.transfer.status.name) }

    @ExceptionHandler
    fun handle(e: TransferNotCompensatingException) =
        problem(HttpStatus.CONFLICT, "transfer-not-compensating", "Transfer not compensating", e.message)
            .also { it.setProperty("step", e.transfer.step.name) }

    @ExceptionHandler
    fun handle(e: CompensationCreditNotFoundException) =
        problem(HttpStatus.UNPROCESSABLE_CONTENT, "compensation-credit-not-found", "Compensation credit not found", e.message)
            .also { it.setProperty("creditIdempotencyKey", e.creditIdempotencyKey) }

    @ExceptionHandler
    fun handle(e: CompensationCreditMismatchException) =
        problem(HttpStatus.UNPROCESSABLE_CONTENT, "compensation-credit-mismatch", "Not the compensation credit", e.message)

    @ExceptionHandler
    fun handle(e: CompensationCreditAlreadyUsedException) =
        problem(HttpStatus.CONFLICT, "compensation-credit-already-used", "Compensation credit already used", e.message)
            .also { it.setProperty("creditIdempotencyKey", e.creditIdempotencyKey) }

    @ExceptionHandler
    fun handle(e: TransferBusyException) = problem(HttpStatus.CONFLICT, "transfer-busy", "Transfer busy", e.message)

    /** Reached only if the route rules let a non-admin through; answered like the security library's 403. */
    @ExceptionHandler
    fun handle(e: AccessDeniedException) = problem(HttpStatus.FORBIDDEN, "forbidden", "Forbidden", "Access denied")

    private fun problem(
        status: HttpStatus,
        slug: String,
        title: String,
        detail: String?,
    ): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail).apply {
            type = URI.create("/problems/$slug")
            this.title = title
        }
}
