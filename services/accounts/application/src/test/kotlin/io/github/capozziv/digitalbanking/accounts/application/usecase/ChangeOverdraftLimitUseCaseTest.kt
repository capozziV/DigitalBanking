package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.fake.CallLog
import io.github.capozziv.digitalbanking.accounts.application.fake.DirectTransactionRunner
import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryAccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.AccountTypeLimit
import io.github.capozziv.digitalbanking.accounts.domain.OverdraftLimit
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ChangeOverdraftLimitUseCaseTest {
    private val seededAt = Instant.parse("2026-10-01T00:00:00Z")
    private val now = Instant.parse("2026-10-22T12:00:00Z")
    private val free = AccountTypeLimit(type = AccountType.FREE, overdraftLimit = OverdraftLimit(0), updatedAt = seededAt)
    private val gold = AccountTypeLimit(type = AccountType.GOLD, overdraftLimit = OverdraftLimit(200000), updatedAt = seededAt)
    private val types = InMemoryAccountTypeRepository(listOf(free, gold))
    private val tx = DirectTransactionRunner()
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `changing the gold limit stores the new limit with updated at from the clock`() {
        val result = ChangeOverdraftLimitUseCase(types, tx, clock).execute(AccountType.GOLD, OverdraftLimit(300000))

        val updated = AccountTypeLimit(type = AccountType.GOLD, overdraftLimit = OverdraftLimit(300000), updatedAt = now)
        assertThat(result).isEqualTo(updated)
        assertThat(types.findAll()).containsExactly(free, updated)
        assertThat(types.findLimit(AccountType.GOLD)).isEqualTo(OverdraftLimit(300000))
    }

    @Test
    fun `changing a limit updates only the type in one transaction`() {
        val log = CallLog()

        ChangeOverdraftLimitUseCase(log.types(types), log.transactions(tx), clock).execute(AccountType.GOLD, OverdraftLimit(300000))

        assertThat(log.calls).containsExactly("begin", "types.updateLimit", "end")
        assertThat(tx.transactions).isEqualTo(1)
    }
}
