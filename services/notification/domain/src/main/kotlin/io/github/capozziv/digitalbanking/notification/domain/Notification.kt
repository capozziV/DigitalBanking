package io.github.capozziv.digitalbanking.notification.domain

import java.time.Instant
import java.util.UUID

/**
 * [data] holds the identifiers of the event as strings and its amounts in cents as [Long], so a client reads the
 * amounts as JSON numbers.
 */
data class Notification(
    val id: UUID,
    val customerId: UUID,
    val kind: NotificationKind,
    val title: String,
    val body: String,
    val data: Map<String, Any>,
    val sourceEventId: UUID,
    val createdAt: Instant,
    val readAt: Instant?,
) {
    /** Idempotent: an already read notification keeps its original [readAt]. */
    fun markRead(at: Instant): Notification = if (readAt != null) this else copy(readAt = at)

    /**
     * The account of the other party of a transfer: the receiver's for the sender (sent, failed, compensated), the
     * sender's for the receiver. Null for a notification about the customer's own account.
     */
    fun counterpartAccountId(): UUID? =
        when (kind) {
            NotificationKind.TRANSFER_SENT,
            NotificationKind.TRANSFER_FAILED,
            NotificationKind.TRANSFER_COMPENSATED,
            -> data["destinationAccountId"]

            NotificationKind.TRANSFER_RECEIVED -> data["sourceAccountId"]

            NotificationKind.WELCOME,
            NotificationKind.ACCOUNT_TYPE_CHANGED,
            NotificationKind.ACCOUNT_BLOCKED,
            NotificationKind.ACCOUNT_UNBLOCKED,
            NotificationKind.ACCOUNT_CLOSED,
            -> null
        }?.let { UUID.fromString(it as String) }
}
