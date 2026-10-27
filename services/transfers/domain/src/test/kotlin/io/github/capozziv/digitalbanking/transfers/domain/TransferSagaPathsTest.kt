package io.github.capozziv.digitalbanking.transfers.domain

import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.afterDeadline
import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.beforeDeadline
import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.transfer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class TransferSagaPathsTest {
    private val ok = StepOutcome.Success
    private val timeout = StepOutcome.TransientFailure
    private val notFound = StepOutcome.Refused(StepOutcome.ENTRY_NOT_FOUND)

    private fun run(vararg outcomes: Pair<StepOutcome, Instant>): Transfer =
        outcomes.fold(transfer()) { t, (outcome, now) -> t.advance(outcome, now) }

    @Test
    fun `debit and credit succeed so completed`() {
        val t = run(ok to beforeDeadline, ok to beforeDeadline)
        assertThat(t.status).isEqualTo(TransferStatus.COMPLETED)
    }

    @Test
    fun `debit refused for balance so failed with the reason`() {
        val t = run(StepOutcome.Refused(StepOutcome.INSUFFICIENT_AVAILABLE_BALANCE) to beforeDeadline)
        assertThat(t.status).isEqualTo(TransferStatus.FAILED)
        assertThat(t.failureReason).isEqualTo(StepOutcome.INSUFFICIENT_AVAILABLE_BALANCE)
    }

    @Test
    fun `credit refused so compensated`() {
        val t = run(ok to beforeDeadline, StepOutcome.Refused("account-not-found") to beforeDeadline, ok to beforeDeadline)
        assertThat(t.status).isEqualTo(TransferStatus.COMPENSATED)
    }

    @Test
    fun `debit times out until the deadline and the entry exists so compensated`() {
        val t = run(timeout to beforeDeadline, timeout to afterDeadline, ok to afterDeadline, ok to afterDeadline)
        assertThat(t.status).isEqualTo(TransferStatus.COMPENSATED)
    }

    @Test
    fun `debit times out until the deadline and no entry so failed`() {
        val t = run(timeout to afterDeadline, timeout to afterDeadline, notFound to afterDeadline)
        assertThat(t.status).isEqualTo(TransferStatus.FAILED)
        assertThat(t.failureReason).isEqualTo(StepOutcome.DEBIT_NOT_CONFIRMED)
    }

    @Test
    fun `credit times out until the deadline and the entry exists so completed`() {
        val t = run(ok to beforeDeadline, timeout to afterDeadline, ok to afterDeadline)
        assertThat(t.status).isEqualTo(TransferStatus.COMPLETED)
    }

    @Test
    fun `credit times out until the deadline and no entry so compensated`() {
        val t = run(ok to beforeDeadline, timeout to afterDeadline, notFound to afterDeadline, ok to afterDeadline)
        assertThat(t.status).isEqualTo(TransferStatus.COMPENSATED)
    }
}
