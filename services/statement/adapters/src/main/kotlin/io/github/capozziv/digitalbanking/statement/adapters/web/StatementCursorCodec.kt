package io.github.capozziv.digitalbanking.statement.adapters.web

import io.github.capozziv.digitalbanking.statement.domain.StatementCursor
import java.util.Base64

/** A cursor that this API did not issue: not Base64, not `v1:<sequence>`, or a sequence below 1. Answered as 400. */
class InvalidCursorException : RuntimeException("invalid cursor")

/** The cursor is opaque to clients: URL-safe Base64 (no padding) of `v1:<beforeSequence>`. */
object StatementCursorCodec {
    private const val PREFIX = "v1:"

    fun encode(cursor: StatementCursor): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString("$PREFIX${cursor.beforeSequence}".toByteArray(Charsets.UTF_8))

    /** Throws [InvalidCursorException] when [value] is not a cursor produced by [encode]. */
    fun decode(value: String): StatementCursor {
        val text =
            try {
                String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
            } catch (e: IllegalArgumentException) {
                throw InvalidCursorException()
            }
        // Only what encode produces: no sign, no leading zero, no spaces. toLongOrNull alone would take "+5" and "05".
        if (!CANONICAL.matches(text)) throw InvalidCursorException()
        val sequence = text.removePrefix(PREFIX).toLongOrNull() ?: throw InvalidCursorException()
        return StatementCursor(sequence)
    }

    private val CANONICAL = Regex("""v1:[1-9][0-9]*""")
}
