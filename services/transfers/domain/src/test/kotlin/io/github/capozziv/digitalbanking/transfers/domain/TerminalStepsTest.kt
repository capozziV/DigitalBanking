package io.github.capozziv.digitalbanking.transfers.domain

import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.beforeDeadline
import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.transfer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class TerminalStepsTest {
    private val terminals =
        mapOf(
            TransferStep.Completed to TransferStatus.COMPLETED,
            TransferStep.Failed to TransferStatus.FAILED,
            TransferStep.Compensated to TransferStatus.COMPENSATED,
        )

    @Test
    fun `terminal steps map to their status and make no call`() {
        terminals.forEach { (step, status) ->
            assertThat(step.status).isEqualTo(status)
            assertThat(step.call(transfer(step))).isNull()
        }
    }

    @Test
    fun `terminal steps reject every transition`() {
        terminals.keys.forEach { step ->
            val t = transfer(step)
            assertThatThrownBy { step.onSuccess(t) }.isInstanceOf(IllegalStateException::class.java)
            assertThatThrownBy { step.onRefused(t, "x") }.isInstanceOf(IllegalStateException::class.java)
            assertThatThrownBy { step.onTransientFailure(t, beforeDeadline) }.isInstanceOf(IllegalStateException::class.java)
        }
    }
}
