package io.github.capozziv.digitalbanking.notification.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class NotificationTest {
    private val notification =
        Notification(
            id = UUID.fromString("0190f7a0-0000-7000-8000-000000000010"),
            customerId = UUID.fromString("0190f7a0-0000-7000-8000-000000000011"),
            kind = NotificationKind.WELCOME,
            title = "t",
            body = "b",
            data = mapOf("accountId" to "x"),
            sourceEventId = UUID.fromString("0190f7a0-0000-7000-8000-000000000012"),
            createdAt = Instant.parse("2026-10-28T10:00:00Z"),
            readAt = null,
        )

    @Test
    fun `markRead sets readAt`() {
        val at = Instant.parse("2026-10-28T11:00:00Z")
        assertThat(notification.markRead(at).readAt).isEqualTo(at)
        assertThat(notification.readAt).isNull()
    }

    @Test
    fun `markRead keeps the original readAt when already read`() {
        val first = Instant.parse("2026-10-28T11:00:00Z")
        val read = notification.markRead(first)
        assertThat(read.markRead(Instant.parse("2026-10-28T12:00:00Z")).readAt).isEqualTo(first)
    }
}
