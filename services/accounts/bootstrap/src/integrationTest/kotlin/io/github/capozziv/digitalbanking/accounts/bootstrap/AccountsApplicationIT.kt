package io.github.capozziv.digitalbanking.accounts.bootstrap

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.env.Environment
import org.springframework.web.client.RestClient

class AccountsApplicationIT : AccountsIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var environment: Environment

    @Test
    fun `health is UP with database UP`() {
        val health =
            RestClient
                .create("http://localhost:$port")
                .get()
                .uri("/actuator/health")
                .retrieve()
                .body(object : ParameterizedTypeReference<Map<String, Any>>() {})

        requireNotNull(health)
        assertThat(health).containsEntry("status", "UP")
        val components = health["components"] as Map<*, *>
        assertThat((components["db"] as Map<*, *>)["status"]).isEqualTo("UP")
    }

    @Test
    fun `virtual threads are enabled`() {
        assertThat(environment.getProperty("spring.threads.virtual.enabled")).isEqualTo("true")
    }
}
