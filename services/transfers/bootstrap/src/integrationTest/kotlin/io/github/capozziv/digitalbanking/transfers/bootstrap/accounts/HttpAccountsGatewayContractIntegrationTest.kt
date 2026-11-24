package io.github.capozziv.digitalbanking.transfers.bootstrap.accounts

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import io.github.capozziv.digitalbanking.transfers.adapters.accounts.AccountsClientConfiguration
import io.github.capozziv.digitalbanking.transfers.adapters.accounts.AccountsClientProperties
import io.github.capozziv.digitalbanking.transfers.adapters.accounts.AccountsResponseClassifier
import io.github.capozziv.digitalbanking.transfers.adapters.accounts.HttpAccountsGateway
import io.github.capozziv.digitalbanking.transfers.application.port.AccountLookup
import io.github.capozziv.digitalbanking.transfers.application.port.AccountsGateway
import io.github.capozziv.digitalbanking.transfers.application.port.EntryLookup
import io.github.capozziv.digitalbanking.transfers.bootstrap.ServiceTokenServers
import io.github.capozziv.digitalbanking.transfers.domain.EntryKind
import io.github.capozziv.digitalbanking.transfers.domain.Money
import io.github.capozziv.digitalbanking.transfers.domain.RecordedEntry
import io.github.capozziv.digitalbanking.transfers.domain.StepCall
import io.github.capozziv.digitalbanking.transfers.domain.StepOutcome
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.observation.ObservationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.slf4j.MDC
import sh.stubborn.contract.stubrunner.StubsMode
import sh.stubborn.contract.stubrunner.junit.StubRunnerExtension
import java.util.UUID

/**
 * The accounts client against the stubs generated from the contracts of accounts
 * (services/accounts/bootstrap/src/contractTest/resources/contracts/http).
 *
 * A stub answers only a request that matches its contract (path, machine token, correlation id, Idempotency-Key,
 * body), so each test also proves the client still sends what accounts expects; a contract changed by accounts
 * changes the stubs and breaks these tests. The failures accounts does not describe in a contract (timeouts, cut
 * connections, stray bodies) stay in [HttpAccountsGatewayIntegrationTest].
 */
class HttpAccountsGatewayContractIntegrationTest {
    private val tokenServer = WireMockServer(options().dynamicPort())
    private lateinit var gateway: AccountsGateway

    /** Admin client of the stub server, to check what the client sent beyond what the contract matches. */
    private lateinit var stubServer: WireMock

    @BeforeEach
    fun setUp() {
        tokenServer.start()
        ServiceTokenServers.stubTokenEndpoint(tokenServer)
        val url = accountsStubs.findStubUrl(ACCOUNTS_STUBS_GROUP, ACCOUNTS_STUBS_ARTIFACT)
        stubServer = WireMock(url.host, url.port)
        stubServer.resetRequests()
        val restClient =
            AccountsClientConfiguration().accountsRestClient(
                AccountsClientProperties(url = url.toString()),
                ServiceTokenServers.clientOf(tokenServer),
                ObservationRegistry.NOOP,
            )
        gateway = HttpAccountsGateway(restClient, AccountsResponseClassifier(SimpleMeterRegistry()))
        MDC.put("correlationId", "corr-contract")
    }

    @AfterEach
    fun tearDown() {
        // A request no stub matched is answered 404 by WireMock, which would pass for a real 404 of accounts.
        val unmatched = stubServer.findAllUnmatchedRequests()
        MDC.clear()
        tokenServer.stop()
        assertThat(unmatched).describedAs("requests that matched no contract").isEmpty()
    }

    @Test
    fun `a debit sends the key, the machine token and the body, and 201 is success`() {
        val result = gateway.perform(StepCall.Debit(ACCOUNT_ID, Money(1_000), "transfer-1-debit", "Transfer to someone"))

        assertThat(result.outcome).isEqualTo(StepOutcome.Success)
        assertThat(result.httpStatus).isEqualTo(201)
        stubServer.verifyThat(
            postRequestedFor(urlEqualTo("/accounts/$ACCOUNT_ID/debits"))
                .withHeader("Idempotency-Key", equalTo("transfer-1-debit"))
                .withHeader("Authorization", equalTo("Bearer ${ServiceTokenServers.SERVICE_TOKEN}"))
                .withHeader("X-Correlation-Id", equalTo("corr-contract"))
                .withRequestBody(equalToJson("""{"amount":1000,"description":"Transfer to someone"}""")),
        )
    }

    @Test
    fun `a debit above the available balance is the insufficient-available-balance refusal`() {
        val result = gateway.perform(StepCall.Debit(ACCOUNT_ID, Money(5_000), "transfer-2-debit", "Transfer to someone"))

        assertThat(result.outcome).isEqualTo(StepOutcome.Refused(StepOutcome.INSUFFICIENT_AVAILABLE_BALANCE))
        assertThat(result.httpStatus).isEqualTo(422)
    }

    @Test
    fun `a credit goes to the credits path and 201 is success`() {
        val result = gateway.perform(StepCall.Credit(ACCOUNT_ID, Money(1_000), "transfer-1-credit", "Transfer from someone"))

        assertThat(result.outcome).isEqualTo(StepOutcome.Success)
        assertThat(result.httpStatus).isEqualTo(201)
        stubServer.verifyThat(
            postRequestedFor(urlEqualTo("/accounts/$ACCOUNT_ID/credits"))
                .withHeader("Idempotency-Key", equalTo("transfer-1-credit"))
                .withHeader("Authorization", equalTo("Bearer ${ServiceTokenServers.SERVICE_TOKEN}"))
                .withHeader("X-Correlation-Id", equalTo("corr-contract")),
        )
    }

