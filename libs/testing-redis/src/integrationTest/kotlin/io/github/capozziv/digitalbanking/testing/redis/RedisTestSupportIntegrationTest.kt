package io.github.capozziv.digitalbanking.testing.redis

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

@Testcontainers
class RedisTestSupportIntegrationTest {
    @Test
    fun `the container answers PING`() {
        assertThat(ping(timeoutMillis = 5_000)).isEqualTo("+PONG")
    }

    @Test
    fun `a paused container does not answer within 1 second and answers again after unpause`() {
        RedisTestSupport.pause(redis)
        try {
            val answer = runCatching { ping(timeoutMillis = 1_000) }
            assertThat(answer.exceptionOrNull()).isInstanceOf(SocketTimeoutException::class.java)
        } finally {
            RedisTestSupport.unpause(redis)
        }

        assertThat(ping(timeoutMillis = 5_000)).isEqualTo("+PONG")
    }

    private fun ping(timeoutMillis: Int): String =
        Socket().use { socket ->
            socket.soTimeout = timeoutMillis
            socket.connect(InetSocketAddress(redis.host, redis.getMappedPort(RedisTestSupport.PORT)), timeoutMillis)
            socket.getOutputStream().write("PING\r\n".toByteArray())
            socket.getOutputStream().flush()
            socket.getInputStream().bufferedReader().readLine()
        }

    companion object {
        @Container
        @JvmStatic
        val redis: GenericContainer<*> = RedisTestSupport.container()
    }
}
