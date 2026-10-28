package io.github.capozziv.digitalbanking.notification.bootstrap.persistence

import io.github.capozziv.digitalbanking.notification.application.port.NotificationCursor
import io.github.capozziv.digitalbanking.notification.application.port.NotificationRepository
import io.github.capozziv.digitalbanking.notification.application.port.ProcessedEventRepository
import io.github.capozziv.digitalbanking.notification.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.notification.bootstrap.NotificationIntegrationTest
import io.github.capozziv.digitalbanking.notification.domain.NotificationKind
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import java.util.UUID

class JdbcNotificationRepositoryIntegrationTest : NotificationIntegrationTest() {
    @Autowired
    private lateinit var repository: NotificationRepository

    @Autowired
    private lateinit var processedEvents: ProcessedEventRepository

    @Autowired
    private lateinit var transactions: TransactionRunner

    @BeforeEach
    fun cleanTables() {
        ownerJdbc.sql("delete from notifications").update()
        ownerJdbc.sql("delete from processed_events").update()
    }

    @Test
    fun `saves and finds a notification with its data`() {
        val notification = aNotification(data = mapOf("amount" to "12345", "name" to "João \"Zé\""))

        repository.saveAll(listOf(notification))

        assertThat(repository.find(notification.id)).isEqualTo(notification)
        assertThat(repository.find(UUID.randomUUID())).isNull()
    }

    @Test
    fun `saves an empty list and a notification with empty data`() {
        repository.saveAll(emptyList())
        val notification = aNotification(data = emptyMap())

        repository.saveAll(listOf(notification))

        assertThat(repository.find(notification.id)).isEqualTo(notification)
    }

    @Test
    fun `unique constraint rejects the same event, customer and kind twice`() {
        val customer = UUID.randomUUID()
        val event = UUID.randomUUID()
        repository.saveAll(listOf(aNotification(customerId = customer, sourceEventId = event)))

        assertThatThrownBy {
            repository.saveAll(listOf(aNotification(customerId = customer, sourceEventId = event)))
        }.isInstanceOf(DuplicateKeyException::class.java)
    }

    @Test
    fun `same event can notify the same customer with different kinds`() {
        val customer = UUID.randomUUID()
        val event = UUID.randomUUID()

        repository.saveAll(
            listOf(
                aNotification(customerId = customer, sourceEventId = event, kind = NotificationKind.TRANSFER_SENT),
                aNotification(customerId = customer, sourceEventId = event, kind = NotificationKind.TRANSFER_RECEIVED),
            ),
        )

        assertThat(repository.page(customer, null, 10)).hasSize(2)
    }

    @Test
    fun `pages newest first, ties on created_at broken by id descending`() {
        val customer = UUID.randomUUID()
        val t0 = microsNow()
        val lowId = UUID.fromString("00000000-0000-7000-8000-000000000001")
        val highId = UUID.fromString("00000000-0000-7000-8000-000000000002")
        val oldest = aNotification(customerId = customer, createdAt = t0)
        val tieLow = aNotification(customerId = customer, createdAt = t0.plusSeconds(1), id = lowId)
        val tieHigh = aNotification(customerId = customer, createdAt = t0.plusSeconds(1), id = highId)
        val newest = aNotification(customerId = customer, createdAt = t0.plusSeconds(2))
        repository.saveAll(listOf(oldest, tieLow, newest, tieHigh))

        val first = repository.page(customer, null, 2)
        assertThat(first).containsExactly(newest, tieHigh)

        val second = repository.page(customer, NotificationCursor(tieHigh.createdAt, tieHigh.id), 2)
        assertThat(second).containsExactly(tieLow, oldest)

        assertThat(repository.page(customer, NotificationCursor(oldest.createdAt, oldest.id), 2)).isEmpty()
    }

    @Test
    fun `page only returns notifications of the customer`() {
        val customer = UUID.randomUUID()
        val mine = aNotification(customerId = customer)
        repository.saveAll(listOf(mine, aNotification()))

        assertThat(repository.page(customer, null, 10)).containsExactly(mine)
    }

    @Test
    fun `counts only unread notifications of the customer`() {
        val customer = UUID.randomUUID()
        repository.saveAll(
            listOf(
                aNotification(customerId = customer),
                aNotification(customerId = customer),
                aNotification(customerId = customer, readAt = microsNow()),
                aNotification(),
            ),
        )

        assertThat(repository.countUnread(customer)).isEqualTo(2L)
    }

    @Test
    fun `markReadUpTo marks the position and older, not newer, not other customers`() {
        val customer = UUID.randomUUID()
        val t0 = microsNow()
        val older = aNotification(customerId = customer, createdAt = t0)
        val target = aNotification(customerId = customer, createdAt = t0.plusSeconds(1))
        val newer = aNotification(customerId = customer, createdAt = t0.plusSeconds(2))
        val other = aNotification(createdAt = t0)
        repository.saveAll(listOf(older, target, newer, other))
        val at = microsNow().plusSeconds(10)

        val changed = repository.markReadUpTo(customer, target, at)

        assertThat(changed).isEqualTo(2)
        assertThat(repository.find(older.id)!!.readAt).isEqualTo(at)
        assertThat(repository.find(target.id)!!.readAt).isEqualTo(at)
        assertThat(repository.find(newer.id)!!.readAt).isNull()
        assertThat(repository.find(other.id)!!.readAt).isNull()
    }

    @Test
    fun `markReadUpTo uses the id to break a tie on created_at and keeps the first read instant`() {
        val customer = UUID.randomUUID()
        val t0 = microsNow()
        val lowId = UUID.fromString("00000000-0000-7000-8000-000000000001")
        val highId = UUID.fromString("00000000-0000-7000-8000-000000000002")
        val low = aNotification(customerId = customer, createdAt = t0, id = lowId)
        val high = aNotification(customerId = customer, createdAt = t0, id = highId)
        val alreadyRead = aNotification(customerId = customer, createdAt = t0.minusSeconds(5), readAt = t0)
        repository.saveAll(listOf(low, high, alreadyRead))
        val at = t0.plusSeconds(10)

        val changed = repository.markReadUpTo(customer, low, at)

        assertThat(changed).isEqualTo(1)
        assertThat(repository.find(low.id)!!.readAt).isEqualTo(at)
        assertThat(repository.find(high.id)!!.readAt).isNull()
        assertThat(repository.find(alreadyRead.id)!!.readAt).isEqualTo(t0)
    }

    @Test
    fun `a transaction that fails after markProcessed rolls everything back`() {
        val eventId = UUID.randomUUID()
        val notification = aNotification(sourceEventId = eventId)

        assertThatThrownBy {
            transactions.inTransaction {
                processedEvents.markProcessed(eventId, "AccountOpened", microsNow())
                repository.saveAll(listOf(notification))
                error("boom")
            }
        }.isInstanceOf(IllegalStateException::class.java).hasMessage("boom")

        assertThat(repository.find(notification.id)).isNull()
        assertThat(processedEvents.markProcessed(eventId, "AccountOpened", microsNow())).isTrue()
    }
}
