package io.github.capozziv.digitalbanking.statement.adapters.messaging

import io.github.capozziv.digitalbanking.statement.domain.EntryKind
import io.github.capozziv.digitalbanking.statement.domain.StatementEntry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class EventEnvelopeReaderTest {
    private val reader = EventEnvelopeReader()

    private val eventId = UUID.randomUUID()
    private val entryId = UUID.randomUUID()
    private val accountId = UUID.randomUUID()
    private val customerId = UUID.randomUUID()
    private val createdAt = Instant.parse("2026-11-10T12:30:45.123456Z")

    @Test
    fun `LedgerEntryRecorded is read from the accounts payload`() {
        val body = envelope("LedgerEntryRecorded", 1, ledgerPayload())

        assertThat(reader.read(body)).isEqualTo(
            ReceivedEvent.LedgerEntryRecorded(
                StatementEntry(
                    entryId = entryId,
                    accountId = accountId,
                    sequence = 7,
                    kind = EntryKind.DEBIT,
                    amountCents = 2_500,
                    balanceAfterCents = -1_000,
                    description = "Coffee",
                    createdAt = createdAt,
                ),
            ),
        )
    }

    @Test
    fun `a ledger entry without description has a null description`() {
        val body = envelope("LedgerEntryRecorded", 1, ledgerPayload(description = "null"))

        val event = reader.read(body) as ReceivedEvent.LedgerEntryRecorded

        assertThat(event.entry.description).isNull()
    }

    @Test
    fun `AccountOpened is read from the accounts payload`() {
        val body = envelope("AccountOpened", 1, """{"accountId":"$accountId","customerId":"$customerId","type":"GOLD"}""")

        assertThat(reader.read(body)).isEqualTo(ReceivedEvent.AccountOpened(accountId, customerId))
    }

    @Test
    fun `AccountTypeChanged is ignored`() {
        val body = envelope("AccountTypeChanged", 1, """{"accountId":"$accountId","newType":"GOLD"}""")

        assertThat(reader.read(body)).isEqualTo(ReceivedEvent.Ignored("AccountTypeChanged", 1))
    }

    @Test
    fun `an unsupported version of a known event is ignored`() {
        assertThat(reader.read(envelope("LedgerEntryRecorded", 2, ledgerPayload())))
            .isEqualTo(ReceivedEvent.Ignored("LedgerEntryRecorded", 2))
        assertThat(reader.read(envelope("AccountOpened", 2, "{}")))
            .isEqualTo(ReceivedEvent.Ignored("AccountOpened", 2))
    }

    @Test
    fun `a ledger entry without sequence is malformed`() {
        val body = envelope("LedgerEntryRecorded", 1, ledgerPayload().replace(""""sequence":7,""", ""))

        assertThatThrownBy { reader.read(body) }.isInstanceOf(MalformedEventException::class.java)
    }

    @Test
    fun `a ledger entry that breaks a domain rule is malformed`() {
        val body = envelope("LedgerEntryRecorded", 1, ledgerPayload(amount = 0))

        assertThatThrownBy { reader.read(body) }.isInstanceOf(MalformedEventException::class.java)
    }

    @Test
    fun `an unknown entry kind is malformed`() {
        val body = envelope("LedgerEntryRecorded", 1, ledgerPayload(kind = "REFUND"))

        assertThatThrownBy { reader.read(body) }.isInstanceOf(MalformedEventException::class.java)
    }

    @Test
    fun `an amount that is not an integer is malformed`() {
        val body = envelope("LedgerEntryRecorded", 1, ledgerPayload(amount = "12.5"))

        assertThatThrownBy { reader.read(body) }.isInstanceOf(MalformedEventException::class.java)
    }

    @Test
    fun `a body that is not a JSON object is malformed`() {
        assertThatThrownBy { reader.read("not json") }.isInstanceOf(MalformedEventException::class.java)
        assertThatThrownBy { reader.read("[1]") }.isInstanceOf(MalformedEventException::class.java)
    }

    @Test
    fun `an envelope without event type is malformed`() {
        assertThatThrownBy { reader.read("""{"eventVersion":1,"payload":{}}""") }
            .isInstanceOf(MalformedEventException::class.java)
    }

    private fun ledgerPayload(
        kind: String = "DEBIT",
        amount: Any = 2_500,
        description: String = "\"Coffee\"",
    ) = """{"entryId":"$entryId","accountId":"$accountId","sequence":7,"kind":"$kind","amount":$amount,""" +
        """"balanceAfter":-1000,"description":$description,"idempotencyKey":"key-1","createdAt":"$createdAt"}"""

    private fun envelope(
        eventType: String,
        version: Int,
        payload: String,
    ) = """{"eventId":"$eventId","eventType":"$eventType","eventVersion":$version,"occurredAt":"$createdAt",""" +
        """"aggregateId":"$accountId","correlationId":"c-1","payload":$payload}"""
}
