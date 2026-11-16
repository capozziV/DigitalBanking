package io.github.capozziv.digitalbanking.security

import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import org.apache.catalina.connector.ClientAbortException
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.async.AsyncRequestNotUsableException

/** A service in miniature: its own route rules, a controller that reads the caller and a catch-all 500 handler. */
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(JwtTestSupport.DecoderConfiguration::class, SecurityTestController::class, CatchAllExceptionHandler::class)
class SecurityTestApplication {
    @Bean
    fun testSecurityRules() =
        SecurityRules {
            authorize(HttpMethod.GET, "/caller", hasAnyRole(CallerRoles.CUSTOMER, CallerRoles.ADMIN, CallerRoles.SERVICE))
            authorize(HttpMethod.GET, "/customer-only", hasAnyRole(CallerRoles.CUSTOMER, CallerRoles.ADMIN))
            authorize("/public/**", permitAll)
        }
}

@RestController
class SecurityTestController {
    @GetMapping("/caller", "/public/caller")
    fun caller(): String =
        when (val caller = currentCaller()) {
            is Caller.Customer -> "customer ${caller.customerId}"
            is Caller.Admin -> "admin ${caller.userId}"
            Caller.Service -> "service"
        }

    @GetMapping("/customer-only", "/admin/ping", "/internal/ping", "/not-declared")
    fun ok() = "ok"

    @GetMapping("/public/disconnect-async")
    fun disconnectAsync(): String = throw AsyncRequestNotUsableException("ServletOutputStream failed to write: Broken pipe")

    @GetMapping("/public/disconnect-tomcat")
    fun disconnectTomcat(): String = throw ClientAbortException("Broken pipe")

    @GetMapping("/public/boom")
    fun boom(): String = error("boom")
}

/** Like the services' ProblemDetailsExceptionHandler: anything unexpected is a 500 logged as ERROR. */
@RestControllerAdvice
class CatchAllExceptionHandler {
    @ExceptionHandler
    fun handle(e: Exception): ProblemDetail {
        LoggerFactory.getLogger(CatchAllExceptionHandler::class.java).error("unexpected error", e)
        return ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
