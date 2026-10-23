package io.github.capozziv.digitalbanking.accounts.domain

@JvmInline
value class Money(
    val cents: Long,
) {
    init {
        require(cents > 0) { "money must be positive" }
    }
}
