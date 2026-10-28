package io.github.capozziv.digitalbanking.transfers.domain

import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.afterDeadline
import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.beforeDeadline
import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.transfer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DebitStepTest {
    private val t = transfer(TransferStep.Debit)

    @Test
    fun `calls a debit on the source account with the debit key`() {
        assertThat(TransferStep.Debit.call(t)).isEqualTo(
            AccountsCall.Debit(
                accountId = TransferFixtures.source,
                amount = Money(500),
                idempotencyKey = "transfer-${TransferFixtures.id}-debit",
                description = "Transfer to ${TransferFixtures.destination}",
            ),
        )
    }

    @Test
    fun `success goes to credit`() {
        assertThat(TransferStep.Debit.onSuccess(t)).isEqualTo(TransferStep.Credit)
    }

    @Test
    fun `refusal fails the transfer`() {
        assertThat(TransferStep.Debit.onRefused(t, "x")).isEqualTo(TransferStep.Failed)
    }

    @Test
    fun `transient failure before the deadline retries the debit`() {
        assertThat(TransferStep.Debit.onTransientFailure(t, beforeDeadline)).isEqualTo(TransferStep.Debit)
    }

    @Test
    fun `transient failure after the deadline resolves the debit`() {
        assertThat(TransferStep.Debit.onTransientFailure(t, afterDeadline)).isEqualTo(TransferStep.ResolveDebit)
    }

    @Test
    fun `is pending`() {
        assertThat(TransferStep.Debit.status).isEqualTo(TransferStatus.PENDING)
    }
}
