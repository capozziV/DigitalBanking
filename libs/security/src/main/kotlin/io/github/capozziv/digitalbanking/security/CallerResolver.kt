package io.github.capozziv.digitalbanking.security

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException
import java.util.UUID

/**
 * Turns a validated [Jwt] into a [Caller].
 *
 * Rules, in this order:
 * 1. the internal scope ([serviceScope], in `scope` as a space-separated string or in `scp` as a list) makes a
 *    [Caller.Service] only on a machine token: its `sub` is not a UUID (Cognito puts the client id there) and it has
 *    no [groupsClaim]. A user token carrying the internal scope throws [InvalidCallerException] (401): it never
 *    becomes a service, so a misconfigured identity provider cannot open the internal routes to users;
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
        if (serviceScope in scopes(jwt)) {
            if (isUserToken(jwt)) throw InvalidCallerException("a user token cannot carry the internal scope")
            return Caller.Service
        }
        val userId = userId(jwt.subject)
        val groups = jwt.getClaimAsStringList(groupsClaim).orEmpty()
        return if (adminGroup in groups) Caller.Admin(userId) else Caller.Customer(userId)
    }

    private fun scopes(jwt: Jwt): Set<String> {
        val fromScope = jwt.getClaimAsString("scope")?.split(' ').orEmpty()
        val fromScp = jwt.getClaimAsStringList("scp").orEmpty()
        return (fromScope + fromScp).toSet()
    }

    /** A user's token: its `sub` is a UUID (a user id) or it has the [groupsClaim], even empty. */
    private fun isUserToken(jwt: Jwt): Boolean = jwt.hasClaim(groupsClaim) || isUuid(jwt.subject)

    private fun isUuid(sub: String?): Boolean =
        sub != null &&
            try {
                UUID.fromString(sub)
                true
            } catch (_: IllegalArgumentException) {
                false
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
