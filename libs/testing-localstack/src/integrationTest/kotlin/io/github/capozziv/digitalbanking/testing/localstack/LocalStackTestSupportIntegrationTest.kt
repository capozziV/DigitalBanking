package io.github.capozziv.digitalbanking.testing.localstack

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class LocalStackTestSupportIntegrationTest {
    @Test
    fun `sqs client creates and lists a queue`() {
        val client = LocalStackTestSupport.sqsClient(localStack)

        val url = client.createQueue { it.queueName("probe") }.queueUrl()

        assertThat(client.listQueues().queueUrls()).contains(url)
    }

    companion object {
        @Container
        @JvmStatic
        val localStack = LocalStackTestSupport.container(listOf("sqs"))
    }
}
