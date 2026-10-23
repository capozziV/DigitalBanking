package io.github.capozziv.digitalbanking.accounts.domain

@JvmInline
value class AvailableBalance(
    val cents: Long,
) {
    fun covers(amount: Money): Boolean = cents >= amount.cents

    companion object {
        /**
         * Balance plus limit, saturated at Long.MAX_VALUE: no debit can exceed that, so a balance near the
         * maximum stays readable instead of failing. The limit is never negative, so there is no lower bound.
         */
        fun of(
            balance: Balance,
            limit: OverdraftLimit,
        ): AvailableBalance =
            AvailableBalance(
                if (balance.cents > Long.MAX_VALUE - limit.cents) Long.MAX_VALUE else balance.cents + limit.cents,
            )
    }
}
