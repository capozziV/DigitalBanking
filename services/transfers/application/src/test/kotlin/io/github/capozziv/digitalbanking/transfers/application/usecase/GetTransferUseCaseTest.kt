package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.transfers.application.fake.InMemoryTransferRepository
import io.github.capozziv.digitalbanking.transfers.domain.Money
import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID

class GetTransferUseCaseTest {
    private val repository = InMemoryTransferRepository()
    private val useCase = GetTransferUseCase(repository)

    @Test
    fun `returns the stored transfer`() {
        val transfer =
            Transfer.request(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Money(100),
                null,
                "key",
                null,
                Instant.parse("2026-10-27T12:00:00Z"),
                Duration.ofMinutes(2),
            )
        repository.insert(transfer)

        assertThat(useCase.execute(transfer.id)).isEqualTo(transfer)
    }

    @Test
    fun `returns null for an unknown id`() {
        assertThat(useCase.execute(UUID.randomUUID())).isNull()
    }
}
