package io.github.capozziv.digitalbanking.notification.bootstrap

import io.github.capozziv.digitalbanking.notification.adapters.messaging.QueueProperties
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.binder.MeterBinder
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.QueueAttributeName
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** Gauge `notification.dlq.messages{queue}`: the approximate number of messages waiting in each DLQ. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(QueueProperties::class)
class DlqMetricsConfiguration {
    @Bean
    fun dlqMetrics(
        queues: QueueProperties,
        sqs: SqsAsyncClient,
    ): MeterBinder {
        val probe = DlqDepthProbe(sqs)
        return MeterBinder { registry ->
            queues.deadLetterQueues.forEach { queue ->
                Gauge
                    .builder("notification.dlq.messages") { probe.depth(queue) }
                    .tag("queue", queue)
                    .description("Approximate number of messages in the dead-letter queue")
                    .register(registry)
            }
        }
    }
}

/** Reads the depth on each scrape; a failure (queue missing, SQS down) is reported as NaN, never thrown. */
internal class DlqDepthProbe(
    private val sqs: SqsAsyncClient,
) {
    private val urls = ConcurrentHashMap<String, String>()

    fun depth(queue: String): Double =
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
        val log = LoggerFactory.getLogger(DlqDepthProbe::class.java)
    }
}
