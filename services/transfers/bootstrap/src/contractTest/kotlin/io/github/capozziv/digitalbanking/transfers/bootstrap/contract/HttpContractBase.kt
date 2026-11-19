package io.github.capozziv.digitalbanking.transfers.bootstrap.contract

import io.github.capozziv.digitalbanking.observability.ObservabilityAutoConfiguration
import io.github.capozziv.digitalbanking.security.DigitalBankingSecurityAutoConfiguration
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import io.github.capozziv.digitalbanking.transfers.adapters.web.PendingTransfersController
import io.github.capozziv.digitalbanking.transfers.adapters.web.TransfersSecurityConfiguration
import io.github.capozziv.digitalbanking.transfers.application.usecase.CountPendingTransfersUseCase
import io.restassured.module.mockmvc.RestAssuredMockMvc
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.doAnswer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import java.util.UUID

/**
 * Base of the tests generated from contracts/http: `GET /internal/accounts/{id}/pending-transfers`, which accounts
 * asks before closing an account.
 *
 * The real web layer runs (PendingTransfersController and its DTO, the JWT security of libs/security with the rules of
 * transfers, the correlation id filter); [CountPendingTransfersUseCase] is a mock that knows one account with
 * pending transfers, so each contract names its scenario by the account id. Requests carry a machine token signed by
 * [JwtTestSupport] ([serviceAuthorization]): the internal route accepts services only.
 */
@WebMvcTest(controllers = [PendingTransfersController::class])
@ImportAutoConfiguration(DigitalBankingSecurityAutoConfiguration::class, ObservabilityAutoConfiguration::class)
@Import(JwtTestSupport.DecoderConfiguration::class, TransfersSecurityConfiguration::class)
abstract class HttpContractBase {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var countPendingTransfers: CountPendingTransfersUseCase

    @BeforeEach
    fun setUpPendingTransfers() {
        RestAssuredMockMvc.mockMvc(mockMvc)
        doAnswer { COUNTS[it.getArgument<UUID>(0)] ?: 0L }.`when`(countPendingTransfers).execute(anyUuid())
    }

    @AfterEach
    fun resetRestAssured() {
        RestAssuredMockMvc.reset()
    }

    /** Value of the Authorization header of a call by another service: a machine token with the internal scope. */
    fun serviceAuthorization(): String = "Bearer ${JwtTestSupport.serviceToken("accounts")}"

    private companion object {
        /** The accounts of the contracts; any other id has no pending transfer. */
        val COUNTS: Map<UUID, Long> = mapOf(UUID.fromString("0198a1b2-0000-7000-8000-0000000000d1") to 3L)

        // A Mockito matcher returns null, which Kotlin refuses for a non-null parameter: register the matcher and hand
        // Kotlin a placeholder instead.
        fun anyUuid(): UUID = ArgumentMatchers.any(UUID::class.java) ?: UUID(0, 0)
    }
}
