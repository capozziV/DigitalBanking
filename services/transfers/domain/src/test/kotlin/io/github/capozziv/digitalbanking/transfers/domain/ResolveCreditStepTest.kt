package io.github.capozziv.digitalbanking.transfers.domain

import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.afterDeadline
import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.beforeDeadline
import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.transfer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ResolveCreditStepTest {
    private val t = transfer(TransferStep.ResolveCredit)

    @Test
    fun `finds the credit entry by its key on the destination account`() {
        assertThat(TransferStep.ResolveCredit.call(t)).isEqualTo(
            AccountsCall.FindEntry(TransferFixtures.destination, "transfer-${TransferFixtures.id}-credit"),
        )
    }

    @Test
    fun `entry found completes`() {
        assertThat(TransferStep.ResolveCredit.onSuccess(t)).isEqualTo(TransferStep.Completed)
    }

    @Test
    fun `entry not found compensates`() {
        assertThat(TransferStep.ResolveCredit.onRefused(t, StepOutcome.ENTRY_NOT_FOUND)).isEqualTo(TransferStep.Compensate)
    }

    @Test
    fun `any other refusal keeps resolving`() {
        assertThat(TransferStep.ResolveCredit.onRefused(t, "other")).isEqualTo(TransferStep.ResolveCredit)
    }

    @Test
    fun `transient failure keeps resolving before and after the deadline`() {
        assertThat(TransferStep.ResolveCredit.onTransientFailure(t, beforeDeadline)).isEqualTo(TransferStep.ResolveCredit)
        assertThat(TransferStep.ResolveCredit.onTransientFailure(t, afterDeadline)).isEqualTo(TransferStep.ResolveCredit)
    }
}
