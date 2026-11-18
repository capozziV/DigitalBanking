package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.transfers.application.fake.InMemoryTransferRepository
import io.github.capozziv.digitalbanking.transfers.domain.Money
import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import io.github.capozziv.digitalbanking.transfers.domain.TransferStep
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID

class CountPendingTransfersUseCaseTest {
    private val repository = InMemoryTransferRepository()
    private val useCase = CountPendingTransfersUseCase(repository)
    private val account = UUID.randomUUID()

    @Test
    fun `an account that never sent a transfer has none pending`() {
        assertThat(useCase.execute(account)).isZero()
    }

    @Test
    fun `counts only the pending transfers leaving the account`() {
        transfer(source = account, step = TransferStep.ReserveLimit)
        transfer(source = account, step = TransferStep.Credit)
        transfer(source = account, step = TransferStep.Compensate)
        transfer(source = account, step = TransferStep.Completed)
        transfer(source = account, step = TransferStep.Failed)
        transfer(source = account, step = TransferStep.Compensated)
        transfer(source = UUID.randomUUID(), destination = account, step = TransferStep.Credit)
        transfer(source = UUID.randomUUID(), step = TransferStep.Debit)

        assertThat(useCase.execute(account)).isEqualTo(3)
    }

    private fun transfer(
        source: UUID,
        step: TransferStep,
        destination: UUID = UUID.randomUUID(),
    ) {
        val requested =
            Transfer.request(
                UUID.randomUUID(),
                source,
                UUID.randomUUID(),
                destination,
                Money(100),
                null,
                "key-${UUID.randomUUID()}",
                null,
                Instant.parse("2026-11-18T12:00:00Z"),
                Duration.ofMinutes(2),
            )
        repository.insert(requested.copy(step = step))
    }
}
