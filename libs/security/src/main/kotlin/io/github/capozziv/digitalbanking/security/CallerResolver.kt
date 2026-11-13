package io.github.capozziv.digitalbanking.security

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException
import java.util.UUID

/**
 * Turns a validated [Jwt] into a [Caller].
 *
 * Rules, in this order:
 * 1. the internal scope ([serviceScope], in `scope` as a space-separated string or in `scp` as a list) makes a
 *    [Caller.Service], whatever the `sub` (Cognito puts the client id there);
 * 2. otherwise the token is a user's: the [adminGroup] in the [groupsClaim] list makes a [Caller.Admin], anything
 *    else a [Caller.Customer], both with the `sub` as id.
 *
 * A user token whose `sub` is not a UUID throws [InvalidCallerException], which the resource server answers with 401.
 */
class CallerResolver(
    private val groupsClaim: String = DEFAULT_GROUPS_CLAIM,
    private val adminGroup: String = ADMIN_GROUP,
    private val serviceScope: String = SERVICE_SCOPE,
) {
    fun resolve(jwt: Jwt): Caller {
        if (serviceScope in scopes(jwt)) return Caller.Service
        val userId = userId(jwt.subject)
        val groups = jwt.getClaimAsStringList(groupsClaim).orEmpty()
        return if (adminGroup in groups) Caller.Admin(userId) else Caller.Customer(userId)
    }

    private fun scopes(jwt: Jwt): Set<String> {
        val fromScope = jwt.getClaimAsString("scope")?.split(' ').orEmpty()
        val fromScp = jwt.getClaimAsStringList("scp").orEmpty()
        return (fromScope + fromScp).toSet()
    }

    private fun userId(sub: String?): UUID =
        try {
            UUID.fromString(sub)
        } catch (_: IllegalArgumentException) {
            throw InvalidCallerException("the token subject is not a user id")
        } catch (_: NullPointerException) {
            throw InvalidCallerException("the token has no subject")
        }

    companion object {
        const val DEFAULT_GROUPS_CLAIM = "groups"
        const val ADMIN_GROUP = "admin"
        const val SERVICE_SCOPE = "digitalbanking/internal"
    }
}

/** A validated token that does not describe any [Caller]; becomes 401 `/problems/unauthorized`. */
class InvalidCallerException(
    message: String,
) : InvalidBearerTokenException(message)
