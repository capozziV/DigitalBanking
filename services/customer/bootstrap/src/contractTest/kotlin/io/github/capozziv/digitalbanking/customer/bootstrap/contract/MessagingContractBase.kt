package io.github.capozziv.digitalbanking.customer.bootstrap.contract

import io.github.capozziv.digitalbanking.customer.adapters.persistence.OutboxEventAppender
import io.github.capozziv.digitalbanking.customer.adapters.persistence.OutboxWriterConfiguration
import io.github.capozziv.digitalbanking.customer.application.port.EventOutbox
import io.github.capozziv.digitalbanking.customer.domain.CustomerEvent
import io.github.capozziv.digitalbanking.customer.domain.CustomerPhoneChanged
import io.github.capozziv.digitalbanking.customer.domain.CustomerRegistered
import io.github.capozziv.digitalbanking.customer.domain.PhoneNumber
import io.github.capozziv.digitalbanking.outbox.OutboxEventEnvelope
import io.github.capozziv.digitalbanking.testing.outboxcontract.OutboxMessageVerifier
import io.github.capozziv.digitalbanking.testing.outboxcontract.OutboxMessagingContractConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import java.util.UUID

/**
 * Base of the message contracts of customer (contracts/messaging). Each trigger appends one event through the
 * service's outbox mapper (OutboxEventAppender, with the JSON mapper Boot builds from application.yml) into a real
 * outbox table, with a correlation id in the MDC as a request would leave it; OutboxMessageVerifier reads the row back
 * as the envelope the relay publishes to the topic of `customer.aws.topic-arn`. Needs Docker (PostgreSQL).
 *
 * The context is listed in @SpringBootTest, with no @SpringBootConfiguration of its own: the @WebMvcTest of
 * HttpContractBase, in this same package, would find it and lose the controllers.
 */
@SpringBootTest(
    classes = [OutboxMessagingContractConfiguration::class, OutboxWriterConfiguration::class, OutboxEventAppender::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = ["digitalbanking.contract.messaging.topic-arn=\${customer.aws.topic-arn}"],
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

    fun customerRegistered() = append(CustomerRegistered(CUSTOMER_ID, "Ana Souza", "ana@example.test", PhoneNumber("+5511987654321")))

    fun customerPhoneChanged() = append(CustomerPhoneChanged(CUSTOMER_ID, PhoneNumber("+5511987654321"), PhoneNumber("+5521988776655")))

    private fun append(event: CustomerEvent) = outbox.append(event)

    private companion object {
        const val CORRELATION_ID = "contract-correlation-id"
        val CUSTOMER_ID: UUID = UUID.fromString("0198a1b2-0000-7000-8000-0000000000c1")
    }
}
