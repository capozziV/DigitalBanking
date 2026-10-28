package io.github.capozziv.digitalbanking.transfers.bootstrap.persistence

import io.github.capozziv.digitalbanking.outbox.OutboxEventEnvelope
import io.github.capozziv.digitalbanking.transfers.application.port.EventOutbox
import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import io.github.capozziv.digitalbanking.transfers.domain.Money
import io.github.capozziv.digitalbanking.transfers.domain.TransferEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import tools.jackson.databind.ObjectMapper
import java.util.UUID

class OutboxEventAppenderIntegrationTest : TransfersIntegrationTest() {
    @Autowired
    private lateinit var eventOutbox: EventOutbox

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val transferId = UUID.randomUUID()
    private val source = UUID.randomUUID()
    private val destination = UUID.randomUUID()

    @BeforeEach
    fun cleanOutbox() {
        ownerJdbc.sql("delete from outbox").update()
    }

    @Test
    fun `TransferCompleted is written with the transfer as aggregate and the spec payload`() {
        eventOutbox.append(TransferEvent.TransferCompleted(transferId, source, destination, Money(500), "rent"))

        val row = onlyRow()
        assertThat(row.eventType).isEqualTo("TransferCompleted")
        assertThat(row.eventVersion).isEqualTo(1)
        assertThat(row.aggregateId).isEqualTo(transferId.toString())
        assertThat(objectMapper.readTree(row.payload)).isEqualTo(
            objectMapper.readTree(
                """{"transferId":"$transferId","sourceAccountId":"$source","destinationAccountId":"$destination","amount":500,"description":"rent"}""",
            ),
        )
    }

    @Test
    fun `TransferFailed carries the reason and a null description`() {
        eventOutbox.append(TransferEvent.TransferFailed(transferId, source, destination, Money(7), null, "insufficient-available-balance"))

        val row = onlyRow()
        assertThat(row.eventType).isEqualTo("TransferFailed")
        assertThat(objectMapper.readTree(row.payload)).isEqualTo(
            objectMapper.readTree(
                """{"transferId":"$transferId","sourceAccountId":"$source","destinationAccountId":"$destination","amount":7,"description":null,"reason":"insufficient-available-balance"}""",
            ),
        )
    }

    @Test
    fun `TransferCompensated is written with the spec payload`() {
        eventOutbox.append(TransferEvent.TransferCompensated(transferId, source, destination, Money(9), "x"))

        val row = onlyRow()
        assertThat(row.eventType).isEqualTo("TransferCompensated")
        assertThat(row.aggregateId).isEqualTo(transferId.toString())
        assertThat(objectMapper.readTree(row.payload).get("amount").asLong()).isEqualTo(9L)
    }

    @Test
    fun `the correlation id in the MDC goes to the correlation_id column`() {
        MDC.put(OutboxEventEnvelope.CORRELATION_ID_MDC_KEY, "corr-outbox-1")
        try {
            eventOutbox.append(TransferEvent.TransferCompleted(transferId, source, destination, Money(1), null))
        } finally {
            MDC.remove(OutboxEventEnvelope.CORRELATION_ID_MDC_KEY)
        }

        val correlationId = ownerJdbc.sql("select correlation_id from outbox").query(String::class.java).single()
        assertThat(correlationId).isEqualTo("corr-outbox-1")
    }

    private data class OutboxRow(
        val eventType: String,
        val eventVersion: Int,
        val aggregateId: String,
        val payload: String,
    )

    private fun onlyRow(): OutboxRow =
        ownerJdbc
            .sql("select event_type, event_version, aggregate_id, payload::text as payload from outbox")
            .query { rs, _ ->
                OutboxRow(rs.getString("event_type"), rs.getInt("event_version"), rs.getString("aggregate_id"), rs.getString("payload"))
            }.single()
}
