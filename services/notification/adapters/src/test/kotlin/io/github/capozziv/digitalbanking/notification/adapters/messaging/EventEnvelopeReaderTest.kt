package io.github.capozziv.digitalbanking.notification.adapters.messaging

import io.github.capozziv.digitalbanking.notification.application.ReceivedEvent
import io.github.capozziv.digitalbanking.notification.domain.NotificationText
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID

class EventEnvelopeReaderTest {
    private val reader = EventEnvelopeReader()

    private val eventId = UUID.randomUUID()
    private val accountId = UUID.randomUUID()
    private val customerId = UUID.randomUUID()
    private val transferId = UUID.randomUUID()
    private val source = UUID.randomUUID()
    private val destination = UUID.randomUUID()

    @Test
    fun `AccountOpened is read from the accounts payload`() {
        val body = envelope("AccountOpened", 1, """{"accountId":"$accountId","customerId":"$customerId","type":"GOLD"}""")

        assertThat(reader.read(body)).isEqualTo(ReceivedEvent.AccountOpened(eventId, accountId, customerId, "GOLD"))
    }

    @Test
    fun `AccountTypeChanged version 1 has no overdraft limit`() {
        val body =
            envelope("AccountTypeChanged", 1, """{"accountId":"$accountId","previousType":"FREE","newType":"GOLD"}""")

        assertThat(reader.read(body)).isEqualTo(ReceivedEvent.AccountTypeChanged(eventId, accountId, "GOLD", null))
    }

    @Test
    fun `AccountTypeChanged version 2 carries the overdraft limit in cents`() {
        val body =
            envelope(
                "AccountTypeChanged",
                2,
                """{"accountId":"$accountId","previousType":"FREE","newType":"GOLD","overdraftLimit":150000}""",
            )

        assertThat(reader.read(body)).isEqualTo(ReceivedEvent.AccountTypeChanged(eventId, accountId, "GOLD", 150_000))
    }

    @Test
    fun `AccountTypeChanged version 2 without the overdraft limit is malformed`() {
        val body =
            envelope("AccountTypeChanged", 2, """{"accountId":"$accountId","previousType":"FREE","newType":"GOLD"}""")

        assertThatThrownBy { reader.read(body) }.isInstanceOf(MalformedEventException::class.java)
    }

    @Test
    fun `TransferCompleted is read from the transfers payload`() {
        val body = envelope("TransferCompleted", 1, transferPayload())

        assertThat(reader.read(body))
            .isEqualTo(ReceivedEvent.TransferCompleted(eventId, transferId, source, destination, 12_345))
    }

    @Test
    fun `TransferCompensated is read from the transfers payload`() {
        val body = envelope("TransferCompensated", 1, transferPayload(description = null))

        assertThat(reader.read(body))
            .isEqualTo(ReceivedEvent.TransferCompensated(eventId, transferId, source, destination, 12_345))
    }

    @ParameterizedTest
    @ValueSource(strings = ["insufficient-available-balance", "debit-not-confirmed"])
    fun `TransferFailed keeps the reason the transfers service writes`(reason: String) {
        val body = envelope("TransferFailed", 1, transferPayload(extra = ""","reason":"$reason""""))

        assertThat(reader.read(body))
            .isEqualTo(ReceivedEvent.TransferFailed(eventId, transferId, source, destination, 12_345, reason))
    }

    /** The reasons written by the transfers service must have their own text, not the generic fallback. */
    @ParameterizedTest
    @ValueSource(strings = ["insufficient-available-balance", "debit-not-confirmed"])
    fun `each reason the transfers service writes has a specific text`(reason: String) {
        val event = reader.read(envelope("TransferFailed", 1, transferPayload(extra = ""","reason":"$reason"""")))
        val fallback = NotificationText.transferFailed(12_345, destination, "some-unknown-reason").body

        val body = NotificationText.transferFailed(12_345, destination, (event as ReceivedEvent.TransferFailed).reason).body

        assertThat(body).isNotEqualTo(fallback)
    }

    @Test
    fun `LedgerEntryRecorded is unsupported`() {
        val body = envelope("LedgerEntryRecorded", 1, """{"entryId":"${UUID.randomUUID()}","amount":10}""")

        assertThat(reader.read(body)).isEqualTo(ReceivedEvent.Unsupported(eventId, "LedgerEntryRecorded"))
    }

    @Test
    fun `an unknown event type is unsupported`() {
        val body = envelope("SomethingNew", 7, """{}""")

        assertThat(reader.read(body)).isEqualTo(ReceivedEvent.Unsupported(eventId, "SomethingNew"))
    }

    @Test
    fun `a body that is not JSON is malformed`() {
        assertThatThrownBy { reader.read("not json") }.isInstanceOf(MalformedEventException::class.java)
    }

    @Test
    fun `an envelope without eventId is malformed`() {
        val body = """{"eventType":"AccountOpened","eventVersion":1,"payload":{}}"""

        assertThatThrownBy { reader.read(body) }.isInstanceOf(MalformedEventException::class.java)
    }

    @Test
    fun `an eventId that is not a UUID is malformed`() {
        val body = envelope("AccountOpened", 1, """{}""").replace(eventId.toString(), "nope")

        assertThatThrownBy { reader.read(body) }.isInstanceOf(MalformedEventException::class.java)
    }

    @Test
    fun `a payload without a required field is malformed`() {
        val body = envelope("AccountOpened", 1, """{"accountId":"$accountId","type":"GOLD"}""")

        assertThatThrownBy { reader.read(body) }.isInstanceOf(MalformedEventException::class.java)
    }

    @Test
    fun `an amount that is not an integer number of cents is malformed`() {
        val body =
            envelope(
                "TransferCompleted",
                1,
                transferPayload().replace("12345", "123.45"),
            )

        assertThatThrownBy { reader.read(body) }.isInstanceOf(MalformedEventException::class.java)
    }

    @Test
    fun `a supported event without payload is malformed`() {
        val body = """{"eventId":"$eventId","eventType":"TransferCompleted","eventVersion":1}"""

        assertThatThrownBy { reader.read(body) }.isInstanceOf(MalformedEventException::class.java)
    }

    private fun transferPayload(
        description: String? = "rent",
        extra: String = "",
    ) = """{"transferId":"$transferId","sourceAccountId":"$source","destinationAccountId":"$destination",""" +
        """"amount":12345,"description":${description?.let { "\"$it\"" } ?: "null"}$extra}"""

    private fun envelope(
        eventType: String,
        version: Int,
        payload: String,
    ) = """{"eventId":"$eventId","eventType":"$eventType","eventVersion":$version,""" +
        """"occurredAt":"2026-10-28T12:00:00Z","aggregateId":"agg","correlationId":"corr","payload":$payload}"""
}
