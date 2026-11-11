package io.github.capozziv.digitalbanking.statement.bootstrap.persistence

import io.github.capozziv.digitalbanking.statement.application.port.StatementRepository
import io.github.capozziv.digitalbanking.statement.bootstrap.StatementIntegrationTest
import io.github.capozziv.digitalbanking.statement.domain.EntryKind
import io.github.capozziv.digitalbanking.statement.domain.StatementCursor
import io.github.capozziv.digitalbanking.statement.domain.StatementEntry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.time.Instant
import java.util.UUID

class DynamoDbStatementRepositoryIntegrationTest : StatementIntegrationTest() {
    @Autowired
    private lateinit var repository: StatementRepository

    private val account = UUID.randomUUID()

    @Test
    fun `inserting the same key twice returns false and keeps one item`() {
        val first = entry(sequence = 1)
        val sameKey = entry(sequence = 1, amountCents = 9_999)

        assertThat(repository.insertEntry(first)).isTrue()
        assertThat(repository.insertEntry(sameKey)).isFalse()

        assertThat(repository.findEntry(account, 1)).isEqualTo(first)
        assertThat(itemCount()).isEqualTo(1)
    }

    @Test
    fun `findEntry returns what was inserted, with a negative balance kept negative`() {
        val overdrawn = entry(sequence = 3, kind = EntryKind.DEBIT, balanceAfterCents = -5_000, description = "rent")

        repository.insertEntry(overdrawn)

        assertThat(repository.findEntry(account, 3)).isEqualTo(overdrawn)
    }

    @Test
    fun `a null description is not stored and comes back null`() {
        val withoutDescription = entry(sequence = 1, description = null)

        repository.insertEntry(withoutDescription)

        assertThat(repository.findEntry(account, 1)).isEqualTo(withoutDescription)
        assertThat(rawItem(1)).doesNotContainKey("description")
    }

    @Test
    fun `findEntry of a missing key is null`() {
        assertThat(repository.findEntry(account, 1)).isNull()
    }

    @Test
    fun `pages walk newest to oldest`() {
        (1L..45L).forEach { repository.insertEntry(entry(sequence = it)) }

        val first = repository.findPage(account, null, 20)
        val second = repository.findPage(account, first.nextCursor, 20)
        val third = repository.findPage(account, second.nextCursor, 20)

        assertThat(first.entries.map { it.sequence }).isEqualTo((45L downTo 26L).toList())
        assertThat(first.nextCursor).isEqualTo(StatementCursor(26))
        assertThat(second.entries.map { it.sequence }).isEqualTo((25L downTo 6L).toList())
        assertThat(second.nextCursor).isEqualTo(StatementCursor(6))
        assertThat(third.entries.map { it.sequence }).isEqualTo((5L downTo 1L).toList())
        assertThat(third.nextCursor).isNull()
    }

    @Test
    fun `a full page ending at sequence 1 has no next cursor`() {
        (1L..2L).forEach { repository.insertEntry(entry(sequence = it)) }

        val page = repository.findPage(account, null, 2)

        assertThat(page.entries.map { it.sequence }).containsExactly(2L, 1L)
        assertThat(page.nextCursor).isNull()
    }

    @Test
    fun `cursor 1 is an empty page`() {
        (1L..3L).forEach { repository.insertEntry(entry(sequence = it)) }

        val page = repository.findPage(account, StatementCursor(1), 20)

        assertThat(page.entries).isEmpty()
        assertThat(page.nextCursor).isNull()
    }

    @Test
    fun `cursor above the last sequence lists from the newest entry`() {
        (1L..3L).forEach { repository.insertEntry(entry(sequence = it)) }

        val page = repository.findPage(account, StatementCursor(999), 20)

        assertThat(page.entries.map { it.sequence }).containsExactly(3L, 2L, 1L)
    }

    @Test
    fun `account item is not listed`() {
        repository.insertAccount(account, UUID.randomUUID())
        repository.insertEntry(entry(sequence = 1))

        val page = repository.findPage(account, null, 20)

        assertThat(page.entries.map { it.sequence }).containsExactly(1L)
    }

    @Test
    fun `entries without the account item are listed`() {
        repository.insertEntry(entry(sequence = 1))

        assertThat(repository.accountExists(account)).isFalse()
        assertThat(repository.findPage(account, null, 20).entries.map { it.sequence }).containsExactly(1L)
    }

    @Test
    fun `entries inserted out of order come back ordered`() {
        listOf(3L, 1L, 5L, 2L, 4L).forEach { repository.insertEntry(entry(sequence = it)) }

        val page = repository.findPage(account, null, 20)

        assertThat(page.entries.map { it.sequence }).containsExactly(5L, 4L, 3L, 2L, 1L)
    }

    @Test
    fun `an account is stored once`() {
        val customer = UUID.randomUUID()

        assertThat(repository.accountExists(account)).isFalse()
        assertThat(repository.insertAccount(account, customer)).isTrue()
        assertThat(repository.insertAccount(account, UUID.randomUUID())).isFalse()

        assertThat(repository.accountExists(account)).isTrue()
        assertThat(rawItem(0)).containsEntry("customerId", AttributeValue.fromS(customer.toString()))
        assertThat(rawItem(0)).containsEntry("itemType", AttributeValue.fromS("ACCOUNT"))
    }

    @Test
    fun `an entry is stored with the documented attributes`() {
        val stored = entry(sequence = 7, kind = EntryKind.DEBIT, balanceAfterCents = -1, description = "fee")

        repository.insertEntry(stored)

        assertThat(rawItem(7)).isEqualTo(
            mapOf(
                "accountId" to AttributeValue.fromS(account.toString()),
                "sequence" to AttributeValue.fromN("7"),
                "itemType" to AttributeValue.fromS("ENTRY"),
                "entryId" to AttributeValue.fromS(stored.entryId.toString()),
                "kind" to AttributeValue.fromS("DEBIT"),
                "amount" to AttributeValue.fromN("1000"),
                "balanceAfter" to AttributeValue.fromN("-1"),
                "description" to AttributeValue.fromS("fee"),
                "createdAt" to AttributeValue.fromS("2026-11-10T12:00:07Z"),
            ),
        )
    }

    private fun entry(
        sequence: Long,
        kind: EntryKind = EntryKind.CREDIT,
        amountCents: Long = 1_000,
        balanceAfterCents: Long = 1_000 * sequence,
        description: String? = "deposit $sequence",
    ) = StatementEntry(
        entryId = UUID.randomUUID(),
        accountId = account,
        sequence = sequence,
        kind = kind,
        amountCents = amountCents,
        balanceAfterCents = balanceAfterCents,
        description = description,
        createdAt = Instant.parse("2026-11-10T12:00:00Z").plusSeconds(sequence),
    )

    private fun rawItem(sequence: Long): Map<String, AttributeValue> =
        dynamoDb
            .getItem {
                it
                    .tableName(TABLE)
                    .consistentRead(true)
                    .key(
                        mapOf(
                            "accountId" to AttributeValue.fromS(account.toString()),
                            "sequence" to AttributeValue.fromN(sequence.toString()),
                        ),
                    )
            }.item()

    private fun itemCount(): Int =
        dynamoDb
            .query {
                it
                    .tableName(TABLE)
                    .consistentRead(true)
                    .keyConditionExpression("accountId = :id")
                    .expressionAttributeValues(mapOf(":id" to AttributeValue.fromS(account.toString())))
            }.count()
}
