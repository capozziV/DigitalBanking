package io.github.capozziv.digitalbanking.accounts.bootstrap

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.client.RestClient
import org.springframework.web.filter.OncePerRequestFilter
import java.util.concurrent.atomic.AtomicReference

class AccountsApplicationIntegrationTest : AccountsIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var requestThreadProbe: RequestThreadProbe

    @Test
    fun `health is UP with database UP`() {
        val health = getHealth()

        assertThat(health).containsEntry("status", "UP")
        assertThat(health)
            .extractingByKey("components")
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .extractingByKey("db")
            .asInstanceOf(InstanceOfAssertFactories.MAP)
            .containsEntry("status", "UP")
    }

    @Test
    fun `http requests are served on virtual threads`() {
        getHealth()

        assertThat(requestThreadProbe.lastRequestOnVirtualThread.get()).isTrue()
    }

    private fun getHealth(): Map<String, Any>? =
        RestClient
            .create("http://localhost:$port")
            .get()
            .uri("/actuator/health")
            .retrieve()
            .body(object : ParameterizedTypeReference<Map<String, Any>>() {})

    /** Records whether the servlet container ran the last request on a virtual thread. */
    class RequestThreadProbe : OncePerRequestFilter() {
        val lastRequestOnVirtualThread = AtomicReference<Boolean>()

        override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain,
        ) {
            lastRequestOnVirtualThread.set(Thread.currentThread().isVirtual)
            filterChain.doFilter(request, response)
        }
    }

    @TestConfiguration
    class RequestThreadProbeConfiguration {
        @Bean
        fun requestThreadProbe() = RequestThreadProbe()
    }
}
