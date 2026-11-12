package io.github.capozziv.digitalbanking.statement.bootstrap

import io.github.capozziv.digitalbanking.statement.adapters.messaging.QueueProperties
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.binder.MeterBinder
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.Scheduled
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Gauge `statement.dlq.messages{queue}`: the approximate number of messages waiting in each DLQ. A scrape reads the
 * last stored value; [DlqDepthRefresher] updates it every `statement.dlq-metrics.refresh-interval`.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(QueueProperties::class)
class DlqMetricsConfiguration {
    @Bean
    fun dlqDepthRefresher(
        queues: QueueProperties,
        sqs: SqsAsyncClient,
    ) = DlqDepthRefresher(queues.deadLetterQueues, sqs)

    @Bean
    fun dlqMetrics(refresher: DlqDepthRefresher): MeterBinder =
        MeterBinder { registry ->
            refresher.queues.forEach { queue ->
                Gauge
                    .builder("statement.dlq.messages") { refresher.depth(queue) }
                    .tag("queue", queue)
                    .description("Approximate number of messages in the dead-letter queue")
                    .register(registry)
            }
        }
}

/** Reads the DLQ depths from SQS on a schedule and keeps the last value; a failure is stored as NaN, never thrown. */
class DlqDepthRefresher(
    val queues: List<String>,
    private val sqs: SqsAsyncClient,
) {
    private val urls = ConcurrentHashMap<String, String>()
    private val depths = ConcurrentHashMap<String, Double>()

    /** NaN until the first refresh. */
    fun depth(queue: String): Double = depths[queue] ?: Double.NaN

    @Scheduled(fixedDelayString = "\${statement.dlq-metrics.refresh-interval}")
    fun refresh() {
        queues.forEach { depths[it] = read(it) }
    }

    private fun read(queue: String): Double =
        try {
            val url =
                urls.computeIfAbsent(
                    queue,
                ) { name -> sqs.getQueueUrl { it.queueName(name) }.get(TIMEOUT_SECONDS, TimeUnit.SECONDS).queueUrl() }
            sqs
                .getQueueAttributes { it.queueUrl(url).attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES) }
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .attributes()
                .getValue(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
                .toDouble()
        } catch (e: Exception) {
            if (e is InterruptedException) Thread.currentThread().interrupt()
            log.warn("could not read the depth of DLQ {}: {}", queue, e.toString())
            Double.NaN
        }

    private companion object {
        const val TIMEOUT_SECONDS = 2L
        val log = LoggerFactory.getLogger(DlqDepthRefresher::class.java)
    }
}
