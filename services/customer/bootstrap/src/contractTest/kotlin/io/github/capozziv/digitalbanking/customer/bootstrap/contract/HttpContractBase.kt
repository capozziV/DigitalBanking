package io.github.capozziv.digitalbanking.customer.bootstrap.contract

import io.github.capozziv.digitalbanking.customer.adapters.web.CustomerSecurityConfiguration
import io.github.capozziv.digitalbanking.customer.adapters.web.InternalCustomerController
import io.github.capozziv.digitalbanking.customer.application.CustomerNotFoundException
import io.github.capozziv.digitalbanking.customer.application.usecase.GetCustomerUseCase
import io.github.capozziv.digitalbanking.customer.domain.Customer
import io.github.capozziv.digitalbanking.customer.domain.CustomerStatus
import io.github.capozziv.digitalbanking.customer.domain.PhoneNumber
import io.github.capozziv.digitalbanking.observability.ObservabilityAutoConfiguration
import io.github.capozziv.digitalbanking.security.DigitalBankingSecurityAutoConfiguration
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
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
import java.time.Instant
import java.util.UUID

/**
 * Base of the tests generated from contracts/http: the internal lookup `GET /internal/customers/{id}` that accounts and
 * transfers make.
 *
 * The real web layer runs (InternalCustomerController, Problem Details, the JWT security of libs/security, the
 * correlation id filter); [GetCustomerUseCase] is a mock that knows one active and one blocked customer, so each
 * contract names its scenario by the customer id. Requests carry a machine token signed by [JwtTestSupport]
 * ([serviceAuthorization]): the internal routes accept services only.
 */
@WebMvcTest(controllers = [InternalCustomerController::class])
@ImportAutoConfiguration(DigitalBankingSecurityAutoConfiguration::class, ObservabilityAutoConfiguration::class)
@Import(JwtTestSupport.DecoderConfiguration::class, CustomerSecurityConfiguration::class)
abstract class HttpContractBase {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var getCustomer: GetCustomerUseCase

    @BeforeEach
    fun setUpCustomers() {
        RestAssuredMockMvc.mockMvc(mockMvc)
        doAnswer { customer(it.getArgument(0)) }.`when`(getCustomer).execute(anyUuid())
    }

    @AfterEach
    fun resetRestAssured() {
        RestAssuredMockMvc.reset()
    }

    /** Value of the Authorization header of a call by another service: a machine token with the internal scope. */
    fun serviceAuthorization(): String = "Bearer ${JwtTestSupport.serviceToken("accounts")}"

    private fun customer(id: UUID): Customer {
        val status = STATUSES[id] ?: throw CustomerNotFoundException(id)
        return Customer(id, "Contract Customer", "contract@example.test", PhoneNumber("+5511987654321"), status, CREATED_AT, CREATED_AT)
    }

    private companion object {
        /** The customers of the contracts; any other id is a customer that does not exist. */
        val STATUSES: Map<UUID, CustomerStatus> =
            mapOf(
                UUID.fromString("0198a1b2-0000-7000-8000-0000000000c1") to CustomerStatus.ACTIVE,
                UUID.fromString("0198a1b2-0000-7000-8000-0000000000b1") to CustomerStatus.BLOCKED,
            )
        val CREATED_AT: Instant = Instant.parse("2026-10-27T10:00:00Z")

        // A Mockito matcher returns null, which Kotlin refuses for a non-null parameter: register the matcher and hand
        // Kotlin a placeholder instead.
        fun anyUuid(): UUID = ArgumentMatchers.any(UUID::class.java) ?: UUID(0, 0)
    }
}
