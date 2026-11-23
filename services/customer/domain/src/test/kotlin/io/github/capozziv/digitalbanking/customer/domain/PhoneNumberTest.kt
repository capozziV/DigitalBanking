package io.github.capozziv.digitalbanking.customer.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class PhoneNumberTest {
    @Test
    fun `a Brazilian mobile number in E164 is accepted`() {
        assertThat(PhoneNumber("+5511987654321").value).isEqualTo("+5511987654321")
        assertThat(PhoneNumber("+5521912345678").value).isEqualTo("+5521912345678")
    }

    @Test
    fun `a number without plus 55, a landline, a DDD starting with 0 or with the wrong length is refused`() {
        listOf(
            "11987654321", // no +55
            "5511987654321", // no plus
            "+551134567890", // landline: the ninth digit is not 9
            "+5501987654321", // DDD starting with 0
            "+551198765432", // 12 digits
            "+55119876543210", // 14 digits
            "+5511987654321 ", // trailing space
            "+55 11 98765-4321", // mask
            "",
        ).forEach {
            assertThatThrownBy { PhoneNumber(it) }.describedAs(it).isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
