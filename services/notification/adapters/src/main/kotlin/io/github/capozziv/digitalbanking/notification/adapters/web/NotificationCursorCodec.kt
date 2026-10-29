package io.github.capozziv.digitalbanking.notification.adapters.web

import io.github.capozziv.digitalbanking.notification.application.port.NotificationCursor
import java.time.Instant
import java.util.Base64
import java.util.UUID

/** The cursor is opaque to clients: URL-safe Base64 (no padding) of `createdAt|id`, with `createdAt` in ISO-8601. */
object NotificationCursorCodec {
    fun encode(cursor: NotificationCursor): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString("${cursor.createdAt}|${cursor.id}".toByteArray(Charsets.UTF_8))

    /** Returns null when [value] is not a cursor produced by [encode] (not Base64, or not `instant|uuid`). */
    fun decode(value: String): NotificationCursor? =
        runCatching {
            val parts = String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8).split('|')
            if (parts.size != 2) return null
            NotificationCursor(Instant.parse(parts[0]), UUID.fromString(parts[1]))
        }.getOrNull()
}
