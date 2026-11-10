package io.github.capozziv.digitalbanking.notification.adapters.web.dto

import jakarta.validation.constraints.NotNull
import java.util.UUID

// Fields are nullable so that a missing value reaches bean validation and becomes a field error.

/** Marks as read the notifications of `customerId` up to `upTo`, inclusive. */
data class MarkNotificationsReadDTO(
    @field:NotNull val customerId: UUID?,
    @field:NotNull val upTo: UUID?,
)

/** How many notifications changed; zero on a repetition. */
data class MarkedNotificationsDTO(
    val marked: Int,
)
