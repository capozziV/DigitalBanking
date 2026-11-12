package io.github.capozziv.digitalbanking.statement.application.usecase

import io.github.capozziv.digitalbanking.statement.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.statement.application.fake.InMemoryStatementRepository
import io.github.capozziv.digitalbanking.statement.application.fake.statementEntry
import io.github.capozziv.digitalbanking.statement.domain.StatementCursor
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID

class GetStatementUseCaseTest {
    private val repository = InMemoryStatementRepository()
    private val useCase = GetStatementUseCase(repository)
    private val account = UUID.randomUUID()

    @Test
    fun `unknown account without entries is not found`() {
        val error = catchThrowableOfType(AccountNotFoundException::class.java) { useCase.execute(account, null, 20) }

        assertThat(error.accountId).isEqualTo(account)
    }

    @Test
    fun `known account without entries is an empty page`() {
        repository.insertAccount(account, UUID.randomUUID())

        val page = useCase.execute(account, null, 20)

        assertThat(page.entries).isEmpty()
        assertThat(page.nextCursor).isNull()
    }

    @Test
    fun `entries without AccountOpened are listed`() {
        (1L..3L).forEach { repository.insertEntry(statementEntry(account, it)) }

        val page = useCase.execute(account, null, 20)

        assertThat(page.entries.map { it.sequence }).containsExactly(3L, 2L, 1L)
        assertThat(page.nextCursor).isNull()
    }

    @Test
    fun `a full page points to the next one and the last page has no cursor`() {
        (1L..5L).forEach { repository.insertEntry(statementEntry(account, it)) }

        val first = useCase.execute(account, null, 2)
        val second = useCase.execute(account, first.nextCursor, 2)
        val third = useCase.execute(account, second.nextCursor, 2)

        assertThat(first.entries.map { it.sequence }).containsExactly(5L, 4L)
        assertThat(first.nextCursor).isEqualTo(StatementCursor(4))
        assertThat(second.entries.map { it.sequence }).containsExactly(3L, 2L)
        assertThat(third.entries.map { it.sequence }).containsExactly(1L)
        assertThat(third.nextCursor).isNull()
    }

    @Test
    fun `a full page ending at sequence 1 has no next cursor`() {
        (1L..2L).forEach { repository.insertEntry(statementEntry(account, it)) }

        val page = useCase.execute(account, null, 2)

        assertThat(page.entries.map { it.sequence }).containsExactly(2L, 1L)
        assertThat(page.nextCursor).isNull()
    }

    @Test
    fun `cursor past the last page is an empty page`() {
        (1L..3L).forEach { repository.insertEntry(statementEntry(account, it)) }

        val afterLast = useCase.execute(account, StatementCursor(1), 20)

        assertThat(afterLast.entries).isEmpty()
        assertThat(afterLast.nextCursor).isNull()
    }

    @Test
    fun `cursor on an account without entries is an empty page, not a not found`() {
        val unknown = UUID.randomUUID()

        listOf(StatementCursor(1), StatementCursor(999)).forEach { cursor ->
            val page = useCase.execute(unknown, cursor, 20)

            assertThat(page.entries).isEmpty()
            assertThat(page.nextCursor).isNull()
        }
    }

    @Test
    fun `cursor above the last sequence lists from the newest entry`() {
        (1L..3L).forEach { repository.insertEntry(statementEntry(account, it)) }

        val page = useCase.execute(account, StatementCursor(999), 20)

        assertThat(page.entries.map { it.sequence }).containsExactly(3L, 2L, 1L)
        assertThat(page.nextCursor).isNull()
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1, 101])
    fun `limit outside 1 to 100 is rejected`(limit: Int) {
        repository.insertAccount(account, UUID.randomUUID())

        assertThatThrownBy { useCase.execute(account, null, limit) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 100])
    fun `limit on the edges of 1 to 100 is accepted`(limit: Int) {
        repository.insertAccount(account, UUID.randomUUID())

        assertThat(useCase.execute(account, null, limit).entries).isEmpty()
    }
}
