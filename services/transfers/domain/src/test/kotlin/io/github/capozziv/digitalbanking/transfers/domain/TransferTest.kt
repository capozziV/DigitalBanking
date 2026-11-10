package io.github.capozziv.digitalbanking.transfers.domain

import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.afterDeadline
import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.beforeDeadline
import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.created
import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.transfer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Duration

class TransferTest {
    @Test
    fun `request starts pending at debit with the deadline computed`() {
        val t = transfer()
        assertThat(t.step).isEqualTo(TransferStep.Debit)
        assertThat(t.status).isEqualTo(TransferStatus.PENDING)
        assertThat(t.deadline).isEqualTo(created.plus(Duration.ofMinutes(2)))
        assertThat(t.attempts).isZero()
        assertThat(t.failureReason).isNull()
        assertThat(t.createdAt).isEqualTo(created)
        assertThat(t.updatedAt).isEqualTo(created)
    }

    @Test
    fun `request rejects the same source and destination`() {
        assertThatThrownBy {
            Transfer.request(
                id = TransferFixtures.id,
                sourceAccountId = TransferFixtures.source,
                destinationAccountId = TransferFixtures.source,
                amount = Money(1),
                description = null,
                idempotencyKey = "k",
                correlationId = null,
                now = created,
                deadlineAfter = Duration.ofMinutes(2),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `is past deadline at and after the deadline only`() {
        val t = transfer()
        assertThat(t.isPastDeadline(beforeDeadline)).isFalse()
        assertThat(t.isPastDeadline(t.deadline)).isTrue()
        assertThat(t.isPastDeadline(afterDeadline)).isTrue()
    }

    @Test
    fun `advance to another step resets attempts`() {
        val t = transfer().copy(attempts = 3).advance(StepOutcome.Success, beforeDeadline)
        assertThat(t.step).isEqualTo(TransferStep.Credit)
        assertThat(t.attempts).isZero()
        assertThat(t.updatedAt).isEqualTo(beforeDeadline)
    }

    @Test
    fun `advance in the same step counts an attempt`() {
        val t = transfer().advance(StepOutcome.TransientFailure, beforeDeadline)
        assertThat(t.step).isEqualTo(TransferStep.Debit)
        assertThat(t.attempts).isEqualTo(1)
        assertThat(t.advance(StepOutcome.TransientFailure, beforeDeadline).attempts).isEqualTo(2)
    }

    @Test
    fun `refusal that fails the transfer records the reason`() {
        val t = transfer().advance(StepOutcome.Refused(StepOutcome.INSUFFICIENT_AVAILABLE_BALANCE), beforeDeadline)
        assertThat(t.status).isEqualTo(TransferStatus.FAILED)
        assertThat(t.failureReason).isEqualTo("insufficient-available-balance")
    }

    @Test
    fun `refusal that compensates does not record a failure reason`() {
        val t = transfer(TransferStep.Credit).advance(StepOutcome.Refused("account-not-found"), beforeDeadline)
        assertThat(t.step).isEqualTo(TransferStep.Compensate)
        assertThat(t.failureReason).isNull()
    }

    private val nonTerminalSteps =
        listOf(
            TransferStep.Debit,
            TransferStep.ResolveDebit,
            TransferStep.Credit,
            TransferStep.ResolveCredit,
            TransferStep.Compensate,
        )

    @Test
    fun `non terminal steps are pending and have no terminal event`() {
        nonTerminalSteps.forEach { step ->
            assertThat(transfer(step).status).describedAs(step.name).isEqualTo(TransferStatus.PENDING)
            assertThat(transfer(step).terminalEvent()).describedAs(step.name).isNull()
        }
    }

    @Test
    fun `terminal steps have the matching status and event`() {
        assertThat(transfer(TransferStep.Completed).terminalEvent()).isInstanceOf(TransferEvent.TransferCompleted::class.java)
        assertThat(transfer(TransferStep.Compensated).terminalEvent()).isInstanceOf(TransferEvent.TransferCompensated::class.java)
        assertThat(transfer(TransferStep.Completed).status).isEqualTo(TransferStatus.COMPLETED)
        assertThat(transfer(TransferStep.Failed).status).isEqualTo(TransferStatus.FAILED)
        assertThat(transfer(TransferStep.Compensated).status).isEqualTo(TransferStatus.COMPENSATED)
    }

    @Test
    fun `request rejects a deadline that is not in the future`() {
        listOf(Duration.ZERO, Duration.ofSeconds(-1)).forEach { deadlineAfter ->
            assertThatThrownBy {
                Transfer.request(
                    id = TransferFixtures.id,
                    sourceAccountId = TransferFixtures.source,
                    destinationAccountId = TransferFixtures.destination,
                    amount = Money(1),
                    description = null,
                    idempotencyKey = "k",
                    correlationId = null,
                    now = created,
                    deadlineAfter = deadlineAfter,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun `failed event carries the reason and the payload`() {
        val failed = transfer(TransferStep.Failed).copy(failureReason = "insufficient-available-balance")
        assertThat(failed.terminalEvent()).isEqualTo(
            TransferEvent.TransferFailed(
                transferId = TransferFixtures.id,
                sourceAccountId = TransferFixtures.source,
                destinationAccountId = TransferFixtures.destination,
                amount = Money(500),
                description = "rent",
                reason = "insufficient-available-balance",
            ),
        )
    }

    @Test
    fun `from name round trips the eight steps`() {
        val all =
            listOf(
                TransferStep.Debit,
                TransferStep.ResolveDebit,
                TransferStep.Credit,
                TransferStep.ResolveCredit,
                TransferStep.Compensate,
                TransferStep.Completed,
                TransferStep.Failed,
                TransferStep.Compensated,
            )
        all.forEach { assertThat(TransferStep.fromName(it.name)).isSameAs(it) }
        assertThat(all.map { it.name }.toSet()).hasSize(8)
        assertThatThrownBy { TransferStep.fromName("NOPE") }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
