package io.github.capozziv.digitalbanking.statement.adapters.web

import io.github.capozziv.digitalbanking.statement.domain.StatementCursor
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.Base64

class StatementCursorCodecTest {
    @Test
    fun `a cursor survives encode and decode`() {
        val cursor = StatementCursor(42)

        assertThat(StatementCursorCodec.decode(StatementCursorCodec.encode(cursor))).isEqualTo(cursor)
    }

    @Test
    fun `text that is not a cursor is rejected`() {
        assertThatThrownBy { StatementCursorCodec.decode("abc") }.isInstanceOf(InvalidCursorException::class.java)
        assertThatThrownBy { StatementCursorCodec.decode("%%%") }.isInstanceOf(InvalidCursorException::class.java)
    }

    @Test
    fun `a sequence below 1 is rejected`() {
        assertThatThrownBy { StatementCursorCodec.decode(base64("v1:0")) }.isInstanceOf(InvalidCursorException::class.java)
        assertThatThrownBy { StatementCursorCodec.decode(base64("v1:-3")) }.isInstanceOf(InvalidCursorException::class.java)
    }

    @Test
    fun `a sequence that is not a number is rejected`() {
        assertThatThrownBy { StatementCursorCodec.decode(base64("v1:x")) }.isInstanceOf(InvalidCursorException::class.java)
    }

    @Test
    fun `a sequence that is not canonical is rejected`() {
        listOf("v1:+5", "v1:05", "v1: 5", "v1:5 ", "v1:").forEach { text ->
            assertThatThrownBy { StatementCursorCodec.decode(base64(text)) }
                .describedAs(text)
                .isInstanceOf(InvalidCursorException::class.java)
        }
    }

    @Test
    fun `an unknown version is rejected`() {
        assertThatThrownBy { StatementCursorCodec.decode(base64("v2:5")) }.isInstanceOf(InvalidCursorException::class.java)
    }

    private fun base64(text: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(text.toByteArray(Charsets.UTF_8))
}
