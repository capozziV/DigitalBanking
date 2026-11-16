package io.github.capozziv.digitalbanking.testing.security

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Signed JWTs for integration tests, without an identity server.
 *
 * One RSA key per JVM; [jwtDecoder] trusts only that key and only the [ISSUER], with the default validators (so an
 * expired token or one from another issuer is rejected, as in production). Register the decoder in a Spring test
 * with `@Import(JwtTestSupport.DecoderConfiguration::class)`: it replaces the decoder Spring Boot builds from
 * `spring.security.oauth2.resourceserver.jwt.issuer-uri`, so no issuer has to be reachable.
 */
object JwtTestSupport {
    const val ISSUER = "http://issuer.digitalbanking.test"
    const val SERVICE_SCOPE = "digitalbanking/internal"
    const val ADMIN_GROUP = "admin"
    const val DEFAULT_GROUPS_CLAIM = "groups"

    private val rsaKey: RSAKey =
        KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair().let {
            RSAKey
                .Builder(it.public as RSAPublicKey)
                .privateKey(it.private as RSAPrivateKey)
                .keyID("digitalbanking-test")
                .build()
        }

    private val encoder = NimbusJwtEncoder(ImmutableJWKSet<SecurityContext>(JWKSet(rsaKey)))

    /** A decoder that accepts the tokens of this object: right signature, [ISSUER], not expired. */
    fun jwtDecoder(): JwtDecoder =
        NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build().apply {
            setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER))
        }

    /**
     * A signed token with [sub]; [groups] go to [groupsClaim] (omitted when empty) and [scopes] to `scope` as a
     * space-separated string (omitted when empty). A negative [expiresIn] gives an expired token.
     */
    fun token(
        sub: String,
        groups: List<String> = emptyList(),
        scopes: List<String> = emptyList(),
        expiresIn: Duration = Duration.ofMinutes(5),
        issuer: String = ISSUER,
        groupsClaim: String = DEFAULT_GROUPS_CLAIM,
    ): String {
        val now = Instant.now()
        val claims =
            JwtClaimsSet
                .builder()
                .issuer(issuer)
                .subject(sub)
                .issuedAt(minOf(now, now.plus(expiresIn)).minusSeconds(1))
                .expiresAt(now.plus(expiresIn))
                .apply { if (groups.isNotEmpty()) claim(groupsClaim, groups) }
                .apply { if (scopes.isNotEmpty()) claim("scope", scopes.joinToString(" ")) }
                .build()
        val header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(rsaKey.keyID).build()
        return encoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
    }

    /** A customer: user token without groups, `sub` = [customerId]. */
    fun customerToken(customerId: UUID): String = token(customerId.toString())

    /** An admin: user token with the `admin` group, `sub` = [userId]. */
    fun adminToken(userId: UUID): String = token(userId.toString(), groups = listOf(ADMIN_GROUP))

    /** A service: machine token with the internal scope; the `sub` is a client id, as Cognito issues it. */
    fun serviceToken(clientId: String = "test-service"): String = token(clientId, scopes = listOf(SERVICE_SCOPE))

    /** Registers [jwtDecoder] as the application's `JwtDecoder`. */
    @Configuration(proxyBeanMethods = false)
    class DecoderConfiguration {
        @Bean
        fun jwtDecoder(): JwtDecoder = JwtTestSupport.jwtDecoder()
    }
}
