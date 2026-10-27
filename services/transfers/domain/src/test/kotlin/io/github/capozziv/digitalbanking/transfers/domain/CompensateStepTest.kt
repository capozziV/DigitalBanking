package io.github.capozziv.digitalbanking.transfers.domain

import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.afterDeadline
import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.beforeDeadline
import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.transfer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CompensateStepTest {
    private val t = transfer(TransferStep.Compensate)

    @Test
    fun `credits the source account back with the compensation key`() {
        assertThat(TransferStep.Compensate.call(t)).isEqualTo(
            AccountsCall.Credit(
                accountId = TransferFixtures.source,
                amount = Money(500),
                idempotencyKey = "transfer-${TransferFixtures.id}-compensation",
                description = "Transfer ${TransferFixtures.id} compensation",
            ),
        )
    }

    @Test
    fun `success ends compensated`() {
        assertThat(TransferStep.Compensate.onSuccess(t)).isEqualTo(TransferStep.Compensated)
    }

    @Test
    fun `refusal stays in compensate`() {
        assertThat(TransferStep.Compensate.onRefused(t, "bug")).isEqualTo(TransferStep.Compensate)
    }

    @Test
    fun `transient failure stays in compensate before and after the deadline`() {
        assertThat(TransferStep.Compensate.onTransientFailure(t, beforeDeadline)).isEqualTo(TransferStep.Compensate)
        assertThat(TransferStep.Compensate.onTransientFailure(t, afterDeadline)).isEqualTo(TransferStep.Compensate)
    }
}
