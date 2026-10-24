package io.github.capozziv.digitalbanking.outbox

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import java.time.Duration
import java.time.Instant
import java.util.UUID

class SnsEventPublisherTest {
    @Test
    fun `every publish request carries the api call timeout`() {
        val sns = CapturingSnsClient()

        SnsEventPublisher(sns, "arn:aws:sns:us-east-1:000000000000:t.fifo", Duration.ofMillis(1500)).publish(event())

        assertThat(
            sns.requests
                .single()
                .overrideConfiguration()
                .flatMap { it.apiCallTimeout() },
        ).hasValue(Duration.ofMillis(1500))
    }

    private fun event() =
        PendingEvent(
            position = 1,
            eventId = UUID(0, 1),
            aggregateId = "acc-1",
            eventType = "LedgerEntryRecorded",
            eventVersion = 1,
            payloadJson = "{}",
            correlationId = null,
            occurredAt = Instant.parse("2026-10-22T14:31:00Z"),
            attempts = 0,
        )

    private class CapturingSnsClient : SnsClient {
        val requests = mutableListOf<PublishRequest>()

        override fun publish(publishRequest: PublishRequest): PublishResponse {
            requests += publishRequest
            return PublishResponse.builder().messageId("m-1").build()
        }

        override fun serviceName(): String = SnsClient.SERVICE_NAME

        override fun close() = Unit
    }
}
