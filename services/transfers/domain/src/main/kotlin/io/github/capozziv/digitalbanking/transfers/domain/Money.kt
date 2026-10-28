package io.github.capozziv.digitalbanking.transfers.domain

data class Money(
    val cents: Long,
) {
    init {
        require(cents > 0) { "money must be positive" }
    }
}
