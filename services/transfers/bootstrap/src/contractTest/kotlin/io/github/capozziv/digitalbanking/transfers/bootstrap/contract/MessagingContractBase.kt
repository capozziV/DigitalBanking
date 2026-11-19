package io.github.capozziv.digitalbanking.transfers.bootstrap.contract

import io.github.capozziv.digitalbanking.outbox.OutboxEventEnvelope
import io.github.capozziv.digitalbanking.testing.outboxcontract.OutboxMessageVerifier
import io.github.capozziv.digitalbanking.testing.outboxcontract.OutboxMessagingContractConfiguration
import io.github.capozziv.digitalbanking.transfers.adapters.persistence.OutboxEventAppender
import io.github.capozziv.digitalbanking.transfers.adapters.persistence.OutboxWriterConfiguration
import io.github.capozziv.digitalbanking.transfers.application.port.EventOutbox
import io.github.capozziv.digitalbanking.transfers.domain.Money
import io.github.capozziv.digitalbanking.transfers.domain.StepOutcome
import io.github.capozziv.digitalbanking.transfers.domain.TransferEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import java.util.UUID

/**
 * Base of the message contracts of transfers (contracts/messaging). Each trigger appends one event through the
 * service's outbox mapper (OutboxEventAppender, with the JSON mapper Boot builds from application.yml) into a real
 * outbox table, with a correlation id in the MDC as the saga leaves it; OutboxMessageVerifier reads the row back as
 * the envelope the relay publishes to the topic of `transfers.aws.topic-arn`. Needs Docker (PostgreSQL).
 */
@SpringBootTest(
    classes = [OutboxMessagingContractConfiguration::class, OutboxWriterConfiguration::class, OutboxEventAppender::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = ["digitalbanking.contract.messaging.topic-arn=\${transfers.aws.topic-arn}"],
)
@ImportAutoConfiguration(JacksonAutoConfiguration::class)
abstract class MessagingContractBase {
    @Autowired
    private lateinit var outbox: EventOutbox

    @Autowired
    private lateinit var verifier: OutboxMessageVerifier

    @BeforeEach
    fun startFromAnEmptyOutbox() {
        verifier.clear()
        MDC.put(OutboxEventEnvelope.CORRELATION_ID_MDC_KEY, CORRELATION_ID)
    }

    @AfterEach
    fun clearCorrelationId() {
        MDC.remove(OutboxEventEnvelope.CORRELATION_ID_MDC_KEY)
    }

    fun transferCompleted() =
        outbox.append(
            TransferEvent.TransferCompleted(TRANSFER_ID, SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID, Money(5000), "Rent"),
        )

    fun transferFailed() =
        outbox.append(
            TransferEvent.TransferFailed(
                TRANSFER_ID,
                SOURCE_ACCOUNT_ID,
                DESTINATION_ACCOUNT_ID,
                Money(5000),
                null,
                StepOutcome.INSUFFICIENT_AVAILABLE_BALANCE,
            ),
        )

    fun transferCompensated() =
        outbox.append(
            TransferEvent.TransferCompensated(
                TRANSFER_ID,
                SOURCE_ACCOUNT_ID,
                DESTINATION_ACCOUNT_ID,
                Money(5000),
                "Rent",
                "credit-not-confirmed",
            ),
        )

    fun transferCompensatedWithoutReason() =
        outbox.append(
            TransferEvent.TransferCompensated(TRANSFER_ID, SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID, Money(5000), "Rent", null),
        )

    private companion object {
        const val CORRELATION_ID = "contract-correlation-id"
        val TRANSFER_ID: UUID = UUID.fromString("0198a1b2-0000-7000-8000-00000000f001")
        val SOURCE_ACCOUNT_ID: UUID = UUID.fromString("0198a1b2-0000-7000-8000-000000000001")
        val DESTINATION_ACCOUNT_ID: UUID = UUID.fromString("0198a1b2-0000-7000-8000-000000000002")
    }
}
