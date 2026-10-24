package io.github.capozziv.digitalbanking.accounts.bootstrap.persistence

import io.github.capozziv.digitalbanking.accounts.application.AccountBusyException
import io.github.capozziv.digitalbanking.accounts.application.port.AccountRepository
import io.github.capozziv.digitalbanking.accounts.application.port.AccountTypeRepository
import io.github.capozziv.digitalbanking.accounts.application.port.EventOutbox
import io.github.capozziv.digitalbanking.accounts.application.port.IdGenerator
import io.github.capozziv.digitalbanking.accounts.application.port.InsertAccountResult
import io.github.capozziv.digitalbanking.accounts.application.port.LedgerEntryRepository
import io.github.capozziv.digitalbanking.accounts.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.accounts.bootstrap.AccountsIntegrationTest
import io.github.capozziv.digitalbanking.accounts.domain.Account
import io.github.capozziv.digitalbanking.accounts.domain.AccountOpened
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.AccountTypeChanged
import io.github.capozziv.digitalbanking.accounts.domain.Balance
import io.github.capozziv.digitalbanking.accounts.domain.EntryKind
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntry
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntryRecorded
import io.github.capozziv.digitalbanking.accounts.domain.Money
import io.github.capozziv.digitalbanking.accounts.domain.OverdraftLimit
import io.github.capozziv.digitalbanking.outbox.OutboxEventEnvelope
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.simple.JdbcClient
import java.sql.DriverManager
import java.sql.SQLException
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class JdbcRepositoriesIntegrationTest : AccountsIntegrationTest() {
    @Autowired private lateinit var accounts: AccountRepository

    @Autowired private lateinit var entries: LedgerEntryRepository

    @Autowired private lateinit var types: AccountTypeRepository

    @Autowired private lateinit var outbox: EventOutbox

    @Autowired private lateinit var tx: TransactionRunner

    @Autowired private lateinit var ids: IdGenerator

    @Autowired private lateinit var appJdbc: JdbcClient

    /** Microsecond precision, as timestamptz stores it: writes with it must round-trip unchanged. */
    private val now = Instant.parse("2026-10-22T14:31:00.123456Z")

    /** Nanosecond precision, which timestamptz would silently truncate. */
    private val nanoInstant = Instant.parse("2026-10-22T14:31:00.123456789Z")

    @AfterEach
    fun clearMdc() {
        MDC.clear()
    }

    // --- account_type ---

    @Test
    fun `account types are seeded with the limits of the spec`() {
        val limits = types.findAll().associate { it.type to it.overdraftLimit.cents }

        assertThat(limits).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                AccountType.FREE to 0L,
                AccountType.STANDARD to 50_000L,
                AccountType.GOLD to 200_000L,
                AccountType.BLACK to 1_000_000L,
                AccountType.DIAMOND to 5_000_000L,
            ),
        )
        assertThat(types.findLimit(AccountType.GOLD)).isEqualTo(OverdraftLimit(200_000))
    }

    @Test
    fun `updateLimit stores the limit and updated_at and returns the stored row`() {
        try {
            val stored = tx.inTransaction { types.updateLimit(AccountType.GOLD, OverdraftLimit(300_000), now) }

            assertThat(stored.type).isEqualTo(AccountType.GOLD)
            assertThat(stored.overdraftLimit).isEqualTo(OverdraftLimit(300_000))
            assertThat(stored.updatedAt).isEqualTo(now)
            assertThat(types.findLimit(AccountType.GOLD)).isEqualTo(OverdraftLimit(300_000))
            assertThat(types.findAll().single { it.type == AccountType.GOLD }.updatedAt).isEqualTo(now)
        } finally {
            tx.inTransaction { types.updateLimit(AccountType.GOLD, OverdraftLimit(200_000), now) }
        }
    }

    @Test
    fun `findLimit and updateLimit throw when the type row is missing`() {
        assertThatThrownBy {
            tx.inTransaction {
                appJdbc.sql("update account_type set type = 'GONE' where type = 'DIAMOND'").update()

                assertThatThrownBy { types.findLimit(AccountType.DIAMOND) }.isInstanceOf(IllegalStateException::class.java)
                assertThatThrownBy { types.updateLimit(AccountType.DIAMOND, OverdraftLimit(1), now) }
                    .isInstanceOf(IllegalStateException::class.java)
                throw RollbackSignal()
            }
        }.isInstanceOf(RollbackSignal::class.java)

        assertThat(types.findLimit(AccountType.DIAMOND)).isEqualTo(OverdraftLimit(5_000_000))
    }

    // --- account ---

    @Test
    fun `insert stores the account and findById reads it back with the type stored by name`() {
        val account = Account(ids.newId(), UUID.randomUUID(), AccountType.FREE)

        val result = tx.inTransaction { accounts.insert(account, "open-${account.id}") }

        assertThat(result).isEqualTo(InsertAccountResult.INSERTED)
        assertThat(accounts.findById(account.id)).isEqualTo(account)
        assertThat(accounts.findByOpeningIdempotencyKey("open-${account.id}")).isEqualTo(account)
        assertThat(rawAccountType(account.id)).isEqualTo("FREE")
    }

    @Test
    fun `findById and findByOpeningIdempotencyKey return null when nothing matches`() {
        assertThat(accounts.findById(UUID.randomUUID())).isNull()
        assertThat(accounts.findByOpeningIdempotencyKey("missing-${UUID.randomUUID()}")).isNull()
    }

    @Test
    fun `second account of the same customer with another key is DUPLICATE_CUSTOMER`() {
        val first = openAccount()
        val second = Account(ids.newId(), first.customerId, AccountType.FREE)

        val result = tx.inTransaction { accounts.insert(second, "open-${second.id}") }

        assertThat(result).isEqualTo(InsertAccountResult.DUPLICATE_CUSTOMER)
        assertThat(accounts.findById(second.id)).isNull()
    }

    @Test
    fun `same opening key for another customer is DUPLICATE_OPENING_KEY`() {
        val first = openAccount()
        val other = Account(ids.newId(), UUID.randomUUID(), AccountType.FREE)

        val result = tx.inTransaction { accounts.insert(other, "open-${first.id}") }

        assertThat(result).isEqualTo(InsertAccountResult.DUPLICATE_OPENING_KEY)
        assertThat(accounts.findById(other.id)).isNull()
    }

    @Test
    fun `same customer and same key is DUPLICATE_OPENING_KEY and the re-read works in the same transaction`() {
        val first = openAccount()
        val retry = Account(ids.newId(), first.customerId, AccountType.FREE)

        val (result, reread) =
            tx.inTransaction {
                accounts.insert(retry, "open-${first.id}") to accounts.findByOpeningIdempotencyKey("open-${first.id}")
            }

        assertThat(result).isEqualTo(InsertAccountResult.DUPLICATE_OPENING_KEY)
        assertThat(reread).isEqualTo(first)
    }

    @Test
    fun `insert racing an uncommitted insert with the same key waits and reports DUPLICATE_OPENING_KEY`() {
        val key = "open-race-${UUID.randomUUID()}"
        val winner = Account(ids.newId(), UUID.randomUUID(), AccountType.FREE)
        val loser = Account(ids.newId(), UUID.randomUUID(), AccountType.FREE)

        appConnection().use { connection ->
            connection.autoCommit = false
            connection
                .prepareStatement(
                    "insert into account (id, customer_id, type, opening_idempotency_key) values (?, ?, 'FREE', ?)",
                ).use { statement ->
                    statement.setObject(1, winner.id)
                    statement.setObject(2, winner.customerId)
                    statement.setString(3, key)
                    statement.executeUpdate()
                }

            val racing =
                CompletableFuture.supplyAsync {
                    tx.inTransaction { accounts.insert(loser, key) to accounts.findByOpeningIdempotencyKey(key) }
                }
            Thread.sleep(300)
            assertThat(racing).isNotDone()

            connection.commit()
            val (result, reread) = racing.get(5, TimeUnit.SECONDS)
            assertThat(result).isEqualTo(InsertAccountResult.DUPLICATE_OPENING_KEY)
            assertThat(reread).isEqualTo(winner)
        }
    }

    @Test
    fun `lockById returns the account and null when it does not exist`() {
        val account = openAccount()

        assertThat(tx.inTransaction { accounts.lockById(account.id) }).isEqualTo(account)
        assertThat(tx.inTransaction { accounts.lockById(UUID.randomUUID()) }).isNull()
    }

    @Test
    fun `lockById throws AccountBusyException within the lock timeout when another transaction holds the lock`() {
        val account = openAccount()

        appConnection().use { holder ->
            holder.autoCommit = false
            holder.prepareStatement("select id from account where id = ? for update").use { statement ->
                statement.setObject(1, account.id)
                statement.executeQuery().close()
            }

            val started = System.nanoTime()
            assertThatThrownBy { tx.inTransaction { accounts.lockById(account.id) } }
                .isInstanceOfSatisfying(AccountBusyException::class.java) { assertThat(it.accountId).isEqualTo(account.id) }
            assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(3))

            holder.rollback()
        }
    }

    @Test
    fun `updateType writes only the type and throws when no account is updated`() {
        val account = openAccount()

        tx.inTransaction { accounts.updateType(account.copy(type = AccountType.GOLD)) }

        assertThat(accounts.findById(account.id)).isEqualTo(account.copy(type = AccountType.GOLD))
        assertThat(rawAccountType(account.id)).isEqualTo("GOLD")
        assertThatThrownBy {
            tx.inTransaction { accounts.updateType(Account(UUID.randomUUID(), UUID.randomUUID(), AccountType.GOLD)) }
        }.isInstanceOf(IllegalStateException::class.java)
    }

    // --- ledger_entry ---

    @Test
    fun `findLast returns null without entries and the entry with the highest sequence otherwise`() {
        val account = openAccount()
        assertThat(entries.findLast(account.id)).isNull()

        val first = entry(account, 1, EntryKind.CREDIT, 3_700, 3_700, "k1")
        val second = entry(account, 2, EntryKind.DEBIT, 1_000, 2_700, "k2", description = null)
        val third = entry(account, 3, EntryKind.CREDIT, 300, 3_000, "k3")
        tx.inTransaction { listOf(first, second, third).forEach { entries.insert(it) } }

        assertThat(entries.findLast(account.id)).isEqualTo(third)
        assertThat(rawEntryKind(second.id)).isEqualTo("DEBIT")
    }

    @Test
    fun `findByIdempotencyKey is scoped to the account`() {
        val account = openAccount()
        val other = openAccount()
        val debit = entry(account, 1, EntryKind.DEBIT, 500, -500, "shared-key", description = null)
        tx.inTransaction { entries.insert(debit) }

        assertThat(entries.findByIdempotencyKey(account.id, "shared-key")).isEqualTo(debit)
        assertThat(entries.findByIdempotencyKey(other.id, "shared-key")).isNull()
    }

    @Test
    fun `insert lets a duplicate sequence or idempotency key propagate`() {
        val account = openAccount()
        tx.inTransaction { entries.insert(entry(account, 1, EntryKind.CREDIT, 100, 100, "k1")) }

        assertThatThrownBy { tx.inTransaction { entries.insert(entry(account, 1, EntryKind.CREDIT, 100, 200, "k2")) } }
            .isInstanceOf(DuplicateKeyException::class.java)
        assertThatThrownBy { tx.inTransaction { entries.insert(entry(account, 2, EntryKind.CREDIT, 100, 200, "k1")) } }
            .isInstanceOf(DuplicateKeyException::class.java)
    }

    @Test
    fun `insert then findByIdempotencyKey returns an entry equal to the one written`() {
        val account = openAccount()
        val credit = entry(account, 1, EntryKind.CREDIT, 3_700, 3_700, "round-trip", description = "Salary")

        tx.inTransaction { entries.insert(credit) }

        assertThat(entries.findByIdempotencyKey(account.id, "round-trip")).isEqualTo(credit)
        assertThat(entries.findLast(account.id)).isEqualTo(credit)
    }

    @Test
    fun `insert rejects a createdAt finer than microseconds and writes nothing`() {
        val account = openAccount()
        val credit = entry(account, 1, EntryKind.CREDIT, 100, 100, "nano").copy(createdAt = nanoInstant)

        assertThatThrownBy { tx.inTransaction { entries.insert(credit) } }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("createdAt must have microsecond precision")
        assertThat(entries.findLast(account.id)).isNull()
    }

    @Test
    fun `the application user cannot update or delete ledger entries`() {
        val account = openAccount()
        tx.inTransaction { entries.insert(entry(account, 1, EntryKind.CREDIT, 100, 100, "k1")) }

        assertThat(sqlStateOf { appJdbc.sql("update ledger_entry set amount = 1").update() }).isEqualTo(INSUFFICIENT_PRIVILEGE)
        assertThat(sqlStateOf { appJdbc.sql("delete from ledger_entry").update() }).isEqualTo(INSUFFICIENT_PRIVILEGE)
        assertThat(entries.findLast(account.id)?.amount).isEqualTo(Money(100))
    }

    // --- outbox ---

    @Test
    fun `append writes LedgerEntryRecorded with the spec payload and the MDC correlation id`() {
        MDC.put(OutboxEventEnvelope.CORRELATION_ID_MDC_KEY, "corr-ledger")
        val account = openAccount()
        val credit = entry(account, 1, EntryKind.CREDIT, 3_700, 3_700, "credit-1", description = "Salary")

        tx.inTransaction { outbox.append(LedgerEntryRecorded(credit)) }

        assertOutboxRow(
            aggregateId = account.id,
            eventType = "LedgerEntryRecorded",
            correlationId = "corr-ledger",
            payload =
                """
                {"entryId": "${credit.id}", "accountId": "${account.id}", "sequence": 1, "kind": "CREDIT",
                 "amount": 3700, "balanceAfter": 3700, "description": "Salary", "idempotencyKey": "credit-1",
                 "createdAt": "2026-10-22T14:31:00.123456Z"}
                """,
        )
    }

    @Test
    fun `append writes a null description and a negative balance as numbers`() {
        val account = openAccount()
        val debit = entry(account, 1, EntryKind.DEBIT, 2_500, -2_500, "debit-1", description = null)

        tx.inTransaction { outbox.append(LedgerEntryRecorded(debit)) }

        assertOutboxRow(
            aggregateId = account.id,
            eventType = "LedgerEntryRecorded",
            correlationId = null,
            payload =
                """
                {"entryId": "${debit.id}", "accountId": "${account.id}", "sequence": 1, "kind": "DEBIT",
                 "amount": 2500, "balanceAfter": -2500, "description": null, "idempotencyKey": "debit-1",
                 "createdAt": "2026-10-22T14:31:00.123456Z"}
                """,
        )
    }

    @Test
    fun `append writes AccountOpened and AccountTypeChanged with the spec payloads`() {
        MDC.put(OutboxEventEnvelope.CORRELATION_ID_MDC_KEY, "corr-account")
        val accountId = ids.newId()
        val customerId = UUID.randomUUID()

        tx.inTransaction { outbox.append(AccountOpened(accountId, customerId, AccountType.FREE)) }
        assertOutboxRow(
            accountId,
            "AccountOpened",
            "corr-account",
            """{"accountId": "$accountId", "customerId": "$customerId", "type": "FREE"}""",
        )

        ownerJdbc.sql("delete from outbox where aggregate_id = :id").param("id", accountId.toString()).update()
        tx.inTransaction { outbox.append(AccountTypeChanged(accountId, AccountType.FREE, AccountType.GOLD)) }
        assertOutboxRow(
            accountId,
            "AccountTypeChanged",
            "corr-account",
            """{"accountId": "$accountId", "previousType": "FREE", "newType": "GOLD"}""",
        )
    }

    @Test
    fun `entry and outbox row roll back together`() {
        val account = openAccount()
        val credit = entry(account, 1, EntryKind.CREDIT, 100, 100, "rollback-1")

        assertThatThrownBy {
            tx.inTransaction {
                entries.insert(credit)
                outbox.append(LedgerEntryRecorded(credit))
                throw IllegalStateException("abort")
            }
        }.hasMessage("abort")

        assertThat(entries.findLast(account.id)).isNull()
        assertThat(outboxRowCount(account.id)).isZero()
    }

    @Test
    fun `inTransaction returns the value of the block`() {
        assertThat(tx.inTransaction { 42 }).isEqualTo(42)
    }

    @Test
    fun `ids are version 7 uuids`() {
        val generated = List(3) { ids.newId() }

        assertThat(generated.map { it.version() }).containsOnly(7)
        assertThat(generated).doesNotHaveDuplicates()
    }

    // --- helpers ---

    private fun openAccount(): Account {
        val account = Account(ids.newId(), UUID.randomUUID(), AccountType.FREE)
        check(tx.inTransaction { accounts.insert(account, "open-${account.id}") } == InsertAccountResult.INSERTED)
        return account
    }

    private fun entry(
        account: Account,
        sequence: Long,
        kind: EntryKind,
        amount: Long,
        balanceAfter: Long,
        key: String,
        description: String? = "entry $sequence",
    ) = LedgerEntry(
        id = ids.newId(),
        accountId = account.id,
        sequence = sequence,
        kind = kind,
        amount = Money(amount),
        balanceAfter = Balance(balanceAfter),
        description = description,
        idempotencyKey = key,
        createdAt = now,
    )

    private fun appConnection() = DriverManager.getConnection(postgres.jdbcUrl, APP_USER, APP_PASSWORD)

    private fun rawAccountType(id: UUID): String =
        ownerJdbc
            .sql("select type from account where id = :id")
            .param("id", id)
            .query(String::class.java)
            .single()

    private fun rawEntryKind(id: UUID): String =
        ownerJdbc
            .sql("select kind from ledger_entry where id = :id")
            .param("id", id)
            .query(String::class.java)
            .single()

    private fun outboxRowCount(aggregateId: UUID): Int =
        ownerJdbc
            .sql("select count(*) from outbox where aggregate_id = :id")
            .param("id", aggregateId.toString())
            .query(Int::class.java)
            .single()

    private fun assertOutboxRow(
        aggregateId: UUID,
        eventType: String,
        correlationId: String?,
        payload: String,
    ) {
        val row =
            ownerJdbc
                .sql("select * from outbox where aggregate_id = :id")
                .param("id", aggregateId.toString())
                .query()
                .singleRow()
        assertThat(row["event_type"]).isEqualTo(eventType)
        assertThat(row["event_version"]).isEqualTo(1)
        assertThat(row["correlation_id"]).isEqualTo(correlationId)
        assertThat(row["published_at"]).isNull()
        assertThat(row["occurred_at"]).isNotNull()
        assertThat(
            ownerJdbc
                .sql("select payload = cast(:expected as jsonb) from outbox where aggregate_id = :id")
                .param("expected", payload.trimIndent())
                .param("id", aggregateId.toString())
                .query(Boolean::class.java)
                .single(),
        ).describedAs("payload %s", row["payload"]).isTrue()
    }

    private fun sqlStateOf(statement: () -> Unit): String? {
        val error = runCatching(statement).exceptionOrNull()
        return generateSequence(error) { it.cause }.filterIsInstance<SQLException>().firstOrNull()?.sqlState
    }

    private class RollbackSignal : RuntimeException("rollback")

    private companion object {
        const val INSUFFICIENT_PRIVILEGE = "42501"
    }
}
