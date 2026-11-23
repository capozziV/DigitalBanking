package io.github.capozziv.digitalbanking.accounts.bootstrap.web

import io.github.capozziv.digitalbanking.accounts.application.port.EventOutbox
import io.github.capozziv.digitalbanking.accounts.bootstrap.AccountsIntegrationTest
import io.github.capozziv.digitalbanking.accounts.domain.AccountEvent
import io.github.capozziv.digitalbanking.accounts.domain.AccountOpened
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntryRecorded
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doAnswer
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpMethod
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Opening an account credits the welcome bonus (the default amount, 10000 cents) in the same transaction as the opening. */
@TestPropertySource(properties = ["accounts.welcome-bonus.amount=10000"])
class WelcomeBonusIntegrationTest : AccountsIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @MockitoSpyBean
    private lateinit var outbox: EventOutbox

    private val api by lazy { ApiClient(port) }

    @Test
    fun `opening an account writes one bonus entry and the events AccountOpened then LedgerEntryRecorded`() {
        val customerId = UUID.randomUUID()

        val response = api.openAccount(customerId)

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(201)
        assertThat(response.body!!.get("balance").asLong()).isEqualTo(10_000)
        val accountId = response.body.get("id").asString()
        val entry =
            ownerJdbc
                .sql(
                    "select sequence, kind, amount, balance_after, description, idempotency_key from ledger_entry where account_id = :id",
                ).param("id", UUID.fromString(accountId))
                .query { rs, _ ->
                    listOf(
                        rs.getLong("sequence"),
                        rs.getString("kind"),
                        rs.getLong("amount"),
                        rs.getLong("balance_after"),
                        rs.getString("description"),
                        rs.getString("idempotency_key"),
                    )
                }.single()
        assertThat(entry).containsExactly(1L, "CREDIT", 10_000L, 10_000L, "Bônus de boas-vindas", "welcome-bonus:$accountId")
        assertThat(eventTypesOf(accountId)).containsExactly("AccountOpened", "LedgerEntryRecorded")
        assertThat(
            api
                .get("/accounts/$accountId", asCustomer(customerId))
                .body!!
                .get("balance")
                .asLong(),
        ).isEqualTo(10_000)
    }

    @Test
    fun `retrying the opening with the same key does not write a second bonus`() {
        val customerId = UUID.randomUUID()
        val key = UUID.randomUUID().toString()
        val first = api.openAccount(customerId, key)
        val accountId = first.body!!.get("id").asString()

        val retry = api.openAccount(customerId, key)

        assertThat(retry.status).describedAs(retry.rawBody).isEqualTo(200)
        assertThat(retry.body!!.get("balance").asLong()).isEqualTo(10_000)
        assertThat(entryCountOf(accountId)).isEqualTo(1)
        assertThat(eventTypesOf(accountId)).containsExactly("AccountOpened", "LedgerEntryRecorded")
    }

    @Test
    fun `two concurrent openings with the same key write one account and one bonus entry`() {
        val customerId = UUID.randomUUID()
        val key = UUID.randomUUID().toString()
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val calls =
                List(2) {
                    pool.submit<Int> {
                        start.await()
                        api.openAccount(customerId, key).status
                    }
                }
            start.countDown()
            val statuses = calls.map { it.get(30, TimeUnit.SECONDS) }

            assertThat(statuses).describedAs("statuses").allMatch { it == 200 || it == 201 }
        } finally {
            pool.shutdownNow()
        }
        val accountIds =
            ownerJdbc
                .sql("select id from account where customer_id = :id")
                .param("id", customerId)
                .query(UUID::class.java)
                .list()
        assertThat(accountIds).hasSize(1)
        assertThat(entryCountOf(accountIds.single().toString())).isEqualTo(1)
        assertThat(eventTypesOf(accountIds.single().toString())).containsExactly("AccountOpened", "LedgerEntryRecorded")
    }

    @Test
    fun `a failure writing the bonus event rolls back the account, the entry and the opening event`() {
        val customerId = UUID.randomUUID()
        doAnswer { invocation ->
            if (invocation.getArgument<Any>(0) is LedgerEntryRecorded) throw IllegalStateException("outbox down")
            invocation.callRealMethod()
        }.`when`(outbox).append(anyEvent())

        val response = api.openAccount(customerId)

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(500)
        assertNothingWrittenFor(customerId)
        val entries =
            ownerJdbc
                .sql("select count(*) from ledger_entry e join account a on a.id = e.account_id where a.customer_id = :id")
                .param("id", customerId)
                .query(Long::class.java)
                .single()
        assertThat(entries).isZero()
    }

    @Test
    fun `reopening after closing starts at zero with no bonus entry and no bonus event`() {
        val customerId = UUID.randomUUID()
        val firstId =
            api
                .openAccount(customerId)
                .body!!
                .get("id")
                .asString()
        drainAndClose(firstId, customerId)

        val reopened = api.openAccount(customerId)

        assertThat(reopened.status).describedAs(reopened.rawBody).isEqualTo(201)
        assertThat(reopened.body!!.get("balance").asLong()).isZero()
        val secondId = reopened.body.get("id").asString()
        assertThat(secondId).isNotEqualTo(firstId)
        assertThat(entryCountOf(secondId)).isZero()
        assertThat(eventTypesOf(secondId)).containsExactly("AccountOpened")
        assertThat(entryCountOf(firstId)).isEqualTo(2)
    }

    @Test
    fun `retrying the reopening with the same key still returns the same zero-balance account`() {
        val customerId = UUID.randomUUID()
        drainAndClose(
            api
                .openAccount(customerId)
                .body!!
                .get("id")
                .asString(),
            customerId,
        )
        val key = UUID.randomUUID().toString()
        val first = api.openAccount(customerId, key)

        val retry = api.openAccount(customerId, key)

        assertThat(retry.status).describedAs(retry.rawBody).isEqualTo(200)
        assertThat(retry.body!!.get("id").asString()).isEqualTo(first.body!!.get("id").asString())
        assertThat(retry.body.get("balance").asLong()).isZero()
    }

    @Test
    fun `concurrent openings of a new customer with different keys write exactly one bonus in total`() {
        val customerId = UUID.randomUUID()
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val calls =
                List(2) {
                    pool.submit<Int> {
                        start.await()
                        api.openAccount(customerId).status
                    }
                }
            start.countDown()
            assertThat(calls.map { it.get(30, TimeUnit.SECONDS) }).containsExactlyInAnyOrder(201, 409)
        } finally {
            pool.shutdownNow()
        }
        assertThat(totalEntriesOf(customerId)).isEqualTo(1)
    }

    @Test
    fun `concurrent reopenings with different keys write no bonus at all`() {
        val customerId = UUID.randomUUID()
        drainAndClose(
            api
                .openAccount(customerId)
                .body!!
                .get("id")
                .asString(),
            customerId,
        )
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val calls =
                List(2) {
                    pool.submit<Int> {
                        start.await()
                        api.openAccount(customerId).status
                    }
                }
            start.countDown()
            assertThat(calls.map { it.get(30, TimeUnit.SECONDS) }).containsExactlyInAnyOrder(201, 409)
        } finally {
            pool.shutdownNow()
        }
        assertThat(totalEntriesOf(customerId)).isEqualTo(2)
    }

    private fun totalEntriesOf(customerId: UUID): Long =
        ownerJdbc
            .sql("select count(*) from ledger_entry e join account a on a.id = e.account_id where a.customer_id = :id")
            .param("id", customerId)
            .query(Long::class.java)
            .single()

    /** Debits the whole bonus through the service API, then closes the account as its owner. */
    private fun drainAndClose(
        accountId: String,
        customerId: UUID,
    ) {
        val debit =
            api.post(
                "/accounts/$accountId/debits",
                """{"amount":10000}""",
                mapOf("Idempotency-Key" to UUID.randomUUID().toString()) + bearer(JwtTestSupport.serviceToken()),
            )
        assertThat(debit.status).describedAs(debit.rawBody).isEqualTo(201)
        val close = api.send(HttpMethod.POST, "/accounts/$accountId/close", headers = asCustomer(customerId))
        assertThat(close.status).describedAs(close.rawBody).isEqualTo(200)
    }

    /** Mockito matcher for a non-null Kotlin parameter: registers `any()` and returns a placeholder event. */
    private fun anyEvent(): AccountEvent {
        any(AccountEvent::class.java)
        return AccountOpened(UUID.randomUUID(), UUID.randomUUID(), AccountType.FREE)
    }

    private fun eventTypesOf(accountId: String): List<String> =
        ownerJdbc
            .sql("select event_type from outbox where aggregate_id = :id order by position")
            .param("id", accountId)
            .query(String::class.java)
            .list()
            .map { it!! }

    private fun entryCountOf(accountId: String): Long =
        ownerJdbc
            .sql("select count(*) from ledger_entry where account_id = :id")
            .param("id", UUID.fromString(accountId))
            .query(Long::class.java)
            .single()
}
