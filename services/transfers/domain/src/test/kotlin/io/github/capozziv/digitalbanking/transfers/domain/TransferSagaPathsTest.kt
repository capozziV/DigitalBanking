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
    fun `credit refused so the compensated transfer keeps the refusal reason from the compensating step on`() {
        val compensating = run(ok to beforeDeadline, StepOutcome.Refused("account-closed") to beforeDeadline)
        assertThat(compensating.step).isEqualTo(TransferStep.Compensate)
        assertThat(compensating.failureReason).isEqualTo("account-closed")

        val compensated = compensating.advance(ok, beforeDeadline)
        assertThat(compensated.failureReason).isEqualTo("account-closed")
        assertThat((compensated.terminalEvent() as TransferEvent.TransferCompensated).reason).isEqualTo("account-closed")
    }

    @Test
    fun `credit that could not be confirmed so compensated with credit-not-confirmed`() {
        val t =
            run(
                ok to beforeDeadline,
                timeout to beforeDeadline,
                timeout to afterDeadline,
                notFound to afterDeadline,
                ok to afterDeadline,
            )
        assertThat(t.failureReason).isEqualTo(StepOutcome.CREDIT_NOT_CONFIRMED)
    }

    @Test
    fun `debit that took a while but was found so compensated without a reason`() {
        val t = run(timeout to beforeDeadline, timeout to afterDeadline, ok to afterDeadline, ok to afterDeadline)
        assertThat(t.failureReason).isNull()
        assertThat((t.terminalEvent() as TransferEvent.TransferCompensated).reason).isNull()
    }

    @Test
    fun `debit times out until the deadline and the entry exists so compensated`() {
        val t = run(timeout to beforeDeadline, timeout to afterDeadline, ok to afterDeadline, ok to afterDeadline)
        assertThat(t.status).isEqualTo(TransferStatus.COMPENSATED)
    }

    @Test
    fun `debit times out until the deadline and no entry so failed`() {
        val t = run(timeout to beforeDeadline, timeout to afterDeadline, timeout to afterDeadline, notFound to afterDeadline)
        assertThat(t.status).isEqualTo(TransferStatus.FAILED)
        assertThat(t.failureReason).isEqualTo(StepOutcome.DEBIT_NOT_CONFIRMED)
    }

    @Test
    fun `credit times out until the deadline and the entry exists so completed`() {
        val t = run(ok to beforeDeadline, timeout to beforeDeadline, timeout to afterDeadline, ok to afterDeadline)
        assertThat(t.status).isEqualTo(TransferStatus.COMPLETED)
    }

    @Test
    fun `credit times out until the deadline and no entry so compensated`() {
        val t =
            run(
                ok to beforeDeadline,
                timeout to beforeDeadline,
                timeout to afterDeadline,
                notFound to afterDeadline,
                ok to afterDeadline,
            )
        assertThat(t.status).isEqualTo(TransferStatus.COMPENSATED)
    }

    @Test
    fun `transient failure before the deadline stays in the same step`() {
        assertThat(run(timeout to beforeDeadline).step).isEqualTo(TransferStep.Debit)
        assertThat(run(ok to beforeDeadline, timeout to beforeDeadline).step).isEqualTo(TransferStep.Credit)
    }

    @Test
    fun `transient failure after the deadline moves to the resolve step`() {
        assertThat(run(timeout to afterDeadline).step).isEqualTo(TransferStep.ResolveDebit)
        assertThat(run(ok to beforeDeadline, timeout to afterDeadline).step).isEqualTo(TransferStep.ResolveCredit)
    }
}
