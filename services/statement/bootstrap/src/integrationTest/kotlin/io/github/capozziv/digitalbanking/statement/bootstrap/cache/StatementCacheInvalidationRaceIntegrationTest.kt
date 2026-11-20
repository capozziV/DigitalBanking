package io.github.capozziv.digitalbanking.statement.bootstrap.cache

import io.github.capozziv.digitalbanking.security.Caller
import io.github.capozziv.digitalbanking.statement.application.port.StatementFirstPageCache
import io.github.capozziv.digitalbanking.statement.application.port.StatementRepository
import io.github.capozziv.digitalbanking.statement.application.usecase.GetStatementUseCase
import io.github.capozziv.digitalbanking.statement.application.usecase.RecordStatementEntryUseCase
import io.github.capozziv.digitalbanking.statement.bootstrap.StatementIntegrationTest
import io.github.capozziv.digitalbanking.statement.domain.EntryKind
import io.github.capozziv.digitalbanking.statement.domain.StatementCursor
import io.github.capozziv.digitalbanking.statement.domain.StatementEntry
import io.github.capozziv.digitalbanking.statement.domain.StatementPage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.util.UUID

/**
 * A read that misses the cache and reads DynamoDB before an entry arrives must not put its old page back after the
 * entry's eviction (#226): with real DynamoDB and Redis, the entry is recorded (and the page evicted) while the read
 * is between its DynamoDB query and its cache write.
 */
class StatementCacheInvalidationRaceIntegrationTest : StatementIntegrationTest() {
    @Autowired
    private lateinit var repository: StatementRepository

    @Autowired
    private lateinit var cache: StatementFirstPageCache

    private val admin = Caller.Admin(UUID.randomUUID())

    @Test
    fun `a page read before an eviction is not cached after it`() {
        val account = UUID.randomUUID()
        repository.insertAccount(account, UUID.randomUUID())
        val record = RecordStatementEntryUseCase(repository, cache)
        val racingRepository =
            object : StatementRepository by repository {
                var raced = false

                override fun findPage(
                    accountId: UUID,
                    cursor: StatementCursor?,
                    limit: Int,
                ): StatementPage {
                    val page = repository.findPage(accountId, cursor, limit)
                    if (!raced) {
                        raced = true
                        // The consumer projects the entry and evicts the page after the read queried DynamoDB.
                        record.execute(entry(account, 1))
                    }
                    return page
                }
            }
        val racingRead = GetStatementUseCase(racingRepository, cache)
        val read = GetStatementUseCase(repository, cache)

        val stale = racingRead.execute(account, null, GetStatementUseCase.DEFAULT_LIMIT, admin)
        val next = read.execute(account, null, GetStatementUseCase.DEFAULT_LIMIT, admin)

        assertThat(stale.entries).isEmpty()
        assertThat(next.entries.map { it.sequence }).containsExactly(1L)
    }

    private fun entry(
        accountId: UUID,
        sequence: Long,
    ) = StatementEntry(
        entryId = UUID.randomUUID(),
        accountId = accountId,
        sequence = sequence,
        kind = EntryKind.CREDIT,
        amountCents = 5_000,
        balanceAfterCents = 5_000,
        description = "welcome bonus",
        createdAt = Instant.parse("2026-11-20T10:00:00Z"),
    )
}
