package io.github.capozziv.digitalbanking.transfers.domain

import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.afterDeadline
import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.beforeDeadline
import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.transfer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ResolveDebitStepTest {
    private val t = transfer(TransferStep.ResolveDebit)

    @Test
    fun `finds the debit entry by its key on the source account`() {
        assertThat(TransferStep.ResolveDebit.call(t)).isEqualTo(
            AccountsCall.FindEntry(TransferFixtures.source, "transfer-${TransferFixtures.id}-debit"),
        )
    }

    @Test
    fun `entry found means the debit happened so compensate`() {
        assertThat(TransferStep.ResolveDebit.onSuccess(t)).isEqualTo(TransferStep.Compensate)
    }

    @Test
    fun `entry not found fails the transfer`() {
        assertThat(TransferStep.ResolveDebit.onRefused(t, StepOutcome.ENTRY_NOT_FOUND)).isEqualTo(TransferStep.Failed)
    }

    @Test
    fun `any other refusal keeps resolving`() {
        assertThat(TransferStep.ResolveDebit.onRefused(t, "other")).isEqualTo(TransferStep.ResolveDebit)
    }

    @Test
    fun `transient failure keeps resolving before and after the deadline`() {
        assertThat(TransferStep.ResolveDebit.onTransientFailure(t, beforeDeadline)).isEqualTo(TransferStep.ResolveDebit)
        assertThat(TransferStep.ResolveDebit.onTransientFailure(t, afterDeadline)).isEqualTo(TransferStep.ResolveDebit)
    }
}
