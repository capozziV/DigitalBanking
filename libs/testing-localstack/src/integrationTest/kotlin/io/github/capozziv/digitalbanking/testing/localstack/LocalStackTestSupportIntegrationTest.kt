package io.github.capozziv.digitalbanking.testing.localstack

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.BillingMode
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType

@Testcontainers
class LocalStackTestSupportIntegrationTest {
    @Test
    fun `dynamo db client creates and lists a table`() {
        val client = LocalStackTestSupport.dynamoDbClient(localStack)

        client.createTable(
            CreateTableRequest
                .builder()
                .tableName("probe")
                .attributeDefinitions(
                    AttributeDefinition
                        .builder()
                        .attributeName("id")
                        .attributeType(ScalarAttributeType.S)
                        .build(),
                ).keySchema(
                    KeySchemaElement
                        .builder()
                        .attributeName("id")
                        .keyType(KeyType.HASH)
                        .build(),
                ).billingMode(BillingMode.PAY_PER_REQUEST)
                .build(),
        )

        assertThat(client.listTables().tableNames()).contains("probe")
    }

    companion object {
        @Container
        @JvmStatic
        val localStack = LocalStackTestSupport.container(listOf("dynamodb"))
    }
}
