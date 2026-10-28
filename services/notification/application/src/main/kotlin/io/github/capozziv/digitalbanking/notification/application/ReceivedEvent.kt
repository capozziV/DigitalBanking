package io.github.capozziv.digitalbanking.notification.application

import java.util.UUID

/** An event read from a queue, already decoded from its envelope. Money is in cents. */
sealed interface ReceivedEvent {
    val eventId: UUID

    /** The envelope's `eventType`, stored in `processed_events`. */
    val eventType: String

    data class AccountOpened(
        override val eventId: UUID,
        val accountId: UUID,
        val customerId: UUID,
        val accountType: String,
    ) : ReceivedEvent {
        override val eventType: String get() = "AccountOpened"
    }

    /** [overdraftLimitCents] is null for version 1 events, which did not carry it. */
    data class AccountTypeChanged(
        override val eventId: UUID,
        val accountId: UUID,
        val newType: String,
        val overdraftLimitCents: Long?,
    ) : ReceivedEvent {
        override val eventType: String get() = "AccountTypeChanged"
    }

    data class TransferCompleted(
        override val eventId: UUID,
        val transferId: UUID,
        val sourceAccountId: UUID,
        val destinationAccountId: UUID,
        val amountCents: Long,
    ) : ReceivedEvent {
        override val eventType: String get() = "TransferCompleted"
    }

    data class TransferFailed(
        override val eventId: UUID,
        val transferId: UUID,
        val sourceAccountId: UUID,
        val destinationAccountId: UUID,
        val amountCents: Long,
        val reason: String,
    ) : ReceivedEvent {
        override val eventType: String get() = "TransferFailed"
    }

    data class TransferCompensated(
        override val eventId: UUID,
        val transferId: UUID,
        val sourceAccountId: UUID,
        val destinationAccountId: UUID,
        val amountCents: Long,
    ) : ReceivedEvent {
        override val eventType: String get() = "TransferCompensated"
    }

    /** Any event type this service does not notify about. */
    data class Unsupported(
        override val eventId: UUID,
        override val eventType: String,
    ) : ReceivedEvent
}
