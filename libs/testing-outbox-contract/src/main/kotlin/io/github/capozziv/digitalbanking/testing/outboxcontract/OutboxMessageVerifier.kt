package io.github.capozziv.digitalbanking.testing.outboxcontract

import io.github.capozziv.digitalbanking.outbox.OutboxEventEnvelope
import io.github.capozziv.digitalbanking.outbox.relay.JdbcOutboxStore
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.transaction.support.TransactionOperations
import sh.stubborn.contract.verifier.converter.YamlContract
import sh.stubborn.contract.verifier.messaging.ContractMessage
import sh.stubborn.contract.verifier.messaging.MessageVerifierReceiver
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender
import java.time.Clock
import java.util.concurrent.TimeUnit

/**
 * The message verifier of a producer whose events leave through the outbox. Stubborn Contract has no SNS/SQS
 * verifier, so this one stands in for the relay: it reads the pending row with the relay's query
 * ([JdbcOutboxStore.fetchPending]), marks it published, and returns [OutboxEventEnvelope.toJson] of it, which is the
 * body SnsEventPublisher sends. A contract trigger must record exactly one event.
 *
 * The destination of a contract (`sentTo`) is the name of the SNS topic, the last part of [topicArn], so the contract
 * also checks the topic the service is configured to publish to.
 */
class OutboxMessageVerifier(
    private val jdbcClient: JdbcClient,
    topicArn: String,
    private val clock: Clock = Clock.systemUTC(),
) : MessageVerifierReceiver<ContractMessage>,
    MessageVerifierSender<ContractMessage> {
    private val topic = topicArn.substringAfterLast(':')

    // Leadership and discard are never used here, so the store needs no transactions.
    private val store = JdbcOutboxStore(jdbcClient, TransactionOperations.withoutTransaction())

    /** Removes every row of the outbox, so each contract starts from an empty one. */
    fun clear() {
        jdbcClient.sql("truncate table outbox").update()
    }

    override fun receive(
        destination: String,
        contract: YamlContract?,
    ): ContractMessage {
        if (destination != topic) throw AssertionError("the service publishes to $topic, not to $destination")
        val pending = store.fetchPending(FETCH_LIMIT)
        if (pending.isEmpty()) throw AssertionError("the trigger recorded no event in the outbox")
        if (pending.size > 1) {
            throw AssertionError("the trigger must record exactly one event, but recorded ${pending.size} events")
        }
        val event = pending.single()
        store.markPublished(event.position, clock.instant())
        return EnvelopeMessage(OutboxEventEnvelope.toJson(event))
    }

    override fun receive(
        destination: String,
        timeout: Long,
        timeUnit: TimeUnit,
        contract: YamlContract?,
    ): ContractMessage = receive(destination, contract)

    override fun send(
        message: ContractMessage,
        destination: String,
        contract: YamlContract?,
    ): Unit = throw UnsupportedOperationException("the producer only publishes; no contract sends it a message")

    override fun <T : Any?> send(
        payload: T,
        headers: Map<String, Any>?,
        destination: String,
        contract: YamlContract?,
    ): Unit = throw UnsupportedOperationException("the producer only publishes; no contract sends it a message")

    private class EnvelopeMessage(
        private val body: String,
    ) : ContractMessage {
        override fun getPayload(): Any = body

        override fun getHeaders(): Map<String, Any> = emptyMap()
    }

    private companion object {
        const val FETCH_LIMIT = 10
    }
}
