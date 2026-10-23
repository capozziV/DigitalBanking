package io.github.capozziv.digitalbanking.accounts.domain

/**
 * The balance after a credit or debit would go beyond the range of a Long. It is an IllegalArgumentException:
 * the amount requested is invalid for the current balance, and nothing is recorded. The available balance
 * never throws it: it saturates at Long.MAX_VALUE.
 */
class BalanceOverflowException(
    message: String,
    cause: Throwable,
) : IllegalArgumentException(message, cause)
