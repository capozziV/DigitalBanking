package io.github.capozziv.digitalbanking.accounts.domain

@JvmInline
value class Balance(
    val cents: Long,
) {
    operator fun plus(m: Money): Balance = Balance(exact { Math.addExact(cents, m.cents) })

    operator fun minus(m: Money): Balance = Balance(exact { Math.subtractExact(cents, m.cents) })

    companion object {
        val ZERO = Balance(0)

        private inline fun exact(operation: () -> Long): Long =
            try {
                operation()
            } catch (e: ArithmeticException) {
                throw BalanceOverflowException("balance overflow", e)
            }
    }
}
