package io.github.capozziv.digitalbanking.accounts.adapters.web

/** The `Idempotency-Key` header: required, 1 to 255 characters, not blank. */
internal object IdempotencyKeyHeader {
    const val NAME = "Idempotency-Key"
    private const val MAX_LENGTH = 255

    fun require(value: String?): String {
        if (value == null) throw MissingIdempotencyKeyException()
        if (value.isBlank() || value.length > MAX_LENGTH) {
            throw InvalidRequestException(listOf(InvalidField(NAME, "must have 1 to $MAX_LENGTH characters and not be blank")))
        }
        return value
    }
}
