package io.github.capozziv.digitalbanking.accounts.bootstrap.web

import io.github.capozziv.digitalbanking.accounts.application.port.EventOutbox
import io.github.capozziv.digitalbanking.accounts.bootstrap.AccountsIntegrationTest
import io.github.capozziv.digitalbanking.accounts.domain.AccountEvent
import io.github.capozziv.digitalbanking.accounts.domain.AccountOpened
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.LedgerEntryRecorded
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doAnswer
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.util.UUID

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
