package io.github.capozziv.digitalbanking.notification.bootstrap.web

import io.github.capozziv.digitalbanking.notification.application.port.NotificationRepository
import io.github.capozziv.digitalbanking.notification.bootstrap.persistence.aNotification
import io.github.capozziv.digitalbanking.notification.domain.Notification
import io.github.capozziv.digitalbanking.notification.domain.NotificationKind
import io.github.capozziv.digitalbanking.testing.security.JwtTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.UUID

/** The notification API: list with cursor, mark as read, Problem Details and the correlation id. */
class NotificationApiIntegrationTest : NotificationWebIntegrationTest() {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var repository: NotificationRepository

    private val api by lazy { ApiClient(port) }

    private val customer = UUID.randomUUID()

    /** As [customer] by default; the access rules are in [NotificationAccessIntegrationTest]. */
    private fun get(
        path: String,
        headers: Map<String, String> = emptyMap(),
        bearer: String = JwtTestSupport.customerToken(customer),
    ) = api.get(path, headers, bearer)

    private fun post(
        path: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
        bearer: String = JwtTestSupport.customerToken(customer),
    ) = api.post(path, body, headers, bearer)

    @BeforeEach
    fun cleanTables() {
        ownerJdbc.sql("delete from notifications").update()
        ownerJdbc.sql("delete from customer_names").update()
        ownerJdbc.sql("delete from known_accounts").update()
    }

