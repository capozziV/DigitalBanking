package io.github.capozziv.digitalbanking.transfers.bootstrap.web

import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import io.github.capozziv.digitalbanking.transfers.application.port.TransferRepository
import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import io.github.capozziv.digitalbanking.transfers.bootstrap.persistence.aTransfer
import io.github.capozziv.digitalbanking.transfers.domain.TransferStep
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import java.util.UUID

/**
 * `GET /internal/accounts/{accountId}/pending-transfers`, asked by accounts before closing an account: how many
 * PENDING transfers leave it. A service token only; a customer, even the owner, and an admin are 403.
 */
class PendingTransfersApiIntegrationTest : TransfersIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var transfers: TransferRepository

    private val service by lazy { ApiClient(port).withToken(JwtTestSupport.serviceToken()) }
    private val account = UUID.randomUUID()

    @Test
    fun `a service gets the count of pending transfers leaving the account`() {
        for (step in listOf(TransferStep.ReserveLimit, TransferStep.Debit, TransferStep.Credit, TransferStep.Compensate)) {
            transfers.insert(aTransfer(step = step).copy(sourceAccountId = account))
        }
        for (step in listOf(TransferStep.Completed, TransferStep.Failed, TransferStep.Compensated)) {
            transfers.insert(aTransfer(step = step).copy(sourceAccountId = account))
        }
        transfers.insert(aTransfer(step = TransferStep.Credit).copy(destinationAccountId = account))

        val response = service.get("/internal/accounts/$account/pending-transfers")

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.propertyNames()).containsExactly("count")
        assertThat(response.body.get("count").asLong()).isEqualTo(4)
    }

    @Test
    fun `an account without transfers has a count of zero`() {
        val response = service.get("/internal/accounts/${UUID.randomUUID()}/pending-transfers")

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.get("count").asLong()).isZero()
    }

    @Test
    fun `a customer and an admin are 403 forbidden, no token is 401`() {
        val path = "/internal/accounts/$account/pending-transfers"

        assertProblem(ApiClient(port).get(path), 403, "forbidden")
        assertProblem(ApiClient(port).withToken(JwtTestSupport.adminToken(UUID.randomUUID())).get(path), 403, "forbidden")
        assertProblem(ApiClient(port).withToken(null).get(path), 401, "unauthorized")
    }

    @Test
    fun `an id that is not a UUID is a validation error`() {
        assertProblem(service.get("/internal/accounts/not-a-uuid/pending-transfers"), 400, "validation-error")
    }
}
