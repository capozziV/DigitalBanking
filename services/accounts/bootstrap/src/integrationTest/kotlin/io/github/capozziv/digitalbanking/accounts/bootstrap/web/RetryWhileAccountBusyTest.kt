package io.github.capozziv.digitalbanking.accounts.bootstrap.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

class RetryWhileAccountBusyTest {
    private val sleeps = mutableListOf<Duration>()

    @Test
    fun `retries an account-busy response after Retry-After and returns the next response`() {
        val responses = ArrayDeque(listOf(busy(retryAfter = "2"), created()))

        val response = retryWhileAccountBusy(sleep = { sleeps += it }) { responses.removeFirst() }

        assertThat(response.status).isEqualTo(201)
        assertThat(sleeps).containsExactly(Duration.ofSeconds(2))
    }

    @Test
    fun `makes at most three attempts and returns the last account-busy response`() {
        var attempts = 0

        val response =
            retryWhileAccountBusy(sleep = { sleeps += it }) {
                attempts++
                busy(retryAfter = "1")
            }

        assertThat(attempts).isEqualTo(3)
        assertThat(response.status).isEqualTo(503)
        assertThat(sleeps).hasSize(2)
    }

    @Test
    fun `does not retry a 503 of another problem type`() {
        var attempts = 0

        val response =
            retryWhileAccountBusy(sleep = { sleeps += it }) {
                attempts++
                problem(503, "service-unavailable", retryAfter = "1")
            }

        assertThat(attempts).isEqualTo(1)
        assertThat(response.status).isEqualTo(503)
        assertThat(sleeps).isEmpty()
    }

    private fun busy(retryAfter: String) = problem(503, "account-busy", retryAfter)

    private fun created() = ApiResponse(201, HttpHeaders(), "{}", mapper.readTree("{}"))

    private fun problem(
        status: Int,
        slug: String,
        retryAfter: String,
    ): ApiResponse {
        val raw = """{"type":"/problems/$slug","status":$status}"""
        val headers = HttpHeaders().apply { set(HttpHeaders.RETRY_AFTER, retryAfter) }
        return ApiResponse(status, headers, raw, mapper.readTree(raw))
    }

    private companion object {
        val mapper: JsonMapper = JsonMapper.builder().build()
    }
}
