package io.github.capozziv.digitalbanking.accounts.domain

@JvmInline
value class AvailableBalance(
    val cents: Long,
) {
    fun covers(amount: Money): Boolean = cents >= amount.cents

    companion object {
        fun of(
            balance: Balance,
            limit: OverdraftLimit,
        ): AvailableBalance =
            try {
                AvailableBalance(Math.addExact(balance.cents, limit.cents))
            } catch (e: ArithmeticException) {
                throw BalanceOverflowException("available balance overflow", e)
            }
    }
}
