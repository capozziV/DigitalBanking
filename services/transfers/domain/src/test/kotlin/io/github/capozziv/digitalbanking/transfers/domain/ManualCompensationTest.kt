package io.github.capozziv.digitalbanking.transfers.domain

import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.afterDeadline
import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.transfer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ManualCompensationTest {
    @Test
    fun `a transfer in compensate ends compensated, with its compensated event`() {
        val stuck = transfer(TransferStep.Compensate).copy(attempts = 7, limitReserved = true)

        val resolved = stuck.compensateManually(afterDeadline)

        assertThat(resolved.step).isEqualTo(TransferStep.Compensated)
        assertThat(resolved.status).isEqualTo(TransferStatus.COMPENSATED)
        assertThat(resolved.attempts).isZero()
        assertThat(resolved.updatedAt).isEqualTo(afterDeadline)
        assertThat(resolved.failureReason).isNull()
        assertThat(resolved.limitReserved).isTrue()
        assertThat(resolved.terminalEvent()).isInstanceOf(TransferEvent.TransferCompensated::class.java)
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
            assertThatThrownBy { transfer(step).compensateManually(afterDeadline) }
                .describedAs(step.name)
                .isInstanceOf(IllegalStateException::class.java)
        }
    }
}
