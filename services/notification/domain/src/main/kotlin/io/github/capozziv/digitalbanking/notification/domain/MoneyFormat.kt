package io.github.capozziv.digitalbanking.notification.domain

object MoneyFormat {
    /** Formats [cents] as Brazilian reais using integer arithmetic only. */
    fun brl(cents: Long): String {
        require(cents >= 0) { "cents must not be negative: $cents" }
        val reais = (cents / 100).toString()
        val remainder = (cents % 100).toString().padStart(2, '0')
        val grouped =
            reais
                .reversed()
                .chunked(3)
                .joinToString(".")
                .reversed()
        return "R$ $grouped,$remainder"
    }
}
