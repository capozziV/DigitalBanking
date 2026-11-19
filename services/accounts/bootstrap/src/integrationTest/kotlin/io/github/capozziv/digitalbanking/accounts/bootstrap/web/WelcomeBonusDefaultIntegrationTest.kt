package io.github.capozziv.digitalbanking.accounts.bootstrap.web

import io.github.capozziv.digitalbanking.accounts.bootstrap.AccountsIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource

/**
 * Reads the shipped default of `accounts.welcome-bonus.amount` (application.yml): the base class pins it to 0, so this
 * class drops the inherited properties and re-declares only the ones the other tests need, leaving the bonus untouched.
 */
@TestPropertySource(
    inheritProperties = false,
    properties = ["accounts.outbox.relay-enabled=false", "accounts.transfers.timeout=2s"],
)
class WelcomeBonusDefaultIntegrationTest : AccountsIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Test
    fun `a new account starts with the default welcome bonus of 10000 cents`() {
        val response = ApiClient(port).openAccount()

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(201)
        assertThat(response.body!!.get("balance").asLong()).isEqualTo(10_000)
    }
}
