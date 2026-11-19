package io.github.capozziv.digitalbanking.accounts.domain

import java.util.UUID

/** A debit on a blocked account: nothing is recorded. */
class AccountBlockedException(
    val accountId: UUID,
) : RuntimeException("account $accountId is blocked")

/** A debit, a credit or a status change on a closed account: nothing is recorded. */
class AccountClosedException(
    val accountId: UUID,
) : RuntimeException("account $accountId is closed")

/** Closing an account whose balance is not exactly zero. */
class BalanceNotZeroException(
    val accountId: UUID,
    val balance: Balance,
) : RuntimeException("account $accountId has balance ${balance.cents}; only a zero balance can be closed")
