package io.github.capozziv.digitalbanking.notification.adapters.web

import io.github.capozziv.digitalbanking.notification.application.NotificationNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.beans.TypeMismatchException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import tools.jackson.core.JacksonException
import java.net.URI

/**
 * Maps the errors of the notification API to Problem Details (RFC 9457) with `type` = `/problems/<slug>`.
 * Extends ResponseEntityExceptionHandler, so the other framework errors (405, 415, ...) are Problem Details
 * too, and Spring Boot backs off its own handler (which would clash with this bean name).
 */
@RestControllerAdvice
class ProblemDetailsExceptionHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler
    fun handle(e: NotificationNotFoundException) =
        problem(
            HttpStatus.NOT_FOUND,
            "notification-not-found",
            "Notification not found",
            "The notification was not found for this customer",
        )

    @ExceptionHandler
    fun handle(e: InvalidRequestException) = validationError(e.errors)

    /** A required query parameter that is absent, such as `customerId`. */
    override fun handleMissingServletRequestParameter(
        ex: MissingServletRequestParameterException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> = badRequest(listOf(InvalidField(ex.parameterName, "is required")))

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> = badRequest(ex.bindingResult.fieldErrors.map { InvalidField(it.field, it.defaultMessage ?: "is invalid") })

    /** A query parameter that does not convert, such as a customerId that is not a UUID. */
    override fun handleTypeMismatch(
        ex: TypeMismatchException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> = badRequest(listOf(InvalidField(ex.propertyName ?: "parameter", "is invalid")))

    /** Malformed JSON, or a value of the wrong type such as a customerId that is not a UUID. */
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
