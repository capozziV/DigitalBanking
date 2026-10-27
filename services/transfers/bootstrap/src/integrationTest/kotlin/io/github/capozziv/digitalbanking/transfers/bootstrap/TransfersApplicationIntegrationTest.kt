package io.github.capozziv.digitalbanking.transfers.bootstrap

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.client.RestClient

class TransfersApplicationIntegrationTest : TransfersIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Test
    fun `health is UP with database UP`() {
        val health =
            RestClient
                .create("http://localhost:$port")
                .get()
                .uri("/actuator/health")
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
