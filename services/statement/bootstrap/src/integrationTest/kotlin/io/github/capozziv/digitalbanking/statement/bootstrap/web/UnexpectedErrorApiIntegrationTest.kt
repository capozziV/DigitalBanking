package io.github.capozziv.digitalbanking.statement.bootstrap.web

import io.github.capozziv.digitalbanking.statement.application.port.StatementRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doThrow
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.util.UUID

/** An exception nobody expects becomes a generic 500 Problem Details, logged as ERROR with the correlation id. */
@ExtendWith(OutputCaptureExtension::class)
class UnexpectedErrorApiIntegrationTest : StatementWebIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @MockitoSpyBean
    private lateinit var repository: StatementRepository

    @Test
    fun `an unexpected exception is 500 internal-error without the internal message, logged as ERROR with the correlation id`(
        output: CapturedOutput,
    ) {
        doThrow(IllegalStateException("secret internal detail"))
            .`when`(repository)
            .accountExists(any(UUID::class.java) ?: UUID.randomUUID())
        val correlationId = UUID.randomUUID().toString()

        val response = ApiClient(port).get("/accounts/${UUID.randomUUID()}/statement", mapOf("X-Correlation-Id" to correlationId))

        assertProblem(response, 500, "internal-error")
        assertThat(response.rawBody).doesNotContain("secret internal detail")
        assertThat(response.header("X-Correlation-Id")).isEqualTo(correlationId)
        assertThat(output.out).containsPattern("ERROR.*$correlationId.*unexpected error")
        assertThat(output.out).contains("secret internal detail")
    }
}
