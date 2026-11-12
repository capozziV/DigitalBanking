package io.github.capozziv.digitalbanking.statement.adapters.persistence

import io.github.capozziv.digitalbanking.statement.application.port.StatementRepository
import io.github.capozziv.digitalbanking.statement.domain.EntryKind
import io.github.capozziv.digitalbanking.statement.domain.StatementCursor
import io.github.capozziv.digitalbanking.statement.domain.StatementEntry
import io.github.capozziv.digitalbanking.statement.domain.StatementPage
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException
import java.time.Instant
import java.util.UUID

/**
 * The statement table (ADR 0018): partition key `accountId`, sort key `sequence`. Each ledger entry is an item at its
 * ledger sequence (1 and up); the account is one item at sequence 0, kept out of every page. All DynamoDB access of
 * the service lives here.
 */
@Repository
class DynamoDbStatementRepository(
    private val dynamoDb: DynamoDbClient,
    @param:Value("\${statement.dynamodb.table}") private val table: String,
) : StatementRepository {
    override fun insertEntry(entry: StatementEntry): Boolean =
        putIfAbsent(
            buildMap {
                putAll(key(entry.accountId, entry.sequence))
                put(ITEM_TYPE, AttributeValue.fromS(ENTRY_ITEM))
                put(ENTRY_ID, AttributeValue.fromS(entry.entryId.toString()))
                put(KIND, AttributeValue.fromS(entry.kind.name))
                put(AMOUNT, AttributeValue.fromN(entry.amountCents.toString()))
                put(BALANCE_AFTER, AttributeValue.fromN(entry.balanceAfterCents.toString()))
                // A null description is left out: DynamoDB has no empty-string stand-in to confuse with it.
                entry.description?.let { put(DESCRIPTION, AttributeValue.fromS(it)) }
                put(CREATED_AT, AttributeValue.fromS(entry.createdAt.toString()))
            },
        )

    /** Strongly consistent: the record use case reads here right after a rejected insert. */
    override fun findEntry(
        accountId: UUID,
        sequence: Long,
    ): StatementEntry? = getItem(accountId, sequence)?.let(::toEntry)

    override fun insertAccount(
        accountId: UUID,
        customerId: UUID,
    ): Boolean =
        putIfAbsent(
            key(accountId, ACCOUNT_SEQUENCE) +
                mapOf(
                    ITEM_TYPE to AttributeValue.fromS(ACCOUNT_ITEM),
                    CUSTOMER_ID to AttributeValue.fromS(customerId.toString()),
                ),
        )

    override fun accountExists(accountId: UUID): Boolean = getItem(accountId, ACCOUNT_SEQUENCE) != null

    override fun findPage(
        accountId: UUID,
        cursor: StatementCursor?,
        limit: Int,
    ): StatementPage {
        // Nothing is below sequence 1, and DynamoDB rejects a BETWEEN whose lower bound is above the upper one.
        if (cursor != null && cursor.beforeSequence <= FIRST_SEQUENCE) return StatementPage(emptyList(), null)
        // The lower bound keeps the account item (sequence 0) out of the page.
        val values =
            mutableMapOf(
                ":id" to AttributeValue.fromS(accountId.toString()),
                ":first" to AttributeValue.fromN(FIRST_SEQUENCE.toString()),
            )
        val condition =
            if (cursor == null) {
                "$ACCOUNT_ID = :id AND #seq >= :first"
            } else {
                values[":last"] = AttributeValue.fromN((cursor.beforeSequence - 1).toString())
                "$ACCOUNT_ID = :id AND #seq BETWEEN :first AND :last"
            }
        val entries =
            dynamoDb
                .query {
                    it
                        .tableName(table)
                        .keyConditionExpression(condition)
                        // SEQUENCE is a DynamoDB reserved word.
                        .expressionAttributeNames(mapOf("#seq" to SEQUENCE))
                        .expressionAttributeValues(values)
                        .scanIndexForward(false)
                        .limit(limit)
                }.items()
                .map(::toEntry)
        val last = entries.lastOrNull()
        val nextCursor =
            if (entries.size == limit && last != null && last.sequence > FIRST_SEQUENCE) StatementCursor(last.sequence) else null
        return StatementPage(entries, nextCursor)
    }

    /** False when an item with the same key exists: the condition names the partition key, present on every item. */
    private fun putIfAbsent(item: Map<String, AttributeValue>): Boolean =
        try {
            dynamoDb.putItem {
                it
                    .tableName(table)
                    .item(item)
                    .conditionExpression("attribute_not_exists($ACCOUNT_ID)")
            }
            true
        } catch (_: ConditionalCheckFailedException) {
            false
        }

    private fun getItem(
        accountId: UUID,
        sequence: Long,
    ): Map<String, AttributeValue>? =
        dynamoDb
            .getItem {
                it
                    .tableName(table)
                    .key(key(accountId, sequence))
                    .consistentRead(true)
            }.takeIf { it.hasItem() && it.item().isNotEmpty() }
            ?.item()

    private fun key(
        accountId: UUID,
        sequence: Long,
    ): Map<String, AttributeValue> =
        mapOf(
            ACCOUNT_ID to AttributeValue.fromS(accountId.toString()),
            SEQUENCE to AttributeValue.fromN(sequence.toString()),
        )

    private fun toEntry(item: Map<String, AttributeValue>): StatementEntry =
        StatementEntry(
            entryId = UUID.fromString(item.getValue(ENTRY_ID).s()),
            accountId = UUID.fromString(item.getValue(ACCOUNT_ID).s()),
            sequence = item.getValue(SEQUENCE).n().toLong(),
            kind = EntryKind.valueOf(item.getValue(KIND).s()),
            amountCents = item.getValue(AMOUNT).n().toLong(),
            balanceAfterCents = item.getValue(BALANCE_AFTER).n().toLong(),
            description = item[DESCRIPTION]?.s(),
            createdAt = Instant.parse(item.getValue(CREATED_AT).s()),
        )

    private companion object {
        const val ACCOUNT_SEQUENCE = 0L
        const val FIRST_SEQUENCE = 1L
        const val ENTRY_ITEM = "ENTRY"
        const val ACCOUNT_ITEM = "ACCOUNT"

        const val ACCOUNT_ID = "accountId"
        const val SEQUENCE = "sequence"
        const val ITEM_TYPE = "itemType"
        const val ENTRY_ID = "entryId"
        const val KIND = "kind"
        const val AMOUNT = "amount"
        const val BALANCE_AFTER = "balanceAfter"
        const val DESCRIPTION = "description"
        const val CREATED_AT = "createdAt"
        const val CUSTOMER_ID = "customerId"
    }
}
