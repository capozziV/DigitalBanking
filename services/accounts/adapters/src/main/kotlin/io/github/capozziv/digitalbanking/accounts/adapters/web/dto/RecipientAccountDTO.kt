package io.github.capozziv.digitalbanking.accounts.adapters.web.dto

import java.util.UUID

/** Where a transfer to a customer lands. Only the id: no balance, no type, no status. */
data class RecipientAccountDTO(
    val accountId: UUID,
)
