package io.github.capozziv.digitalbanking.notification.bootstrap

import io.awspring.cloud.sqs.annotation.SqsListener
import io.github.capozziv.digitalbanking.testing.localstack.LocalStackTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.localstack.LocalStackContainer
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import java.time.Duration
import java.time.Instant
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Proof that Spring Cloud AWS `@SqsListener` works with this project's Spring Boot and AWS SDK versions on a FIFO
 * queue of LocalStack: messages of one group arrive in order, and with `ON_SUCCESS` a message whose processing throws
 * is not acknowledged, so it comes back after the visibility timeout and is deleted only once processing succeeds.
 */
@SpringBootTest(classes = [NotificationApplication::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(SqsListenerFifoIntegrationTest.ListenersConfiguration::class)
// The context is closed with the class (listeners stop polling) and Testcontainers removes LocalStack afterwards.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SqsListenerFifoIntegrationTest {
    @Autowired
    private lateinit var listeners: TestListeners

    @Test
    fun `messages of the same group are received in the order they were sent`() {
        val group = UUID.randomUUID().toString()
        val bodies = listOf("first-$group", "second-$group", "third-$group")

        bodies.forEach { send(ORDERED_QUEUE, group, it) }

        await { listeners.ordered.filter { it.endsWith(group) }.size == bodies.size }
        assertThat(listeners.ordered.filter { it.endsWith(group) }).containsExactlyElementsOf(bodies)
        awaitEmpty(ORDERED_QUEUE)
    }

    @Test
    fun `a message whose processing throws comes back after the visibility timeout and is deleted after success`() {
        val body = "retry-${UUID.randomUUID()}"

        send(RETRY_QUEUE, UUID.randomUUID().toString(), body)

        await { body in listeners.retried }
        assertThat(listeners.deliveries.getValue(body).get()).isEqualTo(2)
        awaitEmpty(RETRY_QUEUE)
        // Nothing left in flight or visible, so no third delivery can happen: the success acknowledged the message.
        assertThat(listeners.deliveries.getValue(body).get()).isEqualTo(2)
    }

    @Test
    fun `when a message fails the next message of the same group is not delivered before it succeeds`() {
        val group = UUID.randomUUID().toString()
        val first = "first-$group"
        val second = "second-$group"

        send(BLOCKING_QUEUE, group, first)
        send(BLOCKING_QUEUE, group, second)

        await { second in listeners.blockingSucceeded }
        // Every delivery attempt in order: M1 failed, M1 succeeded, only then M2 was ever delivered.
        assertThat(listeners.blockingAttempts.filter { it.endsWith(group) })
            .containsExactly(first, first, second)
        assertThat(listeners.blockingSucceeded.filter { it.endsWith(group) }).containsExactly(first, second)
        awaitEmpty(BLOCKING_QUEUE)
    }

    private fun send(
        queue: String,
        group: String,
        body: String,
    ) {
        sqs.sendMessage {
            it
                .queueUrl(queueUrl(queue))
                .messageGroupId(group)
                .messageDeduplicationId(UUID.randomUUID().toString())
                .messageBody(body)
        }
    }

    private fun awaitEmpty(queue: String) =
        await {
            val attributes =
                sqs
                    .getQueueAttributes {
                        it.queueUrl(queueUrl(queue)).attributeNames(
                            QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                            QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE,
                        )
                    }.attributes()
            attributes[QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES] == "0" &&
                attributes[QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE] == "0"
        }

    private fun await(condition: () -> Boolean) {
        val deadline = Instant.now().plus(Duration.ofSeconds(20))
        while (!condition()) {
            check(Instant.now().isBefore(deadline)) { "condition not met within 20s" }
            Thread.sleep(100)
        }
    }

    /** Records what the listeners received; the retry listener throws on the first delivery of each message. */
    class TestListeners {
        val ordered: MutableList<String> = Collections.synchronizedList(mutableListOf())
        val retried: MutableSet<String> = ConcurrentHashMap.newKeySet()
        val blockingAttempts: MutableList<String> = Collections.synchronizedList(mutableListOf())
        val blockingSucceeded: MutableList<String> = Collections.synchronizedList(mutableListOf())
        val deliveries = ConcurrentHashMap<String, AtomicInteger>()

        @SqsListener(queueNames = [ORDERED_QUEUE], acknowledgementMode = "ON_SUCCESS")
        fun onOrdered(body: String) {
            ordered.add(body)
        }

        @SqsListener(queueNames = [BLOCKING_QUEUE], acknowledgementMode = "ON_SUCCESS")
        fun onBlocking(body: String) {
            val firstAttempt = blockingAttempts.none { it == body }
            blockingAttempts.add(body)
            check(!(firstAttempt && body.startsWith("first-"))) { "simulated failure on the first delivery of $body" }
            blockingSucceeded.add(body)
        }

        @SqsListener(queueNames = [RETRY_QUEUE], acknowledgementMode = "ON_SUCCESS")
        fun onRetry(body: String) {
            val delivery = deliveries.computeIfAbsent(body) { AtomicInteger() }.incrementAndGet()
            check(delivery > 1) { "simulated failure on the first delivery of $body" }
            retried.add(body)
        }
    }

    @TestConfiguration
    class ListenersConfiguration {
        @Bean
        fun testListeners() = TestListeners()
    }

    companion object {
        private const val ORDERED_QUEUE = "a1-ordered.fifo"
        private const val RETRY_QUEUE = "a1-retry.fifo"
        private const val BLOCKING_QUEUE = "a1-blocking.fifo"
        private const val VISIBILITY_SECONDS = 1L

        private val localStack: LocalStackContainer by lazy {
            LocalStackTestSupport.container(listOf("sqs")).also { it.start() }
        }
        private val sqs: SqsClient by lazy { LocalStackTestSupport.sqsClient(localStack) }

        private fun queueUrl(queue: String): String = sqs.getQueueUrl { it.queueName(queue) }.queueUrl()

        /** Queues exist before the context starts, since the listener containers resolve their URLs on startup. */
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            listOf(ORDERED_QUEUE, RETRY_QUEUE, BLOCKING_QUEUE).forEach { queue ->
                sqs.createQueue {
                    it.queueName(queue).attributes(
                        mapOf(
                            QueueAttributeName.FIFO_QUEUE to "true",
                            QueueAttributeName.VISIBILITY_TIMEOUT to VISIBILITY_SECONDS.toString(),
                        ),
                    )
                }
            }
            // The whole application starts, so it needs its database too.
            NotificationIntegrationTest.databaseProperties(registry)
            registry.add("spring.cloud.aws.sqs.endpoint") { localStack.endpoint.toString() }
            registry.add("spring.cloud.aws.region.static") { localStack.region }
            registry.add("spring.cloud.aws.credentials.access-key") { localStack.accessKey }
            registry.add("spring.cloud.aws.credentials.secret-key") { localStack.secretKey }
        }
    }
}
