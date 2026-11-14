package io.github.capozziv.digitalbanking.transfers.adapters.accounts.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.UUID

/** Body of `POST /accounts/{id}/debits` and `/credits`; amount in cents. */
data class AccountsEntryRequestDTO(
    val amount: Long,
    val description: String,
)

/** The part of a ledger entry the client checks to tell a real `accounts` answer from a stray 2xx. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class AccountsEntryResponseDTO(
    val id: UUID,
)

/** The part of an account the client reads on a lookup: who owns it. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class AccountsAccountResponseDTO(
    val id: UUID,
    val customerId: UUID,
)

/** Problem Details (RFC 9457) as sent by `accounts`; only `type` matters here. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class AccountsProblemDTO(
    val type: String? = null,
)
