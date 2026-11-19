package io.github.capozziv.digitalbanking.accounts.bootstrap.contract

import io.github.capozziv.digitalbanking.accounts.adapters.persistence.OutboxEventAppender
import io.github.capozziv.digitalbanking.accounts.adapters.persistence.OutboxWriterConfiguration
import io.github.capozziv.digitalbanking.accounts.application.port.EventOutbox
import io.github.capozziv.digitalbanking.accounts.domain.AccountBlocked
import io.github.capozziv.digitalbanking.accounts.domain.AccountClosed
import io.github.capozziv.digitalbanking.accounts.domain.AccountEvent
import io.github.capozziv.digitalbanking.accounts.domain.AccountOpened
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.AccountTypeChanged
import io.github.capozziv.digitalbanking.accounts.domain.AccountUnblocked
import io.github.capozziv.digitalbanking.accounts.domain.Balance
import io.github.capozziv.digitalbanking.accounts.domain.EntryKind
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntry
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntryRecorded
import io.github.capozziv.digitalbanking.accounts.domain.Money
import io.github.capozziv.digitalbanking.accounts.domain.OverdraftLimit
import io.github.capozziv.digitalbanking.outbox.OutboxEventEnvelope
import io.github.capozziv.digitalbanking.testing.outboxcontract.OutboxMessageVerifier
import io.github.capozziv.digitalbanking.testing.outboxcontract.OutboxMessagingContractConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.util.UUID

/**
 * Base of the message contracts of accounts (contracts/messaging). Each trigger appends one event through the
 * service's outbox mapper (OutboxEventAppender, with the JSON mapper Boot builds from application.yml) into a real
 * outbox table, with a correlation id in the MDC as a request would leave it; OutboxMessageVerifier reads the row back
 * as the envelope the relay publishes to the topic of `accounts.aws.topic-arn`. Needs Docker (PostgreSQL).
 *
 * The context is listed in @SpringBootTest, with no @SpringBootConfiguration of its own: the @WebMvcTest of
 * HttpContractBase, in this same package, would find it and lose the controllers.
 */
@SpringBootTest(
    classes = [OutboxMessagingContractConfiguration::class, OutboxWriterConfiguration::class, OutboxEventAppender::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = ["digitalbanking.contract.messaging.topic-arn=\${accounts.aws.topic-arn}"],
)
@ImportAutoConfiguration(JacksonAutoConfiguration::class)
abstract class MessagingContractBase {
    @Autowired
    private lateinit var outbox: EventOutbox

    @Autowired
    private lateinit var verifier: OutboxMessageVerifier

    @BeforeEach
    fun startFromAnEmptyOutbox() {
        verifier.clear()
        MDC.put(OutboxEventEnvelope.CORRELATION_ID_MDC_KEY, CORRELATION_ID)
    }

    @AfterEach
    fun clearCorrelationId() {
        MDC.remove(OutboxEventEnvelope.CORRELATION_ID_MDC_KEY)
    }

    fun accountOpened() = append(AccountOpened(ACCOUNT_ID, CUSTOMER_ID, AccountType.FREE))

    fun creditRecorded() =
        append(
            LedgerEntryRecorded(
                LedgerEntry(
                    id = CREDIT_ENTRY_ID,
                    accountId = ACCOUNT_ID,
                    sequence = 2,
                    kind = EntryKind.CREDIT,
                    amount = Money(5000),
                    balanceAfter = Balance(6000),
                    description = "Transfer received",
                    idempotencyKey = "transfer-0198a1b2-0000-7000-8000-00000000f001-credit",
                    createdAt = Instant.parse("2026-11-18T12:00:00Z"),
                ),
            ),
        )

    fun debitRecordedWithoutDescription() =
        append(
            LedgerEntryRecorded(
                LedgerEntry(
                    id = DEBIT_ENTRY_ID,
                    accountId = ACCOUNT_ID,
                    sequence = 3,
                    kind = EntryKind.DEBIT,
                    amount = Money(1500),
                    balanceAfter = Balance(-500),
                    description = null,
                    idempotencyKey = "debit-key-3",
                    createdAt = Instant.parse("2026-11-18T12:05:00.123456Z"),
                ),
            ),
        )

    fun accountTypeChanged() = append(AccountTypeChanged(ACCOUNT_ID, AccountType.FREE, AccountType.GOLD, OverdraftLimit(200000)))

    fun accountBlocked() = append(AccountBlocked(ACCOUNT_ID, CUSTOMER_ID, STATUS_CHANGED_AT))

    fun accountUnblocked() = append(AccountUnblocked(ACCOUNT_ID, CUSTOMER_ID, STATUS_CHANGED_AT))

    fun accountClosed() = append(AccountClosed(ACCOUNT_ID, CUSTOMER_ID, STATUS_CHANGED_AT))

    private fun append(event: AccountEvent) = outbox.append(event)

    private companion object {
        const val CORRELATION_ID = "contract-correlation-id"
        val ACCOUNT_ID: UUID = UUID.fromString("0198a1b2-0000-7000-8000-000000000001")
        val CUSTOMER_ID: UUID = UUID.fromString("0198a1b2-0000-7000-8000-0000000000c1")
        val STATUS_CHANGED_AT: Instant = Instant.parse("2026-11-18T12:20:00Z")
        val CREDIT_ENTRY_ID: UUID = UUID.fromString("0198a1b2-0000-7000-8000-0000000e1002")
        val DEBIT_ENTRY_ID: UUID = UUID.fromString("0198a1b2-0000-7000-8000-0000000e1003")
    }
}
