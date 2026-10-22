package io.github.capozziv.digitalbanking.accounts.domain

@JvmInline
value class OverdraftLimit(
    val cents: Long,
) {
    init {
        require(cents >= 0) { "overdraft limit must not be negative" }
    }
}
