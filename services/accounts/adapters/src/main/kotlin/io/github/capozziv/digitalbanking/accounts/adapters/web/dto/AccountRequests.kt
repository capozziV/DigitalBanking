package io.github.capozziv.digitalbanking.accounts.adapters.web.dto

import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntry
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.util.UUID

// Fields are nullable so that a missing value reaches bean validation and becomes a field error.

data class OpenAccountRequest(
    @field:NotNull val customerId: UUID?,
)

data class RecordEntryRequest(
    @field:NotNull @field:Positive val amount: Long?,
    // PostgreSQL rejects 0x00 in text; C0 control characters are refused before reaching the database.
    @field:Size(max = LedgerEntry.MAX_DESCRIPTION_LENGTH)
    @field:Pattern(regexp = NO_CONTROL_CHARACTERS)
    val description: String?,
) {
    /** An empty or blank description is treated as absent before it reaches the domain. */
    fun normalizedDescription(): String? = description?.takeIf { it.isNotBlank() }

    private companion object {
        const val NO_CONTROL_CHARACTERS = "[^\\x00-\\x1F]*"
    }
}

data class ChangeAccountTypeRequest(
    @field:NotNull val type: AccountType?,
)

data class ChangeOverdraftLimitRequest(
    @field:NotNull @field:PositiveOrZero val overdraftLimit: Long?,
)
