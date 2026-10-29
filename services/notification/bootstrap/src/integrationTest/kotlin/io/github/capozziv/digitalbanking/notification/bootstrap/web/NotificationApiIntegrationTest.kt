package io.github.capozziv.digitalbanking.notification.bootstrap.web

import io.github.capozziv.digitalbanking.notification.application.port.NotificationRepository
import io.github.capozziv.digitalbanking.notification.bootstrap.persistence.aNotification
import io.github.capozziv.digitalbanking.notification.domain.Notification
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

    @BeforeEach
    fun cleanTables() {
        ownerJdbc.sql("delete from notifications").update()
    }

    @Test
    fun `a customer without notifications gets an empty page`() {
        val response = api.get("/notifications?customerId=$customer")

        assertThat(response.status).describedAs(response.rawBody).isEqualTo(200)
        assertThat(response.body!!.get("items").size()).isZero()
        assertThat(response.body.get("unreadCount").asLong()).isZero()
        assertThat(response.body.get("nextCursor").isNull).isTrue()
    }

    @Test
    fun `the page has 20 notifications, newest first, and the cursor leads to the rest`() {
        val saved = seed(customer, 25)

        val first = api.get("/notifications?customerId=$customer")

        assertThat(first.status).describedAs(first.rawBody).isEqualTo(200)
        assertThat(ids(first)).isEqualTo(saved.take(20).map { it.id.toString() })
        assertThat(first.body!!.get("unreadCount").asLong()).isEqualTo(25)
        val cursor = first.body.get("nextCursor").asString()
        assertThat(cursor).isNotBlank()

        val second = api.get("/notifications?customerId=$customer&cursor=$cursor")

        assertThat(second.status).describedAs(second.rawBody).isEqualTo(200)
        assertThat(ids(second)).isEqualTo(saved.drop(20).map { it.id.toString() })
        assertThat(second.body!!.get("nextCursor").isNull).isTrue()
    }

    @Test
    fun `an item carries its fields and the data map`() {
        val notification = seed(customer, 1).single()

        val item =
            api
                .get("/notifications?customerId=$customer")
                .body!!
                .get("items")
                .get(0)

        assertThat(item.get("id").asString()).isEqualTo(notification.id.toString())
        assertThat(item.get("kind").asString()).isEqualTo("WELCOME")
        assertThat(item.get("title").asString()).isEqualTo(notification.title)
        assertThat(item.get("body").asString()).isEqualTo(notification.body)
        assertThat(item.get("data").get("accountId").asString()).isEqualTo(notification.data.getValue("accountId"))
        assertThat(item.get("createdAt").asString()).isNotBlank()
        assertThat(item.get("readAt").isNull).isTrue()
    }

    @Test
    fun `marking up to a notification reads it and the older ones, and repeating changes nothing`() {
        val saved = seed(customer, 5) // newest first
        val anchor = saved[2]
        val other = seed(UUID.randomUUID(), 1).single()

        val first = api.post("/notifications/read", read(customer, anchor.id))

        assertThat(first.status).describedAs(first.rawBody).isEqualTo(200)
        assertThat(first.body!!.get("marked").asInt()).isEqualTo(3)
        assertThat(
            api
                .get("/notifications?customerId=$customer")
                .body!!
                .get("unreadCount")
                .asLong(),
        ).isEqualTo(2)

        val repeated = api.post("/notifications/read", read(customer, anchor.id))

        assertThat(repeated.status).isEqualTo(200)
        assertThat(repeated.body!!.get("marked").asInt()).isZero()
        assertThat(repository.find(other.id)!!.readAt).isNull()
    }

    @Test
    fun `an upTo of another customer is 404 notification-not-found and marks nothing`() {
        val mine = seed(customer, 2)
        val foreign = seed(UUID.randomUUID(), 1).single()

        val response = api.post("/notifications/read", read(customer, foreign.id))

        assertProblem(response, 404, "notification-not-found")
        assertThat(repository.find(foreign.id)!!.readAt).isNull()
        assertThat(mine.map { repository.find(it.id)!!.readAt }).containsOnlyNulls()
    }

    @Test
    fun `an upTo that does not exist is 404 notification-not-found`() {
        assertProblem(api.post("/notifications/read", read(customer, UUID.randomUUID())), 404, "notification-not-found")
    }

    @Test
    fun `a customerId that is not a UUID is 400 validation-error`() {
        val response = api.get("/notifications?customerId=abc")

        assertProblem(response, 400, "validation-error")
        assertThat(response.errorFields()).containsExactly("customerId")
    }

    @Test
    fun `a missing customerId is 400 validation-error`() {
        assertProblem(api.get("/notifications"), 400, "validation-error")
    }

    @Test
    fun `a tampered cursor is 400 validation-error`() {
        val notDecodable = "%25%25%25"
        val wrongContent = Base64.getUrlEncoder().withoutPadding().encodeToString("hello|world".toByteArray())

        for (cursor in listOf(notDecodable, wrongContent)) {
            val response = api.get("/notifications?customerId=$customer&cursor=$cursor")

            assertProblem(response, 400, "validation-error")
            assertThat(response.errorFields()).describedAs(cursor).containsExactly("cursor")
        }
    }

    @Test
    fun `the read body must carry a customerId and an upTo that are UUIDs`() {
        assertProblem(api.post("/notifications/read", """{"customerId":"abc","upTo":"${UUID.randomUUID()}"}"""), 400, "validation-error")
        assertProblem(api.post("/notifications/read", """{"customerId":"$customer"}"""), 400, "validation-error")
        assertProblem(api.post("/notifications/read", "{"), 400, "validation-error")
    }

    @Test
    fun `X-Correlation-Id is returned as received when safe, and generated otherwise`() {
        val received = api.get("/notifications?customerId=$customer", mapOf("X-Correlation-Id" to "abc-123"))
        val generated = api.get("/notifications?customerId=$customer")
        val unsafe = api.get("/notifications?customerId=$customer", mapOf("X-Correlation-Id" to "bad id!"))

        assertThat(received.header("X-Correlation-Id")).isEqualTo("abc-123")
        assertThat(UUID.fromString(generated.header("X-Correlation-Id"))).isNotNull()
        assertThat(unsafe.header("X-Correlation-Id")).isNotEqualTo("bad id!")
    }

    @Test
    fun `an error response also carries the correlation id`() {
        val response = api.post("/notifications/read", read(customer, UUID.randomUUID()), mapOf("X-Correlation-Id" to "trace-1"))

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

    private fun read(
        customerId: UUID,
        upTo: UUID,
    ) = """{"customerId":"$customerId","upTo":"$upTo"}"""
}
