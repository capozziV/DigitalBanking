package io.github.capozziv.digitalbanking.transfers.adapters.customer.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/** The part of `GET /internal/customers/{id}` the client reads: the status (`ACTIVE` or `BLOCKED`). */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CustomerStatusResponseDTO(
    val status: String? = null,
)

/** Problem Details (RFC 9457) as sent by `customer`; only `type` matters here. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CustomerProblemDTO(
    val type: String? = null,
)
