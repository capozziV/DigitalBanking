package io.github.capozziv.digitalbanking.security

import org.apache.catalina.connector.ClientAbortException
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.async.AsyncRequestNotUsableException

/**
 * Security exceptions thrown inside a controller (for example [currentCaller] on an open route) become 401/403
 * Problem Details instead of reaching the service's generic 500 handler. Runs ahead of the service's own advice.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class SecurityExceptionHandling {
    @ExceptionHandler
    fun handle(e: AuthenticationException): ResponseEntity<ProblemDetail> =
        ResponseEntity.status(401).header(HttpHeaders.WWW_AUTHENTICATE, "Bearer").body(SecurityProblems.unauthorized())

    @ExceptionHandler
    fun handle(e: AccessDeniedException): ResponseEntity<ProblemDetail> = ResponseEntity.status(403).body(SecurityProblems.forbidden())
}

/**
 * A client that went away mid-response ([AsyncRequestNotUsableException], Tomcat's [ClientAbortException], both a
 * "broken pipe") is not a server error: logged at DEBUG, no response written, and the service's generic 500 handler
 * (with its ERROR log) never sees it.
 */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ClientDisconnectHandling {
    @ExceptionHandler(AsyncRequestNotUsableException::class, ClientAbortException::class)
    fun handle(e: Exception) {
        log.debug("client disconnected before the response was written: {}", e.javaClass.simpleName)
    }

    private companion object {
        val log = LoggerFactory.getLogger(ClientDisconnectHandling::class.java)
    }
}
