package io.github.capozziv.digitalbanking.accounts.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/** What each account status allows: debits, credits, status changes and closing. */
class AccountStatusRulesTest {
    private val accountId = UUID.fromString("0199a0b0-0000-7000-8000-000000000001")
    private val customerId = UUID.fromString("0199a0b0-0000-7000-8000-000000000002")
    private val entryId = UUID.fromString("0199a0b0-0000-7000-8000-000000000003")
    private val now = Instant.parse("2026-11-16T12:00:00Z")
    private val limit = OverdraftLimit(10_000)

    private fun account(status: AccountStatus) = Account(accountId, customerId, AccountType.FREE, status)

    @Test
    fun `a new account is active`() {
        val (opened, _) = Account.open(accountId, customerId)

        assertThat(opened.status).isEqualTo(AccountStatus.ACTIVE)
    }

    @Test
    fun `a debit on a blocked account is refused with AccountBlockedException`() {
        assertThatThrownBy { account(AccountStatus.BLOCKED).debit(null, limit, entryId, Money(1), null, "k", now) }
            .isInstanceOf(AccountBlockedException::class.java)
            .extracting { (it as AccountBlockedException).accountId }
            .isEqualTo(accountId)
    }

    @Test
    fun `a credit on a blocked account is recorded`() {
        val recorded = account(AccountStatus.BLOCKED).credit(null, entryId, Money(500), null, "k", now)

        assertThat(recorded.entry.balanceAfter).isEqualTo(Balance(500))
    }

    @Test
    fun `a debit on a closed account is refused with AccountClosedException`() {
        assertThatThrownBy { account(AccountStatus.CLOSED).debit(null, limit, entryId, Money(1), null, "k", now) }
            .isInstanceOf(AccountClosedException::class.java)
    }

    @Test
    fun `a credit on a closed account is refused with AccountClosedException`() {
        assertThatThrownBy { account(AccountStatus.CLOSED).credit(null, entryId, Money(1), null, "k", now) }
            .isInstanceOf(AccountClosedException::class.java)
            .extracting { (it as AccountClosedException).accountId }
            .isEqualTo(accountId)
    }

    @Test
    fun `blocking an active account emits AccountBlocked with the customer and the instant`() {
        val (blocked, event) = account(AccountStatus.ACTIVE).changeStatus(AccountStatus.BLOCKED, now)!!

        assertThat(blocked.status).isEqualTo(AccountStatus.BLOCKED)
        assertThat(event).isEqualTo(AccountBlocked(accountId, customerId, now))
    }

    @Test
    fun `unblocking a blocked account emits AccountUnblocked`() {
        val (active, event) = account(AccountStatus.BLOCKED).changeStatus(AccountStatus.ACTIVE, now)!!

        assertThat(active.status).isEqualTo(AccountStatus.ACTIVE)
        assertThat(event).isEqualTo(AccountUnblocked(accountId, customerId, now))
    }

    @Test
    fun `changing to the same status emits nothing`() {
        assertThat(account(AccountStatus.BLOCKED).changeStatus(AccountStatus.BLOCKED, now)).isNull()
        assertThat(account(AccountStatus.ACTIVE).changeStatus(AccountStatus.ACTIVE, now)).isNull()
    }

    @Test
    fun `changing the status of a closed account is refused with AccountClosedException`() {
        for (status in listOf(AccountStatus.ACTIVE, AccountStatus.BLOCKED)) {
            assertThatThrownBy { account(AccountStatus.CLOSED).changeStatus(status, now) }
                .isInstanceOf(AccountClosedException::class.java)
        }
    }

    @Test
    fun `a status change never closes the account`() {
        assertThatThrownBy { account(AccountStatus.ACTIVE).changeStatus(AccountStatus.CLOSED, now) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `closing an account with zero balance emits AccountClosed`() {
        val (closed, event) = account(AccountStatus.ACTIVE).close(Balance.ZERO, now)

        assertThat(closed.status).isEqualTo(AccountStatus.CLOSED)
        assertThat(event).isEqualTo(AccountClosed(accountId, customerId, now))
    }

    @Test
    fun `a blocked account with zero balance can be closed`() {
        val (closed, _) = account(AccountStatus.BLOCKED).close(Balance.ZERO, now)

        assertThat(closed.status).isEqualTo(AccountStatus.CLOSED)
    }

    @Test
    fun `closing with a positive or a negative balance is refused with BalanceNotZeroException`() {
        for (balance in listOf(Balance(1), Balance(-1))) {
            assertThatThrownBy { account(AccountStatus.ACTIVE).close(balance, now) }
                .isInstanceOf(BalanceNotZeroException::class.java)
                .extracting { (it as BalanceNotZeroException).balance }
                .isEqualTo(balance)
        }
    }

    @Test
    fun `closing a closed account is refused with AccountClosedException, even with a balance`() {
        assertThatThrownBy { account(AccountStatus.CLOSED).close(Balance.ZERO, now) }
            .isInstanceOf(AccountClosedException::class.java)
        assertThatThrownBy { account(AccountStatus.CLOSED).close(Balance(5), now) }
            .isInstanceOf(AccountClosedException::class.java)
    }
}
