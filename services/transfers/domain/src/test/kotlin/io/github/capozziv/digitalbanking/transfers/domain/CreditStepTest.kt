package io.github.capozziv.digitalbanking.transfers.domain

import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.afterDeadline
import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.beforeDeadline
import io.github.capozziv.digitalbanking.transfers.domain.TransferFixtures.transfer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CreditStepTest {
    private val t = transfer(TransferStep.Credit)

    @Test
    fun `calls a credit on the destination account with the credit key`() {
        assertThat(TransferStep.Credit.call(t)).isEqualTo(
            AccountsCall.Credit(
                accountId = TransferFixtures.destination,
                amount = Money(500),
                idempotencyKey = "transfer-${TransferFixtures.id}-credit",
                description = "Transfer from ${TransferFixtures.source}",
            ),
        )
    }

    @Test
    fun `success completes`() {
        assertThat(TransferStep.Credit.onSuccess(t)).isEqualTo(TransferStep.Completed)
    }

    @Test
    fun `refusal compensates`() {
        assertThat(TransferStep.Credit.onRefused(t, "account-not-found")).isEqualTo(TransferStep.Compensate)
    }

    @Test
    fun `transient failure before the deadline retries the credit`() {
        assertThat(TransferStep.Credit.onTransientFailure(t, beforeDeadline)).isEqualTo(TransferStep.Credit)
    }

    @Test
    fun `transient failure after the deadline resolves the credit`() {
        assertThat(TransferStep.Credit.onTransientFailure(t, afterDeadline)).isEqualTo(TransferStep.ResolveCredit)
    }
}
