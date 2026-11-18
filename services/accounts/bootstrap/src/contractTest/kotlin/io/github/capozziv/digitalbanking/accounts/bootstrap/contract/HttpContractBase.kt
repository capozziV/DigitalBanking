package io.github.capozziv.digitalbanking.accounts.bootstrap.contract

import io.github.capozziv.digitalbanking.accounts.adapters.web.AccountController
import io.github.capozziv.digitalbanking.accounts.adapters.web.AccountsSecurityConfiguration
import io.github.capozziv.digitalbanking.accounts.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.accounts.application.usecase.AccountView
import io.github.capozziv.digitalbanking.accounts.application.usecase.ChangeAccountTypeUseCase
import io.github.capozziv.digitalbanking.accounts.application.usecase.DebitOutcome
import io.github.capozziv.digitalbanking.accounts.application.usecase.FindEntryByIdempotencyKeyUseCase
import io.github.capozziv.digitalbanking.accounts.application.usecase.GetAccountUseCase
import io.github.capozziv.digitalbanking.accounts.application.usecase.OpenAccountUseCase
import io.github.capozziv.digitalbanking.accounts.application.usecase.RecordCreditUseCase
import io.github.capozziv.digitalbanking.accounts.application.usecase.RecordDebitUseCase
import io.github.capozziv.digitalbanking.accounts.application.usecase.RecordEntryCommand
import io.github.capozziv.digitalbanking.accounts.application.usecase.RecordEntryResult
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.AvailableBalance
import io.github.capozziv.digitalbanking.accounts.domain.Balance
import io.github.capozziv.digitalbanking.accounts.domain.EntryKind
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntry
import io.github.capozziv.digitalbanking.accounts.domain.Money
import io.github.capozziv.digitalbanking.accounts.domain.OverdraftLimit
import io.github.capozziv.digitalbanking.observability.ObservabilityAutoConfiguration
import io.github.capozziv.digitalbanking.security.Caller
import io.github.capozziv.digitalbanking.security.DigitalBankingSecurityAutoConfiguration
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.restassured.module.mockmvc.RestAssuredMockMvc
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.doAnswer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import java.time.Instant
import java.util.UUID

/**
 * Base of the tests generated from contracts/http: the HTTP API of accounts as transfers calls it.
 *
 * The real web layer runs (AccountController, Problem Details, the JWT security of libs/security with the route rules
 * of accounts, the correlation id filter); the use cases are mocks that answer as a small ledger of fixed ids, so each
 * contract names its scenario by the account id, the key or the amount it sends. Requests carry a machine token signed
 * by [JwtTestSupport] ([serviceAuthorization]), so a route that stopped accepting services breaks a contract.
 */
@WebMvcTest(controllers = [AccountController::class])
@ImportAutoConfiguration(DigitalBankingSecurityAutoConfiguration::class, ObservabilityAutoConfiguration::class)
@Import(JwtTestSupport.DecoderConfiguration::class, AccountsSecurityConfiguration::class, HttpContractBase.Meters::class)
abstract class HttpContractBase {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var openAccount: OpenAccountUseCase

    @MockitoBean
    private lateinit var getAccount: GetAccountUseCase

    @MockitoBean
    private lateinit var recordCredit: RecordCreditUseCase

    @MockitoBean
    private lateinit var recordDebit: RecordDebitUseCase

    @MockitoBean
    private lateinit var findEntry: FindEntryByIdempotencyKeyUseCase

    @MockitoBean
    private lateinit var changeAccountType: ChangeAccountTypeUseCase

    @TestConfiguration(proxyBeanMethods = false)
    class Meters {
        @Bean
        fun meterRegistry(): MeterRegistry = SimpleMeterRegistry()
    }

