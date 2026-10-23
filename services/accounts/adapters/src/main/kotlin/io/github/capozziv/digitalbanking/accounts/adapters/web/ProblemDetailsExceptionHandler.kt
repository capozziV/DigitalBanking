package io.github.capozziv.digitalbanking.accounts.adapters.web

import io.github.capozziv.digitalbanking.accounts.application.AccountBusyException
import io.github.capozziv.digitalbanking.accounts.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.accounts.application.CustomerAlreadyHasAccountException
import io.github.capozziv.digitalbanking.accounts.application.IdempotencyKeyReusedException
import io.github.capozziv.digitalbanking.accounts.domain.BalanceOverflowException
import org.slf4j.LoggerFactory
import org.springframework.beans.TypeMismatchException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.ServletRequestBindingException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import tools.jackson.core.JacksonException
import java.net.URI

/**
 * Maps the errors of the accounts API to Problem Details (RFC 9457) with `type` = `/problems/<slug>`.
 * Extends ResponseEntityExceptionHandler, so the other framework errors (405, 415, ...) are Problem Details
 * too, and Spring Boot backs off its own handler (which would clash with this bean name).
 */
@RestControllerAdvice
class ProblemDetailsExceptionHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler
    fun handle(e: AccountNotFoundException) = problem(HttpStatus.NOT_FOUND, "account-not-found", "Account not found", e.message)

    @ExceptionHandler
    fun handle(e: AccountTypeNotFoundException) =
        problem(HttpStatus.NOT_FOUND, "account-type-not-found", "Account type not found", e.message)

    @ExceptionHandler
    fun handle(e: CustomerAlreadyHasAccountException) =
        problem(HttpStatus.CONFLICT, "customer-already-has-account", "Customer already has an account", e.message)

    @ExceptionHandler
    fun handle(e: IdempotencyKeyReusedException) =
        problem(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "idempotency-key-reused",
            "Idempotency key reused",
            "The Idempotency-Key was already used for another operation",
        )

    @ExceptionHandler
    fun handle(e: InsufficientAvailableBalanceException) =
        problem(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "insufficient-available-balance",
            "Insufficient available balance",
            "The available balance does not cover the debit",
        ).also { it.setProperty("availableBalance", e.availableBalance.cents) }

    @ExceptionHandler
    fun handle(e: AccountBusyException): ResponseEntity<ProblemDetail> {
        val body = problem(HttpStatus.SERVICE_UNAVAILABLE, "account-busy", "Account busy", "The account is busy; retry shortly")
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).header(HttpHeaders.RETRY_AFTER, "1").body(body)
    }

    /** A missing Idempotency-Key gets its own type; other binding errors keep Spring's Problem Details. */
    override fun handleServletRequestBindingException(
        ex: ServletRequestBindingException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        if (ex is MissingRequestHeaderException && ex.headerName.equals(IdempotencyKeyHeader.NAME, ignoreCase = true)) {
            return ResponseEntity
                .badRequest()
                .body(
                    problem(
                        HttpStatus.BAD_REQUEST,
                        "missing-idempotency-key",
                        "Missing Idempotency-Key",
                        "The Idempotency-Key header is required",
                    ),
                )
        }
        return super.handleServletRequestBindingException(ex, headers, status, request)
    }

    @ExceptionHandler
    fun handle(e: InvalidRequestException) = validationError(e.errors)

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> = badRequest(ex.bindingResult.fieldErrors.map { InvalidField(it.field, it.defaultMessage ?: "is invalid") })

    /** A path variable that does not convert, such as an account id that is not a UUID. */
    override fun handleTypeMismatch(
        ex: TypeMismatchException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> = badRequest(listOf(InvalidField(ex.propertyName ?: "path", "is invalid")))

    /** Malformed JSON, or a value of the wrong type such as an unknown account type or a fractional amount. */
    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        val field = ex.jsonPath()
        return badRequest(listOf(if (field != null) InvalidField(field, "is invalid") else InvalidField("body", "is not valid JSON")))
    }

    /**
     * Anything not mapped above is a bug: logged here as ERROR, while the MDC still holds the correlation id,
     * and answered with a generic detail that does not leak the internal message.
     */
    @ExceptionHandler
    fun handle(e: Exception): ProblemDetail {
        log.error("unexpected error", e)
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Internal error", "An unexpected error occurred")
    }

    /** Only a credit or debit can overflow the balance (the available balance saturates instead). */
    @ExceptionHandler
    fun handle(e: BalanceOverflowException) = validationError(listOf(InvalidField("amount", "would take the balance out of range")))

    private fun badRequest(errors: List<InvalidField>): ResponseEntity<Any> = ResponseEntity.badRequest().body(validationError(errors))

    private fun validationError(errors: List<InvalidField>) =
        problem(HttpStatus.BAD_REQUEST, "validation-error", "Validation error", "The request has invalid fields")
            .also { it.setProperty("errors", errors.map { error -> mapOf("field" to error.field, "message" to error.message) }) }

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

    /** Dotted path of the JSON property that failed to bind, or null for malformed JSON. */
    private fun HttpMessageNotReadableException.jsonPath(): String? =
        generateSequence<Throwable>(this) { it.cause }
            .filterIsInstance<JacksonException>()
            .firstOrNull()
            ?.path
            ?.mapNotNull { it.propertyName ?: it.index.takeIf { index -> index >= 0 }?.toString() }
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(".")

    private companion object {
        val log = LoggerFactory.getLogger(ProblemDetailsExceptionHandler::class.java)
    }
}
