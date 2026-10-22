package io.github.capozziv.digitalbanking.accounts.application.usecase

import io.github.capozziv.digitalbanking.accounts.application.fake.InMemoryAccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.application.port.AccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.AccountTypeLimit
import io.github.capozziv.digitalbanking.accounts.domain.OverdraftLimit
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class ListAccountTypesUseCaseTest {
    private val seededAt = Instant.parse("2026-10-01T00:00:00Z")
    private val free = AccountTypeLimit(type = AccountType.FREE, overdraftLimit = OverdraftLimit(0), updatedAt = seededAt)
    private val standard = AccountTypeLimit(type = AccountType.STANDARD, overdraftLimit = OverdraftLimit(50000), updatedAt = seededAt)
    private val gold = AccountTypeLimit(type = AccountType.GOLD, overdraftLimit = OverdraftLimit(200000), updatedAt = seededAt)
    private val black = AccountTypeLimit(type = AccountType.BLACK, overdraftLimit = OverdraftLimit(500000), updatedAt = seededAt)
    private val diamond = AccountTypeLimit(type = AccountType.DIAMOND, overdraftLimit = OverdraftLimit(1000000), updatedAt = seededAt)

    @Test
    fun `lists the five account types in enum order`() {
        val types = InMemoryAccountTypeRepository(listOf(gold, diamond, free, black, standard))

        val result = ListAccountTypesUseCase(types).execute()

        assertThat(result).containsExactly(free, standard, gold, black, diamond)
    }

    @Test
    fun `orders by enum order even when the repository returns another order`() {
        val unordered = InMemoryAccountTypeRepository(listOf(free, standard, gold, black, diamond))
        val reversed =
            object : AccountTypeRepository by unordered {
                override fun findAll(): List<AccountTypeLimit> = unordered.findAll().reversed()
            }

        val result = ListAccountTypesUseCase(reversed).execute()

        assertThat(result).containsExactly(free, standard, gold, black, diamond)
    }
}
