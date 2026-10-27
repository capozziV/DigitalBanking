package io.github.capozziv.digitalbanking.transfers.bootstrap.web

import io.github.capozziv.digitalbanking.transfers.application.usecase.GetTransferUseCase
import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.`when`
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.UUID

/** An exception nobody expects becomes a generic 500 Problem Details, logged as ERROR with the correlation id. */
@ExtendWith(OutputCaptureExtension::class)
class UnexpectedErrorApiIntegrationTest : TransfersIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @MockitoBean
    private lateinit var getTransfer: GetTransferUseCase

    @Test
    fun `an unexpected exception is 500 internal-error without the internal message, logged as ERROR with the correlation id`(
        output: CapturedOutput,
    ) {
        val id = UUID.randomUUID()
        `when`(getTransfer.execute(id)).thenThrow(IllegalStateException("secret internal detail"))
        val correlationId = UUID.randomUUID().toString()

        val response = ApiClient(port).get("/transfers/$id", mapOf("X-Correlation-Id" to correlationId))

        assertProblem(response, 500, "internal-error")
        assertThat(response.rawBody).doesNotContain("secret internal detail")
        assertThat(response.header("X-Correlation-Id")).isEqualTo(correlationId)
        assertThat(output.out).containsPattern("ERROR.*$correlationId.*unexpected error")
        assertThat(output.out).contains("secret internal detail")
    }
}
