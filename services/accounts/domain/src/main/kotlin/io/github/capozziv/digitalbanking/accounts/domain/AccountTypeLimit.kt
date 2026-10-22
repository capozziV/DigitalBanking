package io.github.capozziv.digitalbanking.accounts.domain

import java.time.Instant

data class AccountTypeLimit(
    val type: AccountType,
    val overdraftLimit: OverdraftLimit,
    val updatedAt: Instant,
)
