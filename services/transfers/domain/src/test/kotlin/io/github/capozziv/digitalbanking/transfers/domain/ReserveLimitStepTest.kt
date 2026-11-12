package io.github.capozziv.digitalbanking.transfers.domain

import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.afterDeadline
import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.beforeDeadline
import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.transfer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ReserveLimitStepTest {
    private val t = transfer(TransferStep.ReserveLimit)

    @Test
    fun `asks to reserve the daily limit of the source account on the limit day`() {
        assertThat(TransferStep.ReserveLimit.call(t)).isEqualTo(
            StepCall.ReserveLimit(
                accountId = TransferFixtures.source,
                day = LocalDate.parse("2026-10-27"),
                transferId = TransferFixtures.id,
                amount = Money(500),
            ),
        )
    }

    @Test
    fun `success goes to debit`() {
        assertThat(TransferStep.ReserveLimit.onSuccess(t)).isEqualTo(TransferStep.Debit)
    }

    @Test
    fun `refusal fails the transfer`() {
        assertThat(TransferStep.ReserveLimit.onRefused(t, StepOutcome.DAILY_LIMIT_EXCEEDED)).isEqualTo(TransferStep.Failed)
    }

    @Test
    fun `transient failure before the deadline retries the reservation`() {
        assertThat(TransferStep.ReserveLimit.onTransientFailure(t, beforeDeadline)).isEqualTo(TransferStep.ReserveLimit)
    }

    @Test
    fun `transient failure after the deadline fails the transfer`() {
        assertThat(TransferStep.ReserveLimit.onTransientFailure(t, afterDeadline)).isEqualTo(TransferStep.Failed)
    }

    @Test
    fun `is pending and persisted as RESERVE_LIMIT`() {
        assertThat(TransferStep.ReserveLimit.status).isEqualTo(TransferStatus.PENDING)
        assertThat(TransferStep.ReserveLimit.name).isEqualTo("RESERVE_LIMIT")
    }

    @Test
    fun `a successful reservation marks the limit as reserved and goes to debit`() {
        val next = t.advance(StepOutcome.Success, beforeDeadline)

        assertThat(next.step).isEqualTo(TransferStep.Debit)
        assertThat(next.limitReserved).isTrue()
        assertThat(next.failureReason).isNull()
    }

    @Test
    fun `an exceeded limit fails the transfer with daily-limit-exceeded and no reservation`() {
        val next = t.advance(StepOutcome.Refused(StepOutcome.DAILY_LIMIT_EXCEEDED), beforeDeadline)

        assertThat(next.status).isEqualTo(TransferStatus.FAILED)
        assertThat(next.failureReason).isEqualTo("daily-limit-exceeded")
        assertThat(next.limitReserved).isFalse()
    }

    @Test
    fun `an unavailable limit before the deadline stays reserving and counts an attempt`() {
        val next = t.advance(StepOutcome.TransientFailure, beforeDeadline)

        assertThat(next.step).isEqualTo(TransferStep.ReserveLimit)
        assertThat(next.attempts).isEqualTo(1)
        assertThat(next.failureReason).isNull()
    }

    @Test
    fun `an unavailable limit after the deadline fails the transfer with daily-limit-unavailable`() {
        val next = t.advance(StepOutcome.TransientFailure, afterDeadline)

        assertThat(next.status).isEqualTo(TransferStatus.FAILED)
        assertThat(next.failureReason).isEqualTo("daily-limit-unavailable")
        assertThat(next.limitReserved).isFalse()
        assertThat(next.terminalEvent()).isEqualTo(
            TransferEvent.TransferFailed(
                transferId = TransferFixtures.id,
                sourceAccountId = TransferFixtures.source,
                destinationAccountId = TransferFixtures.destination,
                amount = Money(500),
                description = "rent",
                reason = "daily-limit-unavailable",
            ),
        )
    }

    @Test
    fun `the reservation mark survives the later steps`() {
        val failedDebit =
            t
                .advance(StepOutcome.Success, beforeDeadline)
                .advance(StepOutcome.Refused(StepOutcome.INSUFFICIENT_AVAILABLE_BALANCE), beforeDeadline)

        assertThat(failedDebit.status).isEqualTo(TransferStatus.FAILED)
        assertThat(failedDebit.failureReason).isEqualTo(StepOutcome.INSUFFICIENT_AVAILABLE_BALANCE)
        assertThat(failedDebit.limitReserved).isTrue()
    }

    @Test
    fun `a transient failure after the deadline in another step leaves the failure reason alone`() {
        val next = transfer(TransferStep.Debit).advance(StepOutcome.TransientFailure, afterDeadline)

        assertThat(next.step).isEqualTo(TransferStep.ResolveDebit)
        assertThat(next.failureReason).isNull()
    }
}
