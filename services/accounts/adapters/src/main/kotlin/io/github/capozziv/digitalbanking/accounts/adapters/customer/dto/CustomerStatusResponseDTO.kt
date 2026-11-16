package io.github.capozziv.digitalbanking.accounts.adapters.customer.dto

import java.util.UUID

/** Body of `GET /internal/customers/{id}` of the `customer` service. Nullable, so a missing field is detected. */
data class CustomerStatusResponseDTO(
    val id: UUID?,
    val status: String?,
)
