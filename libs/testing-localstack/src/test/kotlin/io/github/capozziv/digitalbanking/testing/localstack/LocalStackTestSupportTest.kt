package io.github.capozziv.digitalbanking.testing.localstack

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.opentest4j.TestAbortedException

class LocalStackTestSupportTest {
    @Test
    fun `auth token is returned when the variable is set`() {
        assertThat(LocalStackTestSupport.authToken(mapOf("LOCALSTACK_AUTH_TOKEN" to "token-1"))).isEqualTo("token-1")
    }

    @Test
    fun `missing token outside CI aborts the test so it is reported as skipped`() {
        assertThatThrownBy { LocalStackTestSupport.authToken(emptyMap()) }
            .isInstanceOf(TestAbortedException::class.java)
            .hasMessageContaining("LOCALSTACK_AUTH_TOKEN is not set")
    }

    @Test
    fun `blank token outside CI aborts the test`() {
        assertThatThrownBy { LocalStackTestSupport.authToken(mapOf("LOCALSTACK_AUTH_TOKEN" to " ")) }
            .isInstanceOf(TestAbortedException::class.java)
    }

    @Test
    fun `missing token on CI fails instead of skipping`() {
        assertThatThrownBy { LocalStackTestSupport.authToken(mapOf("CI" to "true")) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("LOCALSTACK_AUTH_TOKEN is not set on CI")
    }

    @Test
    fun `container gets the pinned image and the token`() {
        val container = LocalStackTestSupport.container(listOf("sns", "sqs"), mapOf("LOCALSTACK_AUTH_TOKEN" to "token-1"))

        assertThat(container.dockerImageName).isEqualTo("localstack/localstack:2026.10.2")
        assertThat(container.envMap)
            .containsEntry("LOCALSTACK_AUTH_TOKEN", "token-1")
            .doesNotContainKeys("HTTPS_PROXY", "REQUESTS_CA_BUNDLE")
    }

    @Test
    fun `CA bundle without a proxy is ignored`() {
        val container =
            LocalStackTestSupport.container(
                listOf("sns"),
                mapOf("LOCALSTACK_AUTH_TOKEN" to "token-1", "LOCALSTACK_CA_BUNDLE" to "/some/ca.crt"),
            )

        assertThat(container.envMap).doesNotContainKeys("HTTPS_PROXY", "REQUESTS_CA_BUNDLE", "SSL_CERT_FILE")
    }

    @Test
    fun `container goes through the egress proxy and trusts its CA when configured`() {
        val container =
            LocalStackTestSupport.container(
                listOf("sns"),
                mapOf(
                    "LOCALSTACK_AUTH_TOKEN" to "token-1",
                    "LOCALSTACK_HTTPS_PROXY" to "http://172.17.0.1:33624",
                    "LOCALSTACK_CA_BUNDLE" to "/some/ca.crt",
                ),
            )

        assertThat(container.envMap)
            .containsEntry("HTTPS_PROXY", "http://172.17.0.1:33624")
            .containsEntry("NO_PROXY", "localhost,127.0.0.1")
            .containsEntry("REQUESTS_CA_BUNDLE", LocalStackTestSupport.CONTAINER_CA_BUNDLE)
            .containsEntry("SSL_CERT_FILE", LocalStackTestSupport.CONTAINER_CA_BUNDLE)
    }
}
