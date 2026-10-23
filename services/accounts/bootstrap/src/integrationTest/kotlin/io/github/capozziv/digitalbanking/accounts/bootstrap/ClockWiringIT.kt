package io.github.capozziv.digitalbanking.accounts.bootstrap

import io.github.capozziv.digitalbanking.accounts.bootstrap.web.ApiClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.bean.override.convention.TestBean
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/** The use cases and the outbox writer share the single Clock bean (replaced here by a fixed clock). */
class ClockWiringIT : AccountsIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @TestBean
    private lateinit var clock: Clock

    @Test
    fun `the entry createdAt and the outbox occurredAt come from the same Clock bean`() {
        val api = ApiClient(port)
        val opened =
            api.post(
                "/accounts",
                """{"customerId":"${UUID.randomUUID()}"}""",
                mapOf("Idempotency-Key" to UUID.randomUUID().toString()),
            )
        val accountId = opened.body!!.get("id").asString()

        val credit =
            api.post(
                "/accounts/$accountId/credits",
                """{"amount":100}""",
                mapOf("Idempotency-Key" to UUID.randomUUID().toString()),
            )

        assertThat(credit.status).isEqualTo(201)
        assertThat(Instant.parse(credit.body!!.get("createdAt").asString())).isEqualTo(FIXED)
        val occurredAt =
            ownerJdbc
                .sql("select occurred_at from outbox where aggregate_id = :id order by position")
                .param("id", accountId)
                .query(OffsetDateTime::class.java)
                .list()
                .map { it!!.toInstant() }
        assertThat(occurredAt).containsExactly(FIXED, FIXED)
    }

    companion object {
        private val FIXED: Instant = Instant.parse("2026-10-22T14:31:00.123456Z")

        @JvmStatic
        fun clock(): Clock = Clock.fixed(FIXED, ZoneOffset.UTC)
    }
}
