package io.github.capozziv.digitalbanking.notification.application.usecase

import io.github.capozziv.digitalbanking.notification.application.fake.InMemoryNotificationRepository
import io.github.capozziv.digitalbanking.notification.application.port.NotificationCursor
import io.github.capozziv.digitalbanking.notification.domain.Notification
import io.github.capozziv.digitalbanking.notification.domain.NotificationKind
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class ListNotificationsUseCaseTest {
    private val repository = InMemoryNotificationRepository()
    private val useCase = ListNotificationsUseCase(repository)
    private val customer = UUID.randomUUID()
    private val start = Instant.parse("2026-10-28T12:00:00Z")

    @Test
    fun `the first page has the 20 newest notifications and a cursor to the next`() {
        val stored = store(25)

        val page = useCase.list(customer, null)

        assertThat(page.items).hasSize(20)
        assertThat(page.items.map { it.id }).isEqualTo(stored.reversed().take(20).map { it.id })
        val last = page.items.last()
        assertThat(page.nextCursor).isEqualTo(NotificationCursor(last.createdAt, last.id))
    }

    @Test
    fun `the second page has the rest and no next cursor`() {
        val stored = store(25)
        val first = useCase.list(customer, null)

        val second = useCase.list(customer, first.nextCursor)

        assertThat(second.items.map { it.id }).isEqualTo(stored.take(5).reversed().map { it.id })
        assertThat(second.nextCursor).isNull()
    }

    @Test
    fun `exactly one full page has no next cursor`() {
        store(20)

        assertThat(useCase.list(customer, null).nextCursor).isNull()
    }

    @Test
    fun `the unread count covers every notification of the customer`() {
        val stored = store(25)
        repository.markReadUpTo(customer, stored[2], start)
        repository.saveAll(listOf(notification(UUID.randomUUID(), 99)))

        assertThat(useCase.list(customer, null).unreadCount).isEqualTo(22)
    }

    @Test
    fun `a customer without notifications gets an empty page`() {
        val page = useCase.list(customer, null)

        assertThat(page.items).isEmpty()
        assertThat(page.unreadCount).isZero()
        assertThat(page.nextCursor).isNull()
    }

    @Test
    fun `notifications with the same creation time are listed by id descending`() {
        val tied = (1..3).map { tied(it) }
        repository.saveAll(tied)

        val page = useCase.list(customer, null)

        assertThat(page.items.map { it.id }).isEqualTo(tied.sortedByDescending { it.id.toString() }.map { it.id })
    }

    @Test
    fun `paging across a tie on the page boundary neither repeats nor skips`() {
        val newer = store(19)
        val tied = (1..3).map { notification(customer, 100 + it).copy(createdAt = start.minusSeconds(60)) }
        repository.saveAll(tied)
        val expected = (newer + tied).sortedWith(compareByDescending<Notification> { it.createdAt }.thenByDescending { it.id.toString() })

        val first = useCase.list(customer, null)
        val second = useCase.list(customer, first.nextCursor)

        assertThat(first.items).hasSize(20)
        assertThat(second.items).hasSize(2)
        assertThat(first.items.last().createdAt).isEqualTo(second.items.first().createdAt)
        assertThat(first.items.map { it.id } + second.items.map { it.id }).isEqualTo(expected.map { it.id })
        assertThat(second.nextCursor).isNull()
    }

    private fun tied(index: Int) = notification(customer, 100 + index).copy(createdAt = start.plusSeconds(1000))

    private fun store(count: Int): List<Notification> {
        val created = (0 until count).map { notification(customer, it) }
        repository.saveAll(created)
        return created
    }

    private fun notification(
        owner: UUID,
        index: Int,
    ) = Notification(
        id = UUID(0x0190_0000_0000_7000L, index.toLong() + 1),
        customerId = owner,
        kind = NotificationKind.WELCOME,
        title = "t$index",
        body = "b$index",
        data = emptyMap(),
        sourceEventId = UUID.randomUUID(),
        createdAt = start.plusSeconds(index.toLong()),
        readAt = null,
    )
}