    @Test
    fun `a customer without notifications gets an empty page`() {
        val response = get("/notifications")

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.get("items").size()).isZero()
        assertThat(response.body.get("unreadCount").asLong()).isZero()
        assertThat(response.body.get("nextCursor").isNull).isTrue()
    }

    @Test
    fun `the page has 20 notifications, newest first, and the cursor leads to the rest`() {
        val saved = seed(customer, 25)

        val first = get("/notifications")

        assertThat(first.status).describedAs(first.rawBody).isEqualTo(200)
        assertThat(ids(first)).isEqualTo(saved.take(20).map { it.id.toString() })
        assertThat(first.body!!.get("unreadCount").asLong()).isEqualTo(25)
        val cursor = first.body.get("nextCursor").asString()
        assertThat(cursor).isNotBlank()

        val second = get("/notifications?cursor=$cursor")

        assertThat(second.status).describedAs(second.rawBody).isEqualTo(200)
        assertThat(ids(second)).isEqualTo(saved.drop(20).map { it.id.toString() })
        assertThat(second.body!!.get("nextCursor").isNull).isTrue()
    }

    @Test
    fun `an item carries its fields and the data map`() {
        val notification = seed(customer, 1).single()

        val item =
            get("/notifications")
                .body!!
                .get("items")
                .get(0)

        assertThat(item.get("id").asString()).isEqualTo(notification.id.toString())
        assertThat(item.get("kind").asString()).isEqualTo("WELCOME")
        assertThat(item.get("title").asString()).isEqualTo("Bem-vindo ao capzzPay!")
        assertThat(item.get("body").asString()).doesNotContain(notification.data.getValue("accountId").toString())
        assertThat(item.get("data").get("accountId").asString()).isEqualTo(notification.data.getValue("accountId"))
        assertThat(item.get("createdAt").asString()).isNotBlank()
        assertThat(item.get("readAt").isNull).isTrue()
    }

    @Test
    fun `an amount in cents of the data map is a JSON number`() {
        repository.saveAll(listOf(aNotification(customerId = customer, data = mapOf("amountCents" to 250_000L))))

        val data =
            get("/notifications")
                .body!!
                .get("items")
                .get(0)
                .get("data")

        assertThat(data.get("amountCents").isIntegralNumber).isTrue()
        assertThat(data.get("amountCents").asLong()).isEqualTo(250_000L)
    }

    @Test
    fun `a received transfer is told with the full name of the sender, never the account`() {
        val (sender, senderAccount) = UUID.randomUUID() to UUID.randomUUID()
        ownerJdbc.sql("insert into known_accounts (account_id, customer_id) values (?, ?)").params(senderAccount, sender).update()
        ownerJdbc.sql("insert into customer_names (customer_id, name) values (?, ?)").params(sender, "Ana Souza").update()
        repository.saveAll(listOf(receivedTransfer(senderAccount)))

        val item = get("/notifications").body!!.get("items").get(0)

        assertThat(item.get("title").asString()).isEqualTo("Você recebeu uma transferência")
        assertThat(item.get("body").asString()).isEqualTo("Você recebeu R$ 25,00 de Ana Souza.")
    }

    @Test
    fun `a transfer stored with the account in its text, before the names, says another customer while the name is unknown`() {
        val senderAccount = UUID.randomUUID()
        repository.saveAll(listOf(receivedTransfer(senderAccount)))

        val body =
            get("/notifications")
                .body!!
                .get("items")
                .get(0)
                .get("body")
                .asString()

        assertThat(body).isEqualTo("Você recebeu R$ 25,00 de outro cliente.").doesNotContain(senderAccount.toString())
    }

    @Test
    fun `account notifications stored with the old text and the account number are read with the new text`() {
        val account = UUID.randomUUID()
        val data = mapOf<String, Any>("accountId" to account.toString(), "accountType" to "GOLD")
        repository.saveAll(
            listOf(
                stored(NotificationKind.WELCOME, data, "Bem-vindo ao banco!", "Pelo número $account no sininho", 4),
                stored(NotificationKind.ACCOUNT_BLOCKED, data, "Conta bloqueada", "Sua conta $account foi bloqueada.", 3),
                stored(NotificationKind.ACCOUNT_UNBLOCKED, data, "Conta desbloqueada", "Sua conta $account foi desbloqueada.", 2),
                stored(NotificationKind.ACCOUNT_CLOSED, data, "Conta encerrada", "Sua conta $account foi encerrada.", 1),
            ),
        )

        val items = get("/notifications").body!!.get("items")

        val byKind = (0 until items.size()).associate { items.get(it).get("kind").asString() to items.get(it) }
        assertThat(byKind.getValue("WELCOME").get("title").asString()).isEqualTo("Bem-vindo ao capzzPay!")
        assertThat(byKind.getValue("WELCOME").get("body").asString()).startsWith("Sua conta Gold está aberta.")
        assertThat(byKind.getValue("ACCOUNT_BLOCKED").get("body").asString()).startsWith("Sua conta foi bloqueada.")
        assertThat(byKind.getValue("ACCOUNT_UNBLOCKED").get("body").asString()).startsWith("Sua conta foi desbloqueada")
        assertThat(byKind.getValue("ACCOUNT_CLOSED").get("body").asString()).startsWith("Sua conta foi encerrada.")
        byKind.values.forEach {
            assertThat(it.get("title").asString() + it.get("body").asString()).doesNotContain(account.toString())
        }
    }

    private fun stored(
        kind: NotificationKind,
        data: Map<String, Any>,
        title: String,
        body: String,
        minutesAgo: Long,
    ) = aNotification(customerId = customer, kind = kind, data = data, createdAt = Instant.now().minus(minutesAgo, ChronoUnit.MINUTES))
        .copy(title = title, body = body)

    /** As the service wrote it before the names existed: the account of the sender in the stored text. */
    private fun receivedTransfer(senderAccount: UUID) =
        aNotification(
            customerId = customer,
            kind = NotificationKind.TRANSFER_RECEIVED,
            data =
                mapOf(
                    "transferId" to UUID.randomUUID().toString(),
                    "sourceAccountId" to senderAccount.toString(),
                    "destinationAccountId" to UUID.randomUUID().toString(),
                    "amountCents" to 2500L,
                ),
        ).copy(body = "Você recebeu R$ 25,00 da conta $senderAccount.")

    @Test
    fun `marking up to a notification reads it and the older ones, and repeating changes nothing`() {
        val saved = seed(customer, 5) // newest first
        val anchor = saved[2]
        val other = seed(UUID.randomUUID(), 1).single()

        val first = post("/notifications/read", read(anchor.id))

        assertThat(first.status).describedAs(first.rawBody).isEqualTo(200)
        assertThat(first.body!!.get("marked").asInt()).isEqualTo(3)
        assertThat(
            get("/notifications")
                .body!!
                .get("unreadCount")
                .asLong(),
        ).isEqualTo(2)

        val repeated = post("/notifications/read", read(anchor.id))

        assertThat(repeated.status).isEqualTo(200)
        assertThat(repeated.body!!.get("marked").asInt()).isZero()
        assertThat(repository.find(other.id)!!.readAt).isNull()
    }

    @Test
    fun `an upTo of another customer is 404 notification-not-found and marks nothing`() {
        val mine = seed(customer, 2)
        val foreign = seed(UUID.randomUUID(), 1).single()

        val response = post("/notifications/read", read(foreign.id))

        assertProblem(response, 404, "notification-not-found")
        assertThat(repository.find(foreign.id)!!.readAt).isNull()
        assertThat(mine.map { repository.find(it.id)!!.readAt }).containsOnlyNulls()
    }

    @Test
    fun `an upTo that does not exist is 404 notification-not-found`() {
        assertProblem(post("/notifications/read", read(UUID.randomUUID())), 404, "notification-not-found")
    }

    @Test
    fun `a customerId that is not a UUID is 400 validation-error`() {
        val response = get("/notifications?customerId=abc", bearer = JwtTestSupport.adminToken(UUID.randomUUID()))

        assertProblem(response, 400, "validation-error")
        assertThat(response.errorFields()).containsExactly("customerId")
    }

    @Test
    fun `a tampered cursor is 400 validation-error`() {
        val notDecodable = "%25%25%25"

        fun encoded(raw: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray())
        val notAnInstant = encoded("hello|${UUID.randomUUID()}")
        val notAUuid = encoded("2026-10-28T12:00:00Z|world")
        val threeParts = encoded("2026-10-28T12:00:00Z|${UUID.randomUUID()}|extra")

        for (cursor in listOf(notDecodable, notAnInstant, notAUuid, threeParts)) {
            val response = get("/notifications?cursor=$cursor")

            assertProblem(response, 400, "validation-error")
            assertThat(response.errorFields()).describedAs(cursor).containsExactly("cursor")
        }
    }

    @Test
    fun `the read body must carry an upTo that is a UUID`() {
        assertProblem(post("/notifications/read", """{"upTo":"abc"}"""), 400, "validation-error")
        assertProblem(post("/notifications/read", """{}"""), 400, "validation-error")
        assertProblem(post("/notifications/read", "{"), 400, "validation-error")
    }

    @Test
    fun `X-Correlation-Id is returned as received when safe, and generated otherwise`() {
        val received = get("/notifications", mapOf("X-Correlation-Id" to "abc-123"))
        val generated = get("/notifications")
        val unsafe = get("/notifications", mapOf("X-Correlation-Id" to "bad id!"))

        assertThat(received.header("X-Correlation-Id")).isEqualTo("abc-123")
        assertThat(UUID.fromString(generated.header("X-Correlation-Id"))).isNotNull()
        assertThat(unsafe.header("X-Correlation-Id")).isNotEqualTo("bad id!")
    }

    @Test
    fun `an error response also carries the correlation id`() {
        val response = post("/notifications/read", read(UUID.randomUUID()), mapOf("X-Correlation-Id" to "trace-1"))

        assertThat(response.header("X-Correlation-Id")).isEqualTo("trace-1")
    }

    /** Seeds [count] unread notifications, one second apart, and returns them newest first. */
    private fun seed(
        customerId: UUID,
        count: Int,
    ): List<Notification> {
        val base = Instant.now().truncatedTo(ChronoUnit.MICROS).minusSeconds(count.toLong())
        val notifications = (0 until count).map { aNotification(customerId = customerId, createdAt = base.plusSeconds(it.toLong())) }
        repository.saveAll(notifications)
        return notifications.sortedByDescending { it.createdAt }
    }

    private fun ids(response: ApiResponse): List<String> =
        response.body!!.get("items").let { items -> (0 until items.size()).map { items.get(it).get("id").asString() } }

    private fun read(upTo: UUID) = """{"upTo":"$upTo"}"""
}