    @BeforeEach
    fun setUpLedger() {
        RestAssuredMockMvc.mockMvc(mockMvc)

        doAnswer { view(it.getArgument(0)) }.`when`(getAccount).execute(anyUuid(), anyCaller())
        doAnswer { recorded(it.getArgument(0), EntryKind.CREDIT) }.`when`(recordCredit).execute(anyCommand())
        doAnswer { debit(it.getArgument(0)) }.`when`(recordDebit).execute(anyCommand())
        doAnswer { foundEntry(it.getArgument(0), it.getArgument(1)) }
            .`when`(findEntry)
            .execute(anyUuid(), anyString(), anyCaller())
    }

    @AfterEach
    fun resetRestAssured() {
        RestAssuredMockMvc.reset()
    }

    /** Value of the Authorization header of a call by transfers: a machine token with the internal scope. */
    fun serviceAuthorization(): String = "Bearer ${JwtTestSupport.serviceToken("transfers")}"

    private fun view(accountId: UUID): AccountView {
        requireAccount(accountId)
        return AccountView.of(Account(ACCOUNT_ID, OWNER_ID, AccountType.FREE), BALANCE, OverdraftLimit(0))
    }

    private fun debit(command: RecordEntryCommand): DebitOutcome {
        requireAccount(command.accountId)
        if (command.amount.cents > BALANCE.cents) return DebitOutcome.Refused(AvailableBalance(BALANCE.cents))
        return DebitOutcome.Recorded(recorded(command, EntryKind.DEBIT))
    }

    private fun recorded(
        command: RecordEntryCommand,
        kind: EntryKind,
    ): RecordEntryResult {
        requireAccount(command.accountId)
        val balanceAfter = if (kind == EntryKind.CREDIT) BALANCE + command.amount else BALANCE - command.amount
        return RecordEntryResult(entry(kind, command.amount.cents, balanceAfter, command.description, command.idempotencyKey), true)
    }

    private fun foundEntry(
        accountId: UUID,
        idempotencyKey: String,
    ): LedgerEntry? {
        requireAccount(accountId)
        if (idempotencyKey != RECORDED_DEBIT_KEY) return null
        return entry(EntryKind.DEBIT, BALANCE.cents, Balance(0), "Transfer to someone", RECORDED_DEBIT_KEY)
    }

    private fun entry(
        kind: EntryKind,
        cents: Long,
        balanceAfter: Balance,
        description: String?,
        idempotencyKey: String,
    ) = LedgerEntry(
        id = ENTRY_ID,
        accountId = ACCOUNT_ID,
        sequence = 2,
        kind = kind,
        amount = Money(cents),
        balanceAfter = balanceAfter,
        description = description,
        idempotencyKey = idempotencyKey,
        createdAt = CREATED_AT,
    )

    private fun requireAccount(accountId: UUID) {
        if (accountId != ACCOUNT_ID) throw AccountNotFoundException(accountId)
    }

    private companion object {
        /** The account of the contracts; any other id is an account that does not exist. */
        val ACCOUNT_ID: UUID = UUID.fromString("0198a1b2-0000-7000-8000-000000000001")
        val OWNER_ID: UUID = UUID.fromString("0198a1b2-0000-7000-8000-0000000000c1")
        val ENTRY_ID: UUID = UUID.fromString("0198a1b2-0000-7000-8000-0000000000aa")
        val BALANCE = Balance(1_000)
        val CREATED_AT: Instant = Instant.parse("2026-10-27T10:00:00Z")

        /** The only key with an entry on the account (a debit of the whole balance). */
        const val RECORDED_DEBIT_KEY = "transfer-1-debit"

        // Mockito matchers return null, which Kotlin refuses for a non-null parameter: register the matcher and hand
        // Kotlin a placeholder instead.
        fun anyUuid(): UUID = ArgumentMatchers.any(UUID::class.java) ?: ACCOUNT_ID

        fun anyString(): String = ArgumentMatchers.anyString() ?: ""

        fun anyCaller(): Caller = ArgumentMatchers.any(Caller::class.java) ?: Caller.Service

        fun anyCommand(): RecordEntryCommand =
            ArgumentMatchers.any(RecordEntryCommand::class.java)
                ?: RecordEntryCommand(ACCOUNT_ID, Money(1), null, "placeholder")
    }
}
