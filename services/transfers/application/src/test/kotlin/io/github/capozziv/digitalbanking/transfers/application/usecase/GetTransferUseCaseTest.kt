package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.security.Caller
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
    private val owner = UUID.randomUUID()

    private val transfer =
        Transfer
            .request(
                UUID.randomUUID(),
                UUID.randomUUID(),
                owner,
                UUID.randomUUID(),
                Money(100),
                null,
                "key",
                null,
                Instant.parse("2026-10-27T12:00:00Z"),
                Duration.ofMinutes(2),
            ).also { repository.insert(it) }

    @Test
    fun `the customer that owns the source sees the transfer`() {
        assertThat(useCase.execute(transfer.id, Caller.Customer(owner))).isEqualTo(transfer)
    }

    @Test
    fun `another customer gets nothing, as if the transfer did not exist`() {
        assertThat(useCase.execute(transfer.id, Caller.Customer(UUID.randomUUID()))).isNull()
    }

    @Test
    fun `an admin sees any transfer`() {
        assertThat(useCase.execute(transfer.id, Caller.Admin(UUID.randomUUID()))).isEqualTo(transfer)
    }

    @Test
    fun `a service gets nothing`() {
        assertThat(useCase.execute(transfer.id, Caller.Service)).isNull()
    }

    @Test
    fun `a transfer without a recorded owner is seen only by an admin`() {
        val legacy = transfer.copy(id = UUID.randomUUID(), idempotencyKey = "legacy", sourceCustomerId = null)
        repository.insert(legacy)

        assertThat(useCase.execute(legacy.id, Caller.Customer(owner))).isNull()
        assertThat(useCase.execute(legacy.id, Caller.Admin(owner))).isEqualTo(legacy)
    }

    @Test
    fun `returns null for an unknown id`() {
        assertThat(useCase.execute(UUID.randomUUID(), Caller.Admin(UUID.randomUUID()))).isNull()
    }
}
