package io.github.capozziv.digitalbanking.notification.application.usecase

import io.github.capozziv.digitalbanking.notification.application.ReceivedEvent
import io.github.capozziv.digitalbanking.notification.application.UnknownAccountException
import io.github.capozziv.digitalbanking.notification.application.port.IdGenerator
import io.github.capozziv.digitalbanking.notification.application.port.KnownAccountRepository
import io.github.capozziv.digitalbanking.notification.application.port.NotificationRepository
import io.github.capozziv.digitalbanking.notification.application.port.ProcessedEventRepository
import io.github.capozziv.digitalbanking.notification.application.port.TransactionRunner
import io.github.capozziv.digitalbanking.notification.domain.Notification
import io.github.capozziv.digitalbanking.notification.domain.NotificationContent
import io.github.capozziv.digitalbanking.notification.domain.NotificationKind
import io.github.capozziv.digitalbanking.notification.domain.NotificationText
import java.time.Clock
import java.time.Instant
import java.util.UUID

enum class ConsumeOutcome { PROCESSED, DUPLICATE, IGNORED }

/**
 * Applies each event exactly once: the `processed_events` row, the account → customer copy
 * and the notifications are written in one transaction, so a redelivery after the commit is a no-op.
 */
class ConsumeEventUseCase(
    private val processedEvents: ProcessedEventRepository,
    private val knownAccounts: KnownAccountRepository,
    private val notifications: NotificationRepository,
    private val transactions: TransactionRunner,
    private val ids: IdGenerator,
    private val clock: Clock,
) {
    fun execute(event: ReceivedEvent): ConsumeOutcome =
        transactions.inTransaction {
            val now = clock.instant()
            // Owners are resolved before anything is written, so an unknown account leaves no trace.
            val drafts = draftsFor(event)
            if (!processedEvents.markProcessed(event.eventId, event.eventType, now)) {
                return@inTransaction ConsumeOutcome.DUPLICATE
            }
            if (event is ReceivedEvent.Unsupported) return@inTransaction ConsumeOutcome.IGNORED
            if (event is ReceivedEvent.AccountOpened) knownAccounts.save(event.accountId, event.customerId)
            notifications.saveAll(drafts.map { it.toNotification(event.eventId, now) })
            ConsumeOutcome.PROCESSED
        }

    private fun draftsFor(event: ReceivedEvent): List<Draft> =
        when (event) {
            is ReceivedEvent.AccountOpened -> {
                listOf(
                    Draft(
                        event.customerId,
                        NotificationKind.WELCOME,
                        NotificationText.welcome(event.accountType, event.accountId),
                        mapOf("accountId" to event.accountId.toString(), "accountType" to event.accountType),
                    ),
                )
            }

            is ReceivedEvent.AccountTypeChanged -> {
                listOf(
                    Draft(
                        ownerOf(event.accountId),
                        NotificationKind.ACCOUNT_TYPE_CHANGED,
                        NotificationText.accountTypeChanged(event.newType, event.overdraftLimitCents),
                        buildMap {
                            put("accountId", event.accountId.toString())
                            put("accountType", event.newType)
                            event.overdraftLimitCents?.let { put("overdraftLimitCents", it.toString()) }
                        },
                    ),
                )
            }

            is ReceivedEvent.TransferCompleted -> {
                transferCompletedDrafts(event)
            }

            is ReceivedEvent.TransferFailed -> {
                listOf(
                    Draft(
                        ownerOf(event.sourceAccountId),
                        NotificationKind.TRANSFER_FAILED,
                        NotificationText.transferFailed(event.amountCents, event.destinationAccountId, event.reason),
                        transferData(event.transferId, event.sourceAccountId, event.destinationAccountId, event.amountCents) +
                            ("reason" to event.reason),
                    ),
                )
            }

            is ReceivedEvent.TransferCompensated -> {
                listOf(
                    Draft(
                        ownerOf(event.sourceAccountId),
                        NotificationKind.TRANSFER_COMPENSATED,
                        NotificationText.transferCompensated(event.amountCents, event.destinationAccountId),
                        transferData(event.transferId, event.sourceAccountId, event.destinationAccountId, event.amountCents),
                    ),
                )
            }

            is ReceivedEvent.Unsupported -> {
                emptyList()
            }
        }

    /** Both owners get a notification even when they are the same customer (two accounts of theirs). */
    private fun transferCompletedDrafts(event: ReceivedEvent.TransferCompleted): List<Draft> {
        val sender = ownerOf(event.sourceAccountId)
        val receiver = ownerOf(event.destinationAccountId)
        val data = transferData(event.transferId, event.sourceAccountId, event.destinationAccountId, event.amountCents)
        return listOf(
            Draft(
                sender,
                NotificationKind.TRANSFER_SENT,
                NotificationText.transferSent(event.amountCents, event.destinationAccountId),
                data,
            ),
            Draft(
                receiver,
                NotificationKind.TRANSFER_RECEIVED,
                NotificationText.transferReceived(event.amountCents, event.sourceAccountId),
                data,
            ),
        )
    }

    private fun ownerOf(accountId: UUID): UUID = knownAccounts.customerOf(accountId) ?: throw UnknownAccountException(accountId)

    private fun transferData(
        transferId: UUID,
        sourceAccountId: UUID,
        destinationAccountId: UUID,
        amountCents: Long,
    ) = mapOf(
        "transferId" to transferId.toString(),
        "sourceAccountId" to sourceAccountId.toString(),
        "destinationAccountId" to destinationAccountId.toString(),
        "amountCents" to amountCents.toString(),
    )

    private inner class Draft(
        val customerId: UUID,
        val kind: NotificationKind,
        val content: NotificationContent,
        val data: Map<String, String>,
    ) {
        fun toNotification(
            sourceEventId: UUID,
            at: Instant,
        ) = Notification(
            id = ids.newId(),
            customerId = customerId,
            kind = kind,
            title = content.title,
            body = content.body,
            data = data,
            sourceEventId = sourceEventId,
            createdAt = at,
            readAt = null,
        )
    }
}
