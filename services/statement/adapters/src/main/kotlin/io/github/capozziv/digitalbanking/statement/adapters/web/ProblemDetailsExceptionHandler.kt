package io.github.capozziv.digitalbanking.statement.adapters.web

import io.github.capozziv.digitalbanking.statement.application.AccountNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.beans.TypeMismatchException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.net.URI

/**
 * Maps the errors of the statement API to Problem Details (RFC 9457) with `type` = `/problems/<slug>`.
 * Extends ResponseEntityExceptionHandler, so the other framework errors (405, 415, ...) are Problem Details
 * too, and Spring Boot backs off its own handler (which would clash with this bean name).
 */
@RestControllerAdvice
class ProblemDetailsExceptionHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler
    fun handle(e: AccountNotFoundException) =
        problem(
            HttpStatus.NOT_FOUND,
            "account-not-found",
            "Account not found",
            "The statement has no account ${e.accountId}",
        )

    @ExceptionHandler
    fun handle(e: InvalidRequestException) = validationError(e.errors)

    @ExceptionHandler
    fun handle(e: InvalidCursorException) = validationError(listOf(InvalidField("cursor", "is invalid")))

    /** A path or query parameter that does not convert, such as an accountId that is not a UUID or a limit that is not a number. */
    override fun handleTypeMismatch(
        ex: TypeMismatchException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> = badRequest(listOf(InvalidField(ex.propertyName ?: "parameter", "is invalid")))

    /**
     * Anything not mapped above is a bug: logged here as ERROR, while the MDC still holds the correlation id,
     * and answered with a generic detail that does not leak the internal message.
     */
    @ExceptionHandler
    fun handle(e: Exception): ProblemDetail {
        log.error("unexpected error", e)
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Internal error", "An unexpected error occurred")
    }

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

    private companion object {
        val log = LoggerFactory.getLogger(ProblemDetailsExceptionHandler::class.java)
    }
}
