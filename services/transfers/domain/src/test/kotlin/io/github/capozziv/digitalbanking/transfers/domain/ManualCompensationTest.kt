package io.github.capozziv.digitalbanking.transfers.domain

import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.afterDeadline
import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.transfer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class ManualCompensationTest {
    private val stuck = transfer(TransferStep.Compensate).copy(attempts = 7, limitReserved = true)
    private val credit = RecordedEntry(stuck.sourceAccountId, EntryKind.CREDIT, stuck.amount)

    @Test
    fun `a transfer in compensate ends compensated, with its compensated event`() {
        val resolved = stuck.compensateManually(credit, afterDeadline)

        assertThat(resolved.step).isEqualTo(TransferStep.Compensated)
        assertThat(resolved.status).isEqualTo(TransferStatus.COMPENSATED)
        assertThat(resolved.attempts).isZero()
        assertThat(resolved.updatedAt).isEqualTo(afterDeadline)
        assertThat(resolved.failureReason).isNull()
        assertThat(resolved.limitReserved).isTrue()
        assertThat(resolved.terminalEvent()).isInstanceOf(TransferEvent.TransferCompensated::class.java)
    }

    @Test
    fun `the credit of the transfer's amount on the source account is the compensation credit`() {
        assertThat(stuck.manualCreditMismatch(credit)).isNull()
    }

    @Test
    fun `a credit on another account is not the compensation credit`() {
        val elsewhere = credit.copy(accountId = UUID.fromString("00000000-0000-7000-8000-0000000000ff"))

        assertThat(stuck.manualCreditMismatch(elsewhere)).contains("not the source account")
        assertThatThrownBy { stuck.compensateManually(elsewhere, afterDeadline) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `a debit is not the compensation credit`() {
        val debit = credit.copy(kind = EntryKind.DEBIT)

        assertThat(stuck.manualCreditMismatch(debit)).contains("DEBIT")
        assertThatThrownBy { stuck.compensateManually(debit, afterDeadline) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `a credit of another amount is not the compensation credit`() {
        val more = credit.copy(amount = Money(stuck.amount.cents + 1))
        val less = credit.copy(amount = Money(stuck.amount.cents - 1))

        assertThat(stuck.manualCreditMismatch(more)).contains("501 cents")
        assertThat(stuck.manualCreditMismatch(less)).contains("499 cents")
        assertThatThrownBy { stuck.compensateManually(more, afterDeadline) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `only a transfer in compensate can be compensated manually`() {
        val others =
            listOf(
                TransferStep.ReserveLimit,
                TransferStep.Debit,
                TransferStep.ResolveDebit,
                TransferStep.Credit,
                TransferStep.ResolveCredit,
                TransferStep.Completed,
                TransferStep.Failed,
                TransferStep.Compensated,
            )
        for (step in others) {
            assertThatThrownBy { transfer(step).compensateManually(credit, afterDeadline) }
                .describedAs(step.name)
                .isInstanceOf(IllegalStateException::class.java)
        }
    }
}
