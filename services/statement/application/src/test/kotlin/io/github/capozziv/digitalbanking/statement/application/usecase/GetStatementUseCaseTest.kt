package io.github.capozziv.digitalbanking.statement.application.usecase

import io.github.capozziv.digitalbanking.security.Caller
import io.github.capozziv.digitalbanking.statement.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.statement.application.fake.InMemoryStatementFirstPageCache
import io.github.capozziv.digitalbanking.statement.application.fake.InMemoryStatementRepository
import io.github.capozziv.digitalbanking.statement.application.fake.statementEntry
import io.github.capozziv.digitalbanking.statement.domain.StatementCursor
import io.github.capozziv.digitalbanking.statement.domain.StatementPage
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID

class GetStatementUseCaseTest {
    private val repository = InMemoryStatementRepository()
    private val cache = InMemoryStatementFirstPageCache()
    private val useCase = GetStatementUseCase(repository, cache)
    private val account = UUID.randomUUID()
    private val owner = UUID.randomUUID()
    private val admin = Caller.Admin(UUID.randomUUID())

    private fun statement(
        accountId: UUID,
        cursor: StatementCursor?,
        limit: Int,
        caller: Caller = admin,
    ) = useCase.execute(accountId, cursor, limit, caller)

    @Test
    fun `unknown account without entries is not found`() {
        val error = catchThrowableOfType(AccountNotFoundException::class.java) { statement(account, null, 20) }

        assertThat(error.accountId).isEqualTo(account)
    }

    @Test
    fun `known account without entries is an empty page`() {
        repository.insertAccount(account, UUID.randomUUID())

        val page = statement(account, null, 20)

        assertThat(page.entries).isEmpty()
        assertThat(page.nextCursor).isNull()
    }

    @Test
    fun `entries without AccountOpened are listed`() {
        (1L..3L).forEach { repository.insertEntry(statementEntry(account, it)) }

        val page = statement(account, null, 20)

        assertThat(page.entries.map { it.sequence }).containsExactly(3L, 2L, 1L)
        assertThat(page.nextCursor).isNull()
    }

    @Test
    fun `a full page points to the next one and the last page has no cursor`() {
        (1L..5L).forEach { repository.insertEntry(statementEntry(account, it)) }

        val first = statement(account, null, 2)
        val second = statement(account, first.nextCursor, 2)
        val third = statement(account, second.nextCursor, 2)

        assertThat(first.entries.map { it.sequence }).containsExactly(5L, 4L)
        assertThat(first.nextCursor).isEqualTo(StatementCursor(4))
        assertThat(second.entries.map { it.sequence }).containsExactly(3L, 2L)
        assertThat(third.entries.map { it.sequence }).containsExactly(1L)
        assertThat(third.nextCursor).isNull()
    }

    @Test
    fun `a full page ending at sequence 1 has no next cursor`() {
        (1L..2L).forEach { repository.insertEntry(statementEntry(account, it)) }

        val page = statement(account, null, 2)

        assertThat(page.entries.map { it.sequence }).containsExactly(2L, 1L)
        assertThat(page.nextCursor).isNull()
    }

    @Test
    fun `cursor past the last page is an empty page`() {
        (1L..3L).forEach { repository.insertEntry(statementEntry(account, it)) }

        val afterLast = statement(account, StatementCursor(1), 20)

        assertThat(afterLast.entries).isEmpty()
        assertThat(afterLast.nextCursor).isNull()
    }

    @Test
    fun `cursor on an account without entries is an empty page, not a not found`() {
        val unknown = UUID.randomUUID()

        listOf(StatementCursor(1), StatementCursor(999)).forEach { cursor ->
            val page = statement(unknown, cursor, 20)

            assertThat(page.entries).isEmpty()
            assertThat(page.nextCursor).isNull()
        }
    }

