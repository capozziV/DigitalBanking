package io.github.capozziv.digitalbanking.notification.bootstrap.persistence

import io.github.capozziv.digitalbanking.notification.domain.Notification
import io.github.capozziv.digitalbanking.notification.domain.NotificationKind
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/** An instant with microsecond precision, which is what `timestamptz` stores. */
internal fun microsNow(): Instant = Instant.now().truncatedTo(ChronoUnit.MICROS)

internal fun aNotification(
    customerId: UUID = UUID.randomUUID(),
    createdAt: Instant = microsNow(),
    id: UUID = UUID.randomUUID(),
    kind: NotificationKind = NotificationKind.WELCOME,
    sourceEventId: UUID = UUID.randomUUID(),
    readAt: Instant? = null,
    data: Map<String, String> = mapOf("accountId" to UUID.randomUUID().toString()),
): Notification =
    Notification(
        id = id,
        customerId = customerId,
        kind = kind,
        title = "Título",
        body = "Corpo da notificação",
        data = data,
        sourceEventId = sourceEventId,
        createdAt = createdAt,
        readAt = readAt,
    )
