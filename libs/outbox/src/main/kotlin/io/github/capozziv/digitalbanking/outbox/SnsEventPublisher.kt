package io.github.capozziv.digitalbanking.outbox

import software.amazon.awssdk.services.sns.SnsClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import java.time.Duration

/**
 * Publishes to an SNS FIFO topic: the body is the [OutboxEnvelope], the message group is the aggregate
 * (order per aggregate) and the deduplication id is the event id. `eventType` and `correlationId` also
 * go as message attributes, so subscriptions with raw message delivery can read them without the body.
 * Each call is limited to [publishTimeout] (use [OutboxRelaySettings.publishTimeout]).
 */
class SnsEventPublisher(
    private val snsClient: SnsClient,
    private val topicArn: String,
    private val publishTimeout: Duration,
) : EventPublisher {
    override fun publish(event: PendingEvent) {
        val attributes =
            buildMap {
                put(EVENT_TYPE_ATTRIBUTE, stringAttribute(event.eventType))
                event.correlationId?.let { put(CORRELATION_ID_ATTRIBUTE, stringAttribute(it)) }
            }
        snsClient.publish {
            it
                .topicArn(topicArn)
                .message(OutboxEnvelope.toJson(event))
                .messageGroupId(event.aggregateId)
                .messageDeduplicationId(event.eventId.toString())
                .messageAttributes(attributes)
                .overrideConfiguration { config -> config.apiCallTimeout(publishTimeout) }
        }
    }

    private fun stringAttribute(value: String) =
        MessageAttributeValue
            .builder()
            .dataType("String")
            .stringValue(value)
            .build()

    companion object {
        const val EVENT_TYPE_ATTRIBUTE = "eventType"
        const val CORRELATION_ID_ATTRIBUTE = "correlationId"
    }
}
