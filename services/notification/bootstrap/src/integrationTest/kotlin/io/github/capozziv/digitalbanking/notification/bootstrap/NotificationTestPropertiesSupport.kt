package io.github.capozziv.digitalbanking.notification.bootstrap

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.localstack.LocalStackContainer

/** AWS properties shared by the `@SpringBootTest` classes of the notification service. */
object NotificationTestPropertiesSupport {
    /**
     * Points the SQS client at [localStack]. An endpoint also selects the static test credentials
     * (AwsCredentialsConfiguration), so no keys are needed.
     */
    fun localStack(
        registry: DynamicPropertyRegistry,
        localStack: LocalStackContainer,
    ) {
        registry.add("spring.cloud.aws.sqs.endpoint") { localStack.endpoint.toString() }
        registry.add("spring.cloud.aws.region.static") { localStack.region }
    }
}
