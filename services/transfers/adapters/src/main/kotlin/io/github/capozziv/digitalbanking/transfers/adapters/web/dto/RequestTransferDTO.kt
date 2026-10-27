package io.github.capozziv.digitalbanking.transfers.adapters.web.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.util.UUID

// Fields are nullable so that a missing value reaches bean validation and becomes a field error.

/** Amount in cents. */
data class RequestTransferDTO(
    @field:NotNull val sourceAccountId: UUID?,
    @field:NotNull val destinationAccountId: UUID?,
    @field:NotNull @field:Positive val amount: Long?,
    // Same rule as the accounts service: a single line, so the whole C0 range is refused (this also keeps 0x00,
    // which PostgreSQL rejects in text, away from the database).
    @field:Size(max = MAX_DESCRIPTION_LENGTH)
    @field:Pattern(regexp = NO_CONTROL_CHARACTERS, message = "must not contain control characters")
    val description: String?,
) {
    /** An empty or blank description is treated as absent before it reaches the domain. */
    fun normalizedDescription(): String? = description?.takeIf { it.isNotBlank() }

    private companion object {
        const val MAX_DESCRIPTION_LENGTH = 140

        // Anchored because it is published as the OpenAPI `pattern`, which (ECMA-262) is not implicitly anchored.
        const val NO_CONTROL_CHARACTERS = "^[^\\x00-\\x1F]*$"
    }
}
