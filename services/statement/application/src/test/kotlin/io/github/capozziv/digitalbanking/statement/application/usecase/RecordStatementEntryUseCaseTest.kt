package io.github.capozziv.digitalbanking.statement.application.usecase

import io.github.capozziv.digitalbanking.statement.application.ConflictingStatementEntryException
import io.github.capozziv.digitalbanking.statement.application.fake.InMemoryStatementFirstPageCache
import io.github.capozziv.digitalbanking.statement.application.fake.InMemoryStatementRepository
import io.github.capozziv.digitalbanking.statement.application.fake.statementEntry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import java.util.UUID

class RecordStatementEntryUseCaseTest {
    private val repository = InMemoryStatementRepository()
    private val cache = InMemoryStatementFirstPageCache()
    private val useCase = RecordStatementEntryUseCase(repository, cache)
    private val account = UUID.randomUUID()

    @Test
    fun `records a new entry`() {
        val entry = statementEntry(account, 1)

        assertThat(useCase.execute(entry)).isEqualTo(RecordOutcome.RECORDED)
        assertThat(repository.findEntry(account, 1)).isEqualTo(entry)
    }

    @Test
    fun `same entry twice is a duplicate`() {
        val entry = statementEntry(account, 1)
        useCase.execute(entry)

        assertThat(useCase.execute(entry)).isEqualTo(RecordOutcome.DUPLICATE)
        assertThat(repository.entryCount).isEqualTo(1)
    }

    @Test
    fun `same sequence with another entryId is a conflict`() {
        val stored = statementEntry(account, 1)
        val received = statementEntry(account, 1)
        useCase.execute(stored)

        val error = catchThrowableOfType(ConflictingStatementEntryException::class.java) { useCase.execute(received) }

        assertThat(error.accountId).isEqualTo(account)
        assertThat(error.sequence).isEqualTo(1)
        assertThat(error.storedEntryId).isEqualTo(stored.entryId)
        assertThat(error.receivedEntryId).isEqualTo(received.entryId)
        assertThat(repository.findEntry(account, 1)).isEqualTo(stored)
    }

    @Test
    fun `a recorded entry evicts the first page`() {
        useCase.execute(statementEntry(account, 1))

        assertThat(cache.evictions[account]).isEqualTo(1)
    }

    @Test
    fun `a duplicate entry evicts the first page`() {
        val entry = statementEntry(account, 1)
        useCase.execute(entry)

        useCase.execute(entry)

        assertThat(cache.evictions[account]).isEqualTo(2)
    }

    @Test
    fun `a conflicting entry does not evict`() {
        useCase.execute(statementEntry(account, 1))

        catchThrowableOfType(ConflictingStatementEntryException::class.java) { useCase.execute(statementEntry(account, 1)) }

        assertThat(cache.evictions[account]).isEqualTo(1)
    }
}
