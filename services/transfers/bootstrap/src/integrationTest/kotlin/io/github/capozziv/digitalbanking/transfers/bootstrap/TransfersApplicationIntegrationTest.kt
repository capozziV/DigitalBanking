package io.github.capozziv.digitalbanking.transfers.bootstrap

import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.client.RestClient
import java.util.UUID

class TransfersApplicationIntegrationTest : TransfersIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Test
    fun `health is UP with database UP, as an admin sees it`() {
        val health =
            RestClient
                .create("http://localhost:$port")
                .get()
                .uri("/actuator/health")
                .headers { it.setBearerAuth(JwtTestSupport.adminToken(UUID.randomUUID())) }
                .retrieve()
                .body(object : ParameterizedTypeReference<Map<String, Any>>() {})

        assertThat(health).containsEntry("status", "UP")
        assertThat(health)
            .extractingByKey("components")
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .extractingByKey("db")
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsEntry("status", "UP")
    }
}
