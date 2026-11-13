package io.github.capozziv.digitalbanking.security

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import java.util.UUID

class CallerResolverTest {
    private val userId = UUID.fromString("00000000-0000-7000-8000-000000000001")
    private val resolver = CallerResolver()

    @Test
    fun `a user token without the admin group is a customer whose id is the sub`() {
        val caller = resolver.resolve(jwt(sub = userId.toString(), claims = mapOf("groups" to listOf("other"))))

        assertThat(caller).isEqualTo(Caller.Customer(userId))
    }

    @Test
    fun `a user token without any groups claim is a customer`() {
        assertThat(resolver.resolve(jwt(sub = userId.toString()))).isEqualTo(Caller.Customer(userId))
    }

    @Test
    fun `a user token with the admin group is an admin`() {
        val caller = resolver.resolve(jwt(sub = userId.toString(), claims = mapOf("groups" to listOf("other", "admin"))))

        assertThat(caller).isEqualTo(Caller.Admin(userId))
    }

    @Test
    fun `a token with the internal scope is a service, even when its sub is a client id`() {
        val caller = resolver.resolve(jwt(sub = "transfers-client", claims = mapOf("scope" to "openid digitalbanking/internal")))

        assertThat(caller).isEqualTo(Caller.Service)
    }

    @Test
    fun `the internal scope is also read from a list of scopes`() {
        val caller = resolver.resolve(jwt(sub = "transfers-client", claims = mapOf("scp" to listOf("digitalbanking/internal"))))

        assertThat(caller).isEqualTo(Caller.Service)
    }

    @Test
    fun `a scope that only contains the internal one as a substring is not a service`() {
        val caller = resolver.resolve(jwt(sub = userId.toString(), claims = mapOf("scope" to "digitalbanking/internal-read")))

        assertThat(caller).isEqualTo(Caller.Customer(userId))
    }

    @Test
    fun `a user token whose sub is not a UUID is rejected as an invalid token`() {
        assertThatThrownBy { resolver.resolve(jwt(sub = "not-a-uuid")) }
            .isInstanceOf(InvalidCallerException::class.java)
            .hasMessageNotContaining("not-a-uuid")
    }

    @Test
    fun `the groups are read from the configured claim`() {
        val cognito = CallerResolver(groupsClaim = "cognito:groups")

        assertThat(cognito.resolve(jwt(sub = userId.toString(), claims = mapOf("cognito:groups" to listOf("admin")))))
            .isEqualTo(Caller.Admin(userId))
        assertThat(cognito.resolve(jwt(sub = userId.toString(), claims = mapOf("groups" to listOf("admin")))))
            .isEqualTo(Caller.Customer(userId))
    }

    private fun jwt(
        sub: String,
        claims: Map<String, Any> = emptyMap(),
    ): Jwt =
        Jwt
            .withTokenValue("token")
            .header("alg", "RS256")
            .subject(sub)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .claims { it.putAll(claims) }
            .build()
}
