package io.github.capozziv.digitalbanking.customer.domain

/**
 * A Brazilian mobile phone number in E.164 (`+55`, a DDD that does not start with 0, the ninth digit 9 and eight
 * more digits). Not verified: the bank only checks the format, and the number is unique among customers.
 */
data class PhoneNumber(
    val value: String,
) {
    init {
        require(PATTERN_REGEX.matches(value)) { "phone must be a Brazilian mobile number in E.164 (+55DD9XXXXXXXX)" }
    }

    override fun toString(): String = value

    companion object {
        /** Anchored, since the web layer publishes it as the OpenAPI `pattern`, which is not implicitly anchored. */
        const val PATTERN = "^\\+55[1-9][0-9]9[0-9]{8}$"

        private val PATTERN_REGEX = Regex(PATTERN)
    }
}