    @Test
    fun `cursor above the last sequence lists from the newest entry`() {
        (1L..3L).forEach { repository.insertEntry(statementEntry(account, it)) }

        val page = statement(account, StatementCursor(999), 20)

        assertThat(page.entries.map { it.sequence }).containsExactly(3L, 2L, 1L)
        assertThat(page.nextCursor).isNull()
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1, 101])
    fun `limit outside 1 to 100 is rejected`(limit: Int) {
        repository.insertAccount(account, UUID.randomUUID())

        assertThatThrownBy { statement(account, null, limit) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @ParameterizedTest
    @ValueSource(ints = [1, 100])
    fun `limit on the edges of 1 to 100 is accepted`(limit: Int) {
        repository.insertAccount(account, UUID.randomUUID())

        assertThat(statement(account, null, limit).entries).isEmpty()
    }

    @Test
    fun `a cached first page is returned without reading the repository`() {
        val cached = StatementPage(listOf(statementEntry(account, 7)), null)
        cache.seed(account, cached)

        val page = statement(account, null, GetStatementUseCase.DEFAULT_LIMIT)

        assertThat(page).isEqualTo(cached)
    }

    @Test
    fun `a miss reads the repository and caches the page`() {
        (1L..3L).forEach { repository.insertEntry(statementEntry(account, it)) }

        val page = statement(account, null, GetStatementUseCase.DEFAULT_LIMIT)

        assertThat(cache.puts).isEqualTo(1)
        assertThat(cache.peek(account)).isEqualTo(page)
    }

    @Test
    fun `an empty first page of a known account is cached`() {
        repository.insertAccount(account, UUID.randomUUID())

        val page = statement(account, null, GetStatementUseCase.DEFAULT_LIMIT)

        assertThat(page.entries).isEmpty()
        assertThat(cache.puts).isEqualTo(1)
        assertThat(cache.peek(account)).isEqualTo(page)
    }

    @Test
    fun `an unknown account is not cached`() {
        repeat(2) {
            assertThatThrownBy { statement(account, null, GetStatementUseCase.DEFAULT_LIMIT) }
                .isInstanceOf(AccountNotFoundException::class.java)
        }

        assertThat(cache.puts).isEqualTo(0)
    }

    @Test
    fun `a cursor never touches the cache`() {
        (1L..3L).forEach { repository.insertEntry(statementEntry(account, it)) }

        statement(account, StatementCursor(3), GetStatementUseCase.DEFAULT_LIMIT)

        assertThat(cache.gets).isEqualTo(0)
        assertThat(cache.puts).isEqualTo(0)
    }

    @Test
    fun `a limit other than the default never touches the cache`() {
        (1L..3L).forEach { repository.insertEntry(statementEntry(account, it)) }

        statement(account, null, GetStatementUseCase.DEFAULT_LIMIT + 1)

        assertThat(cache.gets).isEqualTo(0)
        assertThat(cache.puts).isEqualTo(0)
    }

    @Test
    fun `the owner customer reads the statement`() {
        repository.insertAccount(account, owner)
        (1L..3L).forEach { repository.insertEntry(statementEntry(account, it)) }

        val page = statement(account, null, 20, Caller.Customer(owner))

        assertThat(page.entries.map { it.sequence }).containsExactly(3L, 2L, 1L)
    }

    @Test
    fun `another customer gets not found, the same as an unknown account`() {
        repository.insertAccount(account, owner)
        repository.insertEntry(statementEntry(account, 1))

        val error =
            catchThrowableOfType(AccountNotFoundException::class.java) {
                statement(account, null, 20, Caller.Customer(UUID.randomUUID()))
            }

        assertThat(error.accountId).isEqualTo(account)
    }

    @Test
    fun `another customer gets not found on a cursor page too`() {
        repository.insertAccount(account, owner)
        (1L..3L).forEach { repository.insertEntry(statementEntry(account, it)) }

        assertThatThrownBy { statement(account, StatementCursor(3), 20, Caller.Customer(UUID.randomUUID())) }
            .isInstanceOf(AccountNotFoundException::class.java)
    }

    @Test
    fun `a customer asking for an unknown account gets not found`() {
        assertThatThrownBy { statement(account, null, 20, Caller.Customer(owner)) }
            .isInstanceOf(AccountNotFoundException::class.java)
    }

    @Test
    fun `entries of an account without AccountOpened are not shown to a customer because the owner is unknown`() {
        repository.insertEntry(statementEntry(account, 1))

        assertThatThrownBy { statement(account, null, 20, Caller.Customer(owner)) }
            .isInstanceOf(AccountNotFoundException::class.java)
    }

    @Test
    fun `an admin reads the statement of any customer`() {
        repository.insertAccount(account, owner)
        repository.insertEntry(statementEntry(account, 1))

        val page = statement(account, null, 20, Caller.Admin(UUID.randomUUID()))

        assertThat(page.entries).hasSize(1)
    }

    @Test
    fun `a service caller gets not found`() {
        repository.insertAccount(account, owner)

        assertThatThrownBy { statement(account, null, 20, Caller.Service) }.isInstanceOf(AccountNotFoundException::class.java)
    }

    @Test
    fun `a cached first page is not served to a customer who is not the owner`() {
        repository.insertAccount(account, owner)
        cache.seed(account, StatementPage(listOf(statementEntry(account, 7)), null))

        assertThatThrownBy { statement(account, null, GetStatementUseCase.DEFAULT_LIMIT, Caller.Customer(UUID.randomUUID())) }
            .isInstanceOf(AccountNotFoundException::class.java)
        assertThat(statement(account, null, GetStatementUseCase.DEFAULT_LIMIT, Caller.Customer(owner)).entries).hasSize(1)
    }
}
