package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.security.Caller
import io.github.capozziv.digitalbanking.transfers.application.fake.FakeAccountsGateway
import io.github.capozziv.digitalbanking.transfers.application.fake.InMemoryDailyLimitGateway
import io.github.capozziv.digitalbanking.transfers.application.fake.MutableClock
import io.github.capozziv.digitalbanking.transfers.application.port.AccountLookup
import io.github.capozziv.digitalbanking.transfers.domain.DailyLimitDay
import io.github.capozziv.digitalbanking.transfers.domain.Money
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class GetDailyLimitUseCaseTest {
    private val account = UUID.randomUUID()
    private val owner = FakeAccountsGateway.OWNER
    private val limits = InMemoryDailyLimitGateway(Money(500_000))
    private val accounts = FakeAccountsGateway()
    private val clock = MutableClock(Instant.parse("2026-11-12T15:00:00Z"))
    private val useCase = GetDailyLimitUseCase(limits, accounts, clock)

    @Test
    fun `reports the usage of today in Sao Paulo, not in UTC`() {
        // 02:30 UTC of the 13th is 23:30 of the 12th in São Paulo.
        val lateClock = MutableClock(Instant.parse("2026-11-13T02:30:00Z"))
        val today = LocalDate.parse("2026-11-12")
        limits.reserve(UUID.randomUUID(), account, today, Money(300_000))
        limits.reserve(UUID.randomUUID(), account, today.plusDays(1), Money(100_000))

        val outcome = GetDailyLimitUseCase(limits, accounts, lateClock).execute(account, Caller.Customer(owner))

        val usage = (outcome as DailyLimitOutcome.Found).usage
        assertThat(usage.day).isEqualTo(today)
        assertThat(usage.limit).isEqualTo(Money(500_000))
        assertThat(usage.used).isEqualTo(300_000)
        assertThat(usage.available).isEqualTo(200_000)
    }

    @Test
    fun `an account that never transferred has nothing used`() {
        val usage = (useCase.execute(account, Caller.Customer(owner)) as DailyLimitOutcome.Found).usage

        assertThat(usage.day).isEqualTo(DailyLimitDay.of(clock.instant()))
        assertThat(usage.used).isZero()
        assertThat(usage.available).isEqualTo(500_000)
    }

    @Test
    fun `the account of another customer is not found`() {
        accounts.lookupResults[account] = AccountLookup.Found(UUID.randomUUID())

        assertThat(useCase.execute(account, Caller.Customer(owner))).isEqualTo(DailyLimitOutcome.AccountNotFound)
    }

    @Test
    fun `an account that accounts does not know is not found for a customer`() {
        accounts.lookupResults[account] = AccountLookup.NotFound

        assertThat(useCase.execute(account, Caller.Customer(owner))).isEqualTo(DailyLimitOutcome.AccountNotFound)
    }

    @Test
    fun `accounts unavailable is reported`() {
        accounts.lookupResults[account] = AccountLookup.Unavailable

        assertThat(useCase.execute(account, Caller.Customer(owner))).isEqualTo(DailyLimitOutcome.AccountsUnavailable)
    }

    @Test
    fun `an admin sees the limit of any account without asking accounts`() {
        accounts.lookupResults[account] = AccountLookup.Found(UUID.randomUUID())

        assertThat(useCase.execute(account, Caller.Admin(UUID.randomUUID()))).isInstanceOf(DailyLimitOutcome.Found::class.java)
        assertThat(accounts.lookups).isEmpty()
    }

    @Test
    fun `a service is not the owner of any account`() {
        assertThat(useCase.execute(account, Caller.Service)).isEqualTo(DailyLimitOutcome.AccountNotFound)
    }
}
