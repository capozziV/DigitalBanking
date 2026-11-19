package io.github.capozziv.digitalbanking.statement.adapters.messaging

import io.github.capozziv.digitalbanking.statement.application.port.StatementFirstPageCache
import io.github.capozziv.digitalbanking.statement.application.port.StatementRepository
import io.github.capozziv.digitalbanking.statement.application.usecase.RecordStatementEntryUseCase
import io.github.capozziv.digitalbanking.statement.application.usecase.RegisterAccountUseCase
import io.github.capozziv.digitalbanking.statement.domain.EntryKind
import io.github.capozziv.digitalbanking.statement.domain.StatementCursor
import io.github.capozziv.digitalbanking.statement.domain.StatementEntry
import io.github.capozziv.digitalbanking.statement.domain.StatementPage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import sh.stubborn.contract.stubrunner.StubsMode
import sh.stubborn.contract.stubrunner.junit.StubRunnerExtension
import sh.stubborn.contract.verifier.converter.YamlContract
import sh.stubborn.contract.verifier.messaging.MessageVerifierSender
import java.time.Instant
import java.util.UUID

/**
 * Consumer side of the message contracts of accounts: the Stub Runner triggers each contract, and its message (the
 * outbox envelope, as the relay publishes it) goes straight to the handler of StatementEventListener, with no SQS.
 */
class AccountEventsContractTest {
    private val entries = mutableListOf<StatementEntry>()
    private val accounts = mutableMapOf<UUID, UUID>()
    private val evicted = mutableListOf<UUID>()

    private val repository =
        object : StatementRepository {
            override fun insertEntry(entry: StatementEntry) = entries.add(entry)

            override fun findEntry(
                accountId: UUID,
                sequence: Long,
            ) = error("not used")

            override fun insertAccount(
                accountId: UUID,
                customerId: UUID,
            ) = accounts.putIfAbsent(accountId, customerId) == null

            override fun accountExists(accountId: UUID) = accountId in accounts

            override fun findCustomerId(accountId: UUID) = accounts[accountId]

            override fun findPage(
                accountId: UUID,
                cursor: StatementCursor?,
                limit: Int,
            ): StatementPage = error("not used")
        }

    private val cache =
        object : StatementFirstPageCache {
            override fun get(accountId: UUID) = error("not used")

            override fun put(
                accountId: UUID,
                page: StatementPage,
            ) = error("not used")

            override fun evict(accountId: UUID) {
                evicted += accountId
            }
        }

    private val listener =
        StatementEventListener(EventEnvelopeReader(), RecordStatementEntryUseCase(repository, cache), RegisterAccountUseCase(repository))

    @BeforeEach
    fun routeMessagesToTheListener() {
        listenerSender.listener = listener
    }

    @AfterEach
    fun stopRouting() {
        listenerSender.listener = null
    }

    @Test
    fun `account opened registers the owner of the account`() {
        assertThat(accountsStubs.trigger("accountOpened")).isTrue()

        assertThat(accounts).containsExactlyEntriesOf(mapOf(ACCOUNT_ID to CUSTOMER_ID))
    }

    @Test
    fun `a recorded credit becomes a statement entry`() {
        assertThat(accountsStubs.trigger("creditRecorded")).isTrue()

        assertThat(entries).containsExactly(
            StatementEntry(
                entryId = UUID.fromString("0198a1b2-0000-7000-8000-0000000e1002"),
                accountId = ACCOUNT_ID,
                sequence = 2,
                kind = EntryKind.CREDIT,
                amountCents = 5000,
                balanceAfterCents = 6000,
                description = "Transfer received",
                createdAt = Instant.parse("2026-11-18T12:00:00Z"),
            ),
        )
        assertThat(evicted).containsExactly(ACCOUNT_ID)
    }

    @Test
    fun `a recorded debit without a description becomes a statement entry in overdraft`() {
        assertThat(accountsStubs.trigger("debitRecordedWithoutDescription")).isTrue()

        assertThat(entries).containsExactly(
            StatementEntry(
                entryId = UUID.fromString("0198a1b2-0000-7000-8000-0000000e1003"),
                accountId = ACCOUNT_ID,
                sequence = 3,
                kind = EntryKind.DEBIT,
                amountCents = 1500,
                balanceAfterCents = -500,
                description = null,
                createdAt = Instant.parse("2026-11-18T12:05:00.123456Z"),
            ),
        )
    }

    @Test
    fun `an account type change is acknowledged without effect`() {
        assertThat(accountsStubs.trigger("accountTypeChanged")).isTrue()

        assertThat(entries).isEmpty()
        assertThat(accounts).isEmpty()
    }

    /** Delivers each message sent to the accounts topic to the listener of the test running now. */
    class ListenerSender : MessageVerifierSender<Any> {
        @Volatile
        var listener: StatementEventListener? = null

        override fun send(
            message: Any,
            destination: String,
            contract: YamlContract?,
        ) = deliver(message, destination)

        override fun <T : Any?> send(
            payload: T,
            headers: Map<String, Any>?,
            destination: String,
            contract: YamlContract?,
        ) = deliver(payload, destination)

        private fun deliver(
            payload: Any?,
            destination: String,
        ) {
            check(destination == ACCOUNTS_TOPIC) { "statement subscribes only to $ACCOUNTS_TOPIC, not to $destination" }
            checkNotNull(listener) { "no listener to deliver to" }.onAccountEvent(payload as String)
        }
    }

    companion object {
        private const val ACCOUNTS_TOPIC = "accounts-events.fifo"
        private val ACCOUNT_ID: UUID = UUID.fromString("0198a1b2-0000-7000-8000-000000000001")
        private val CUSTOMER_ID: UUID = UUID.fromString("0198a1b2-0000-7000-8000-0000000000c1")

        private val listenerSender = ListenerSender()

        @JvmField
        @RegisterExtension
        val accountsStubs: StubRunnerExtension =
            StubRunnerExtension()
                .stubsMode(StubsMode.CLASSPATH)
                .messageVerifierSender(listenerSender)
                .downloadStub("io.github.capozziv.digitalbanking.accounts", "bootstrap")
    }
}
