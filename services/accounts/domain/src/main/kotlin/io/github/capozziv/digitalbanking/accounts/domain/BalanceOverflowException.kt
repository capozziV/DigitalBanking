package io.github.capozziv.digitalbanking.accounts.domain

/**
 * A balance or available balance would go beyond the range of a Long. It is an IllegalArgumentException:
 * the amount requested is invalid for the current balance, and nothing is recorded.
 */
class BalanceOverflowException(
    message: String,
    cause: Throwable,
) : IllegalArgumentException(message, cause)
