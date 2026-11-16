package io.github.capozziv.digitalbanking.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import tools.jackson.databind.json.JsonMapper
import java.net.URI

/** The two Problem Details of this library, in the services' format (`type` `/problems/<slug>`, `title`, `status`). */
internal object SecurityProblems {
    fun unauthorized(): ProblemDetail = problem(HttpStatus.UNAUTHORIZED, "unauthorized", "Unauthorized", "A valid bearer token is required")

    fun forbidden(): ProblemDetail =
        problem(HttpStatus.FORBIDDEN, "forbidden", "Forbidden", "The caller is not allowed to perform this operation")

    private fun problem(
        status: HttpStatus,
        slug: String,
        title: String,
        detail: String,
    ) = ProblemDetail.forStatusAndDetail(status, detail).apply {
        type = URI.create("/problems/$slug")
        this.title = title
    }

    private val mapper = JsonMapper.builder().build()

    /** Writes [problem] as `application/problem+json`, with `instance` set to the request path. */
    fun write(
        problem: ProblemDetail,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val body =
            linkedMapOf(
                "type" to problem.type.toString(),
                "title" to problem.title,
                "status" to problem.status,
                "detail" to problem.detail,
                "instance" to request.requestURI,
            )
        response.status = problem.status
        response.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        mapper.writeValue(response.outputStream, body)
    }
}

/** 401 `/problems/unauthorized`, keeping the `WWW-Authenticate: Bearer ...` challenge of the resource server. */
internal class ProblemDetailsAuthenticationEntryPoint : AuthenticationEntryPoint {
    private val bearer = BearerTokenAuthenticationEntryPoint()

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        log.debug("unauthenticated request to {}: {}", request.requestURI, authException.javaClass.simpleName)
        bearer.commence(request, response, authException)
        SecurityProblems.write(SecurityProblems.unauthorized(), request, response)
    }

    private companion object {
        val log = LoggerFactory.getLogger(ProblemDetailsAuthenticationEntryPoint::class.java)
    }
}

/** 403 `/problems/forbidden` for an authenticated caller without the role the route asks for. */
internal class ProblemDetailsAccessDeniedHandler : AccessDeniedHandler {
    private val bearer = BearerTokenAccessDeniedHandler()

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        log.debug("forbidden request to {} {}", request.method, request.requestURI)
        bearer.handle(request, response, accessDeniedException)
        SecurityProblems.write(SecurityProblems.forbidden(), request, response)
    }

    private companion object {
        val log = LoggerFactory.getLogger(ProblemDetailsAccessDeniedHandler::class.java)
    }
}
