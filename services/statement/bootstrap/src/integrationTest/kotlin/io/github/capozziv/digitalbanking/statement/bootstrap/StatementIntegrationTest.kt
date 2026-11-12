package io.github.capozziv.digitalbanking.statement.bootstrap

import io.github.capozziv.digitalbanking.testing.localstack.LocalStackTestSupport
import io.github.capozziv.digitalbanking.testing.redis.RedisTestSupport
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.localstack.LocalStackContainer
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.BillingMode
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.QueueAttributeName

/**
 * Base of the integration tests of the statement service. One LocalStack container (DynamoDB, SNS, SQS) is shared by
 * all subclasses; the table and the queues, with the names the application uses by default, exist before the
 * context starts, since SQS listener containers resolve their queue URLs on startup.
 *
 * The properties are in [TestPropertySource], which subclasses inherit and merge, so a subclass's own
 * `@SpringBootTest` (e.g. a web environment) does not drop them. Tests share the table: each one uses its own
 * random account ids.
 */
@SpringBootTest
@TestPropertySource(
    properties = [
        // The DLQ gauge refreshes once at startup; no further reads during the tests.
        "statement.dlq-metrics.refresh-interval=1h",
    ],
)
abstract class StatementIntegrationTest {
    companion object {
        const val TABLE = "statement"
        const val ACCOUNTS_QUEUE = "statement-accounts.fifo"
        const val ACCOUNTS_DLQ = "statement-accounts-dlq.fifo"
        const val MAX_RECEIVE_COUNT = 5

        /** Short, so a failed message is retried quickly; production uses 30 s. */
        const val VISIBILITY_SECONDS = 1

        @JvmStatic
        val localStack: LocalStackContainer by lazy {
            LocalStackTestSupport.container(listOf("dynamodb", "sns", "sqs")).also { it.start() }
        }

        /** Redis of the first-page cache, shared by all subclasses like the LocalStack container. */
        @JvmStatic
        val redis: GenericContainer<*> by lazy {
            RedisTestSupport.container().also { it.start() }
        }

        /** Raw DynamoDB client, for setup and for inspecting items as stored. */
        @JvmStatic
        val dynamoDb: DynamoDbClient by lazy { LocalStackTestSupport.dynamoDbClient(localStack) }

        @JvmStatic
        val sqs: SqsClient by lazy { LocalStackTestSupport.sqsClient(localStack) }

        @JvmStatic
        @DynamicPropertySource
        fun awsProperties(registry: DynamicPropertyRegistry) {
            createTable()
            createQueueWithDlq(ACCOUNTS_QUEUE, ACCOUNTS_DLQ)
            // An explicit endpoint also selects the static test credentials (AwsCredentialsConfiguration).
            registry.add("spring.cloud.aws.sqs.endpoint") { localStack.endpoint.toString() }
            registry.add("statement.dynamodb.endpoint") { localStack.endpoint.toString() }
            registry.add("spring.cloud.aws.region.static") { localStack.region }
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.getMappedPort(RedisTestSupport.PORT) }
        }

        private fun createTable() {
            val exists =
                try {
                    dynamoDb.describeTable { it.tableName(TABLE) }
                    true
                } catch (_: ResourceNotFoundException) {
                    false
                }
            if (exists) return
            dynamoDb.createTable {
                it
                    .tableName(TABLE)
                    .attributeDefinitions(
                        AttributeDefinition
                            .builder()
                            .attributeName("accountId")
                            .attributeType(ScalarAttributeType.S)
                            .build(),
                        AttributeDefinition
                            .builder()
                            .attributeName("sequence")
                            .attributeType(ScalarAttributeType.N)
                            .build(),
                    ).keySchema(
                        KeySchemaElement
                            .builder()
                            .attributeName("accountId")
                            .keyType(KeyType.HASH)
                            .build(),
                        KeySchemaElement
                            .builder()
                            .attributeName("sequence")
                            .keyType(KeyType.RANGE)
                            .build(),
                    ).billingMode(BillingMode.PAY_PER_REQUEST)
            }
            dynamoDb.waiter().waitUntilTableExists { it.tableName(TABLE) }
        }

        /** CreateQueue is idempotent for the same name and attributes, so every subclass may call it. */
        private fun createQueueWithDlq(
            queue: String,
            dlq: String,
        ) {
            val dlqUrl = sqs.createQueue { it.queueName(dlq).attributes(mapOf(QueueAttributeName.FIFO_QUEUE to "true")) }.queueUrl()
            val dlqArn =
                sqs
                    .getQueueAttributes { it.queueUrl(dlqUrl).attributeNames(QueueAttributeName.QUEUE_ARN) }
                    .attributes()
                    .getValue(QueueAttributeName.QUEUE_ARN)
            sqs.createQueue {
                it.queueName(queue).attributes(
                    mapOf(
                        QueueAttributeName.FIFO_QUEUE to "true",
                        QueueAttributeName.VISIBILITY_TIMEOUT to VISIBILITY_SECONDS.toString(),
                        QueueAttributeName.REDRIVE_POLICY to
                            """{"deadLetterTargetArn":"$dlqArn","maxReceiveCount":"$MAX_RECEIVE_COUNT"}""",
                    ),
                )
            }
        }
    }
}
