package io.github.capozziv.digitalbanking.accounts.bootstrap.web

import io.github.capozziv.digitalbanking.accounts.application.usecase.ListAccountTypesUseCase
import io.github.capozziv.digitalbanking.accounts.bootstrap.AccountsIntegrationTest
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
class UnexpectedErrorApiIntegrationTest : AccountsIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @MockitoBean
    private lateinit var listAccountTypes: ListAccountTypesUseCase

    @Test
    fun `an unexpected exception is 500 internal-error without the internal message, logged as ERROR with the correlation id`(
        output: CapturedOutput,
    ) {
        `when`(listAccountTypes.execute()).thenThrow(IllegalStateException("secret internal detail"))
        val correlationId = UUID.randomUUID().toString()

        val response = ApiClient(port).get("/admin/account-types", mapOf("X-Correlation-Id" to correlationId))

        assertProblem(response, 500, "internal-error")
        assertThat(response.rawBody).doesNotContain("secret internal detail")
        assertThat(response.header("X-Correlation-Id")).isEqualTo(correlationId)
        assertThat(output.out).containsPattern("ERROR.*$correlationId.*unexpected error")
        assertThat(output.out).contains("secret internal detail")
    }
}
