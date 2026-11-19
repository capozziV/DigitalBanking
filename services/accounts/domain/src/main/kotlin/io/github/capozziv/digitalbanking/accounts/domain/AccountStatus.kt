package io.github.capozziv.digitalbanking.accounts.domain

/**
 * Whether the account may move money. ACTIVE takes debits and credits; BLOCKED (by an admin) takes credits only;
 * CLOSED (by its owner or an admin, with zero balance) takes neither and never changes again.
 */
enum class AccountStatus { ACTIVE, BLOCKED, CLOSED }