    @Test
    fun `a credit repeated with a recorded key answers 200 and is success`() {
        val result = gateway.perform(StepCall.Credit(ACCOUNT_ID, Money(1_000), "transfer-9-credit", "Transfer from someone"))

        assertThat(result.outcome).isEqualTo(StepOutcome.Success)
        assertThat(result.httpStatus).isEqualTo(200)
    }

    @Test
    fun `422 idempotency-key-reused is a definitive refusal with the problem type`() {
        val result = gateway.perform(StepCall.Debit(ACCOUNT_ID, Money(1_000), "transfer-5-debit", "Transfer to someone"))

        assertThat(result.outcome).isEqualTo(StepOutcome.Refused("idempotency-key-reused"))
        assertThat(result.httpStatus).isEqualTo(422)
    }

    @Test
    fun `503 account-busy is transient`() {
        val result = gateway.perform(StepCall.Debit(BUSY_ACCOUNT_ID, Money(1_000), "transfer-6-debit", "Transfer to someone"))

        assertThat(result.outcome).isEqualTo(StepOutcome.TransientFailure)
        assertThat(result.httpStatus).isEqualTo(503)
    }

    @Test
    fun `a credit to a missing account is the account-not-found refusal`() {
        val result = gateway.perform(StepCall.Credit(MISSING_ACCOUNT_ID, Money(1_000), "transfer-3-credit", "Transfer from someone"))

        assertThat(result.outcome).isEqualTo(StepOutcome.Refused("account-not-found"))
        assertThat(result.httpStatus).isEqualTo(404)
    }

    @Test
    fun `find entry sends the key as a query parameter and 200 is success`() {
        val result = gateway.perform(StepCall.FindEntry(ACCOUNT_ID, "transfer-1-debit"))

        assertThat(result.outcome).isEqualTo(StepOutcome.Success)
        stubServer.verifyThat(
            getRequestedFor(urlPathEqualTo("/accounts/$ACCOUNT_ID/entries"))
                .withQueryParam("idempotencyKey", equalTo("transfer-1-debit"))
                .withHeader("Authorization", equalTo("Bearer ${ServiceTokenServers.SERVICE_TOKEN}"))
                .withHeader("X-Correlation-Id", equalTo("corr-contract")),
        )
    }

    @Test
    fun `find entry is the entry-not-found refusal for a key never recorded and account-not-found for a missing account`() {
        assertThat(gateway.perform(StepCall.FindEntry(ACCOUNT_ID, "never-used")).outcome)
            .isEqualTo(StepOutcome.Refused(StepOutcome.ENTRY_NOT_FOUND))
        assertThat(gateway.perform(StepCall.FindEntry(MISSING_ACCOUNT_ID, "transfer-1-debit")).outcome)
            .isEqualTo(StepOutcome.Refused("account-not-found"))
    }

    @Test
    fun `find entry for the manual compensation reads the account, kind and amount of the entry`() {
        assertThat(gateway.findEntry(ACCOUNT_ID, "transfer-1-debit"))
            .isEqualTo(EntryLookup.Found(RecordedEntry(ACCOUNT_ID, EntryKind.DEBIT, Money(1_000))))
    }

    @Test
    fun `find entry for the manual compensation reads a CREDIT entry too`() {
        assertThat(gateway.findEntry(ACCOUNT_ID, "transfer-1-credit"))
            .isEqualTo(EntryLookup.Found(RecordedEntry(ACCOUNT_ID, EntryKind.CREDIT, Money(1_000))))
    }

    @Test
    fun `find entry for the manual compensation is NotFound for entry-not-found and account-not-found`() {
        assertThat(gateway.findEntry(ACCOUNT_ID, "never-used")).isEqualTo(EntryLookup.NotFound)
        assertThat(gateway.findEntry(MISSING_ACCOUNT_ID, "transfer-1-debit")).isEqualTo(EntryLookup.NotFound)
    }

    @Test
    fun `lookup account returns the owner for 200`() {
        assertThat(gateway.lookupAccount(ACCOUNT_ID)).isEqualTo(AccountLookup.Found(OWNER_ID))
        stubServer.verifyThat(
            getRequestedFor(urlEqualTo("/accounts/$ACCOUNT_ID"))
                .withHeader("Authorization", equalTo("Bearer ${ServiceTokenServers.SERVICE_TOKEN}"))
                .withHeader("X-Correlation-Id", equalTo("corr-contract")),
        )
    }

    @Test
    fun `lookup account returns NotFound for 404 account-not-found`() {
        assertThat(gateway.lookupAccount(MISSING_ACCOUNT_ID)).isEqualTo(AccountLookup.NotFound)
    }

    companion object {
        const val ACCOUNTS_STUBS_GROUP = "io.github.capozziv.digitalbanking.accounts"
        const val ACCOUNTS_STUBS_ARTIFACT = "bootstrap"

        // The ids of the contracts of accounts.
        val ACCOUNT_ID: UUID = UUID.fromString("0198a1b2-0000-7000-8000-000000000001")
        val OWNER_ID: UUID = UUID.fromString("0198a1b2-0000-7000-8000-0000000000c1")
        val MISSING_ACCOUNT_ID: UUID = UUID.fromString("0198a1b2-0000-7000-8000-000000000404")
        val BUSY_ACCOUNT_ID: UUID = UUID.fromString("0198a1b2-0000-7000-8000-000000000503")

        @JvmField
        @RegisterExtension
        val accountsStubs: StubRunnerExtension =
            StubRunnerExtension()
                .stubsMode(StubsMode.CLASSPATH)
                .downloadStub(ACCOUNTS_STUBS_GROUP, ACCOUNTS_STUBS_ARTIFACT)
    }
}
