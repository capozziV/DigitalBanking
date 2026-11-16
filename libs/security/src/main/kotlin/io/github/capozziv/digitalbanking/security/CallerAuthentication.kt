package io.github.capozziv.digitalbanking.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

/**
 * The caller of the current request, for controllers in `adapters/web`, which pass it to the use case:
 * ```
 * @GetMapping("/accounts/{id}")
 * fun get(@PathVariable id: UUID) = getAccount(id, currentCaller())
 * ```
 * Throws [AuthenticationCredentialsNotFoundException] (answered with 401 `/problems/unauthorized`) when the request
 * has no authenticated caller, which only happens on a route declared open.
 */
fun currentCaller(): Caller =
    (SecurityContextHolder.getContext().authentication as? CallerAuthenticationToken)?.caller
        ?: throw AuthenticationCredentialsNotFoundException("the request has no authenticated caller")

/** A validated JWT together with the [Caller] it describes; its only authority is the caller's role. */
class CallerAuthenticationToken(
    jwt: Jwt,
    val caller: Caller,
) : JwtAuthenticationToken(jwt, listOf(SimpleGrantedAuthority("ROLE_" + CallerRoles.of(caller))), jwt.subject.orEmpty())

/** The resource server's JWT converter: resolves the [Caller] with a [CallerResolver]. */
internal class CallerAuthenticationConverter(
    private val resolver: CallerResolver,
) : Converter<Jwt, AbstractAuthenticationToken> {
    override fun convert(source: Jwt): AbstractAuthenticationToken = CallerAuthenticationToken(source, resolver.resolve(source))
}
