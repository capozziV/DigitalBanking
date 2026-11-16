package io.github.capozziv.digitalbanking.testing.security

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.JwtValidationException
import java.time.Duration
import java.util.UUID

class JwtTestSupportTest {
    private val decoder = JwtTestSupport.jwtDecoder()
    private val userId = UUID.fromString("00000000-0000-7000-8000-000000000001")

    @Test
    fun `a token carries the sub, the groups and the scopes and is accepted by the decoder`() {
        val jwt = decoder.decode(JwtTestSupport.token(userId.toString(), groups = listOf("admin"), scopes = listOf("a", "b")))

        assertThat(jwt.subject).isEqualTo(userId.toString())
        assertThat(jwt.getClaimAsStringList("groups")).containsExactly("admin")
        assertThat(jwt.getClaimAsString("scope")).isEqualTo("a b")
        assertThat(jwt.issuer.toString()).isEqualTo(JwtTestSupport.ISSUER)
    }

    @Test
    fun `the groups go to the claim the caller names`() {
        val jwt = decoder.decode(JwtTestSupport.token(userId.toString(), groups = listOf("admin"), groupsClaim = "cognito:groups"))

        assertThat(jwt.getClaimAsStringList("cognito:groups")).containsExactly("admin")
        assertThat(jwt.hasClaim("groups")).isFalse()
    }

    @Test
    fun `the shortcuts build customer, admin and service tokens`() {
        assertThat(decoder.decode(JwtTestSupport.customerToken(userId)).getClaimAsStringList("groups")).isNull()
        assertThat(decoder.decode(JwtTestSupport.adminToken(userId)).getClaimAsStringList("groups")).containsExactly("admin")
        assertThat(decoder.decode(JwtTestSupport.serviceToken()).getClaimAsString("scope")).isEqualTo("digitalbanking/internal")
    }

    @Test
    fun `an expired token is rejected by the decoder`() {
        val expired = JwtTestSupport.token(userId.toString(), expiresIn = Duration.ofMinutes(-5))

        assertThatThrownBy { decoder.decode(expired) }.isInstanceOf(JwtValidationException::class.java)
    }

    @Test
    fun `a token from another issuer is rejected by the decoder`() {
        val foreign = JwtTestSupport.token(userId.toString(), issuer = "http://other-issuer.test")

        assertThatThrownBy { decoder.decode(foreign) }.isInstanceOf(JwtValidationException::class.java)
    }
}
