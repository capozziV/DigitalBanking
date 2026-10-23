package io.github.capozziv.digitalbanking.outbox

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.util.UUID

class OutboxEnvelopeTest {
    private val jsonMapper = JsonMapper.builder().build()

    private val event =
        PendingEvent(
            position = 7,
            eventId = UUID.fromString("019a0b5e-0000-7000-8000-000000000001"),
            aggregateId = "acc-1",
            eventType = "LedgerEntryRecorded",
            eventVersion = 1,
            payloadJson = """{"amount": 3700, "kind": "CREDIT"}""",
            correlationId = "corr-1",
            occurredAt = Instant.parse("2026-10-22T14:31:00.123456Z"),
            attempts = 0,
        )

    @Test
    fun `envelope has exactly the spec fields with the payload as a json object`() {
        val json = jsonMapper.readTree(OutboxEnvelope.toJson(event))

        assertThat(json.propertyNames())
            .containsExactly("eventId", "eventType", "eventVersion", "occurredAt", "aggregateId", "correlationId", "payload")
        assertThat(json.path("eventId").asString()).isEqualTo("019a0b5e-0000-7000-8000-000000000001")
        assertThat(json.path("eventType").asString()).isEqualTo("LedgerEntryRecorded")
        assertThat(json.path("eventVersion").isInt).isTrue()
        assertThat(json.path("eventVersion").asInt()).isEqualTo(1)
        assertThat(json.path("occurredAt").asString()).isEqualTo("2026-10-22T14:31:00.123456Z")
        assertThat(json.path("aggregateId").asString()).isEqualTo("acc-1")
        assertThat(json.path("correlationId").asString()).isEqualTo("corr-1")
        assertThat(json.path("payload").isObject).isTrue()
        assertThat(json.path("payload")).isEqualTo(jsonMapper.readTree(event.payloadJson))
    }

    @Test
    fun `missing correlation id is written as null`() {
        val json = jsonMapper.readTree(OutboxEnvelope.toJson(event.copy(correlationId = null)))

        assertThat(json.has("correlationId")).isTrue()
        assertThat(json.path("correlationId").isNull).isTrue()
    }
}
