package io.github.capozziv.digitalbanking.accounts.adapters.web

/**
 * The `Idempotency-Key` header: 1 to 255 characters, not blank. Its absence is detected by Spring
 * (required @RequestHeader) and mapped to missing-idempotency-key by the exception handler.
 */
internal object IdempotencyKeyHeader {
    const val NAME = "Idempotency-Key"
    private const val MAX_LENGTH = 255

    fun require(value: String): String {
        if (value.isBlank() || value.length > MAX_LENGTH) {
            throw InvalidRequestException(listOf(InvalidField(NAME, "must have 1 to $MAX_LENGTH characters and not be blank")))
        }
        return value
    }
}
