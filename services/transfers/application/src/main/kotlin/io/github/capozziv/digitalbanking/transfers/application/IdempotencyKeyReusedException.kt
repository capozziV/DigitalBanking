package io.github.capozziv.digitalbanking.transfers.application

/** The same idempotency key was sent with a different request (source, destination or amount). */
class IdempotencyKeyReusedException(
    val idempotencyKey: String,
) : RuntimeException("idempotency key $idempotencyKey was already used with a different request")
