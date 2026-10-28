package io.github.capozziv.digitalbanking.accounts.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class AccountTest {
    private val accountId = UUID.fromString("0199a0b0-0000-7000-8000-000000000001")
    private val customerId = UUID.fromString("0199a0b0-0000-7000-8000-000000000002")
    private val entryId = UUID.fromString("0199a0b0-0000-7000-8000-000000000003")
    private val now = Instant.parse("2026-10-22T12:00:00Z")
    private val account = Account(accountId, customerId, AccountType.FREE)

    private fun lastEntry(
        sequence: Long,
        balanceAfter: Long,
        accountId: UUID = this.accountId,
    ) = LedgerEntry(
        id = UUID.fromString("0199a0af-0000-7000-8000-000000000009"),
        accountId = accountId,
        sequence = sequence,
        kind = EntryKind.CREDIT,
        amount = Money(1),
        balanceAfter = Balance(balanceAfter),
        description = null,
        idempotencyKey = "previous-key",
        createdAt = Instant.parse("2026-10-21T12:00:00Z"),
    )

    @Test
    fun `opening an account creates a free account and an account opened event with the same ids`() {
        val (opened, event) = Account.open(accountId, customerId)

        assertThat(opened).isEqualTo(Account(accountId, customerId, AccountType.FREE))
        assertThat(event).isEqualTo(AccountOpened(accountId, customerId, AccountType.FREE))
    }

    @Test
    fun `first credit has sequence 1 and balance after equal to the amount`() {
        val recorded = account.credit(null, entryId, Money(1000), "salary", "key-1", now)

        assertThat(recorded.entry).isEqualTo(
            LedgerEntry(
                id = entryId,
                accountId = accountId,
                sequence = 1,
                kind = EntryKind.CREDIT,
                amount = Money(1000),
                balanceAfter = Balance(1000),
                description = "salary",
                idempotencyKey = "key-1",
                createdAt = now,
            ),
        )
        assertThat(recorded.event).isEqualTo(LedgerEntryRecorded(recorded.entry))
        assertThat(recorded.event.accountId).isEqualTo(accountId)
    }

    @Test
    fun `credit after an existing entry continues the sequence and adds to the balance`() {
        val recorded = account.credit(lastEntry(sequence = 3, balanceAfter = 1000), entryId, Money(500), null, "key-2", now)

        assertThat(recorded.entry.sequence).isEqualTo(4)
        assertThat(recorded.entry.balanceAfter).isEqualTo(Balance(1500))
    }

    @Test
    fun `debit within the overdraft limit is recorded and leaves a negative balance`() {
        val result =
            account.debit(lastEntry(sequence = 1, balanceAfter = 1000), OverdraftLimit(500), entryId, Money(1500), "rent", "key-3", now)

        assertThat(result).isInstanceOf(DebitResult.Recorded::class.java)
        val entry = (result as DebitResult.Recorded).recorded.entry
        assertThat(entry.kind).isEqualTo(EntryKind.DEBIT)
        assertThat(entry.amount).isEqualTo(Money(1500))
        assertThat(entry.sequence).isEqualTo(2)
        assertThat(entry.balanceAfter).isEqualTo(Balance(-500))
        assertThat(result.recorded.event).isEqualTo(LedgerEntryRecorded(entry))
    }

    @Test
    fun `debit above the available balance is refused with the available balance`() {
        val result =
            account.debit(lastEntry(sequence = 1, balanceAfter = 1000), OverdraftLimit(500), entryId, Money(1501), null, "key-4", now)

        assertThat(result).isEqualTo(DebitResult.Refused(AvailableBalance(1500)))
    }

    @Test
    fun `debit on an account without entries uses a zero balance`() {
        val result = account.debit(null, OverdraftLimit(0), entryId, Money(1), null, "key-5", now)

        assertThat(result).isEqualTo(DebitResult.Refused(AvailableBalance(0)))
    }

    @Test
    fun `debit of 1 is refused when the available balance is negative after a downgrade`() {
        val result =
            account.debit(lastEntry(sequence = 7, balanceAfter = -800), OverdraftLimit(500), entryId, Money(1), null, "key-6", now)

        assertThat(result).isEqualTo(DebitResult.Refused(AvailableBalance(-300)))
    }

    @Test
    fun `credit on an account below the overdraft limit is still recorded`() {
        val recorded = account.credit(lastEntry(sequence = 7, balanceAfter = -800), entryId, Money(100), null, "key-7", now)

        assertThat(recorded.entry.balanceAfter).isEqualTo(Balance(-700))
        assertThat(recorded.entry.sequence).isEqualTo(8)
    }

    @Test
    fun `credit beyond the maximum balance fails`() {
        assertThatThrownBy {
            account.credit(lastEntry(sequence = 1, balanceAfter = Long.MAX_VALUE), entryId, Money(1), null, "key-8", now)
        }.isInstanceOf(BalanceOverflowException::class.java)
            .hasMessage("balance overflow")
    }

    @Test
    fun `credit rejects a last entry from another account`() {
        val otherAccount = UUID.fromString("0199a0b0-0000-7000-8000-0000000000ff")

        assertThatThrownBy {
            account.credit(lastEntry(sequence = 1, balanceAfter = 0, accountId = otherAccount), entryId, Money(1), null, "key-9", now)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("last entry belongs to another account")
    }

    @Test
    fun `debit rejects a last entry from another account`() {
        val otherAccount = UUID.fromString("0199a0b0-0000-7000-8000-0000000000ff")

        assertThatThrownBy {
            account.debit(
                lastEntry(sequence = 1, balanceAfter = 1000, accountId = otherAccount),
                OverdraftLimit(0),
                entryId,
                Money(1),
                null,
                "key-10",
                now,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("last entry belongs to another account")
    }

    @Test
    fun `changing a free account to gold emits the type change`() {
        val (changed, event) = account.changeType(AccountType.GOLD, OverdraftLimit(100_000))!!

        assertThat(changed).isEqualTo(Account(accountId, customerId, AccountType.GOLD))
        assertThat(event).isEqualTo(AccountTypeChanged(accountId, AccountType.FREE, AccountType.GOLD, OverdraftLimit(100_000)))
    }

    @Test
    fun `changing to the same type emits nothing`() {
        assertThat(account.changeType(AccountType.FREE, OverdraftLimit(0))).isNull()
    }
}
