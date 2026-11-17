package io.github.capozziv.digitalbanking.outbox.relay.web.dto

import io.swagger.v3.oas.annotations.media.Schema

/** Why the admin discards the event; kept in the outbox row with the admin's id and the last error. */
data class DiscardOutboxEventDTO(
    @field:Schema(description = "Why the event is discarded", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 500)
    val reason: String? = null,
)
