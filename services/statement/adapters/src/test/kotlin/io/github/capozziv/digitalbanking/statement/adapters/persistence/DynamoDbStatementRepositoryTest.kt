package io.github.capozziv.digitalbanking.statement.adapters.persistence

import io.github.capozziv.digitalbanking.statement.domain.StatementCursor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.model.QueryResponse
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.function.Consumer

class DynamoDbStatementRepositoryTest {
    private val queries = mutableListOf<QueryRequest>()

    // Records the QueryRequest of every query call and answers with an empty result.
    @Suppress("UNCHECKED_CAST")
    private val dynamoDb =
        Proxy.newProxyInstance(javaClass.classLoader, arrayOf(DynamoDbClient::class.java)) { _, method, args ->
            check(method.name == "query") { "unexpected call: ${method.name}" }
            val request =
                when (val arg = args[0]) {
                    is QueryRequest -> arg
                    else -> QueryRequest.builder().also { (arg as Consumer<QueryRequest.Builder>).accept(it) }.build()
                }
            queries += request
            QueryResponse.builder().build()
        } as DynamoDbClient

    private val repository = DynamoDbStatementRepository(dynamoDb, "statement")

    // The SDK getter returns a Boolean that Kotlin treats as non-null; read the raw field to see an unset value.
    private fun consistentRead(): Boolean? {
        val request = queries.single()
        return request.sdkFields().single { it.memberName() == "ConsistentRead" }.getValueOrDefault(request) as Boolean?
    }

    @Test
    fun `the first page is read with strong consistency`() {
        repository.findPage(UUID.randomUUID(), cursor = null, limit = 20)

        assertThat(consistentRead()).isTrue()
    }

    @Test
    fun `a page after a cursor is read with eventual consistency`() {
        repository.findPage(UUID.randomUUID(), cursor = StatementCursor(50), limit = 20)

        assertThat(consistentRead()).isNotEqualTo(true)
    }
}
