package io.github.capozziv.digitalbanking.notification.application.fake

import io.github.capozziv.digitalbanking.notification.application.port.NotificationCursor
import io.github.capozziv.digitalbanking.notification.application.port.NotificationRepository
import io.github.capozziv.digitalbanking.notification.domain.Notification
import java.time.Instant
import java.util.UUID

/**
 * Mirrors the SQL contract: newest first by (createdAt desc, id desc), cursor exclusive,
 * and `UNIQUE (source_event_id, customer_id, kind)`.
 */
class InMemoryNotificationRepository(
    private val transactions: ImmediateTransactionRunner? = null,
) : NotificationRepository {
    private val rows = linkedMapOf<UUID, Notification>()

    val all: List<Notification> get() = rows.values.toList()

    override fun saveAll(notifications: List<Notification>) {
        transactions?.let { check(it.open) { "saveAll outside a transaction" } }
        notifications.forEach { notification ->
            val clash =
                rows.values.any {
                    it.sourceEventId == notification.sourceEventId &&
                        it.customerId == notification.customerId &&
                        it.kind == notification.kind
                }
            check(!clash) { "unique (source_event_id, customer_id, kind) violated by ${notification.id}" }
            rows[notification.id] = notification
        }
    }

    override fun page(
        customerId: UUID,
        cursor: NotificationCursor?,
        size: Int,
    ): List<Notification> =
        rows.values
            .filter { it.customerId == customerId }
            .filter { cursor == null || isOlder(it, cursor.createdAt, cursor.id) }
            .sortedWith(NEWEST_FIRST)
            .take(size)

    override fun countUnread(customerId: UUID): Long = rows.values.count { it.customerId == customerId && it.readAt == null }.toLong()

    override fun find(id: UUID): Notification? = rows[id]

    override fun markReadUpTo(
        customerId: UUID,
        upTo: Notification,
        at: Instant,
    ): Int {
        transactions?.let { check(it.open) { "markReadUpTo outside a transaction" } }
        val targets =
            rows.values.filter {
                it.customerId == customerId &&
                    it.readAt == null &&
                    (it.id == upTo.id || isOlder(it, upTo.createdAt, upTo.id))
            }
        targets.forEach { rows[it.id] = it.markRead(at) }
        return targets.size
    }

    private fun isOlder(
        notification: Notification,
        createdAt: Instant,
        id: UUID,
    ): Boolean {
        val byTime = notification.createdAt.compareTo(createdAt)
        return byTime < 0 || (byTime == 0 && notification.id.toString() < id.toString())
    }

    private companion object {
        val NEWEST_FIRST =
            compareByDescending<Notification> { it.createdAt }.thenByDescending { it.id.toString() }
    }
}
