package io.github.capozziv.digitalbanking.notification.application.usecase

import io.github.capozziv.digitalbanking.notification.application.NotificationNotFoundException
import io.github.capozziv.digitalbanking.notification.application.fake.ImmediateTransactionRunner
import io.github.capozziv.digitalbanking.notification.application.fake.InMemoryNotificationRepository
import io.github.capozziv.digitalbanking.notification.application.fake.MutableClock
import io.github.capozziv.digitalbanking.notification.domain.Notification
import io.github.capozziv.digitalbanking.notification.domain.NotificationKind
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class MarkNotificationsReadUseCaseTest {
    private val start = Instant.parse("2026-10-28T12:00:00Z")
    private val readAt = Instant.parse("2026-10-28T13:00:00Z")
    private val transactions = ImmediateTransactionRunner()
    private val repository = InMemoryNotificationRepository(transactions)
    private val useCase = MarkNotificationsReadUseCase(repository, transactions, MutableClock(readAt))
    private val customer = UUID.randomUUID()
    private val other = UUID.randomUUID()

    @Test
    fun `marks up to the given notification inclusive and leaves the newer ones unread`() {
        val stored = store(customer, 5, firstIndex = 0)
        val transactionsBefore = transactions.transactions

        val marked = useCase.markRead(customer, stored[2].id)

        assertThat(marked).isEqualTo(3)
        val byId = repository.all.associateBy { it.id }
        assertThat(stored.take(3).map { byId.getValue(it.id).readAt }).containsOnly(readAt)
        assertThat(stored.drop(3).map { byId.getValue(it.id).readAt }).containsOnlyNulls()
        assertThat(transactions.transactions).isEqualTo(transactionsBefore + 1)
    }

    @Test
    fun `does not touch notifications of other customers`() {
        val mine = store(customer, 2, firstIndex = 0)
        val theirs = store(other, 2, firstIndex = 10)

        useCase.markRead(customer, mine.last().id)

        val byId = repository.all.associateBy { it.id }
        assertThat(theirs.map { byId.getValue(it.id).readAt }).containsOnlyNulls()
    }

    @Test
    fun `already read notifications are not counted again`() {
        val stored = store(customer, 3, firstIndex = 0)
        useCase.markRead(customer, stored[1].id)

        assertThat(useCase.markRead(customer, stored[2].id)).isEqualTo(1)
    }

    @Test
    fun `up to a notification of another customer is not found and marks nothing`() {
        store(customer, 2, firstIndex = 0)
        val theirs = store(other, 1, firstIndex = 10).single()

        assertThatThrownBy { useCase.markRead(customer, theirs.id) }
            .isInstanceOf(NotificationNotFoundException::class.java)
            .satisfies({ assertThat((it as NotificationNotFoundException).notificationId).isEqualTo(theirs.id) })

        assertThat(repository.all.map { it.readAt }).containsOnlyNulls()
    }

    @Test
    fun `up to an unknown notification is not found`() {
        assertThatThrownBy { useCase.markRead(customer, UUID.randomUUID()) }
            .isInstanceOf(NotificationNotFoundException::class.java)
    }

    private fun store(
        owner: UUID,
        count: Int,
        firstIndex: Int,
    ): List<Notification> {
        val created =
            (firstIndex until firstIndex + count).map { index ->
                Notification(
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
        transactions.inTransaction { repository.saveAll(created) }
        return created
    }
}
