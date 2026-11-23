package io.github.capozziv.digitalbanking.notification.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/** The text shown to the customer is built when the notification is read, with the name of the other party. */
class NotificationRenderingTest {
    private val source = UUID.fromString("0190f7a0-0000-7000-8000-000000000001")
    private val destination = UUID.fromString("0190f7a0-0000-7000-8000-000000000002")

    @Test
    fun `a sent transfer names the receiver`() {
        val content = NotificationText.render(transfer(NotificationKind.TRANSFER_SENT), "Ana Souza")

        assertThat(content).isEqualTo(NotificationContent("Transferência enviada", "Você enviou R$ 123,45 para Ana Souza."))
    }

    @Test
    fun `a received transfer names the sender`() {
        val content = NotificationText.render(transfer(NotificationKind.TRANSFER_RECEIVED), "Ana Souza")

        assertThat(content.body).isEqualTo("Você recebeu R$ 123,45 de Ana Souza.")
    }

    @Test
    fun `a failed transfer names the receiver and keeps the reason`() {
        val content =
            NotificationText.render(
                transfer(NotificationKind.TRANSFER_FAILED, "reason" to "insufficient-available-balance"),
                "Ana Souza",
            )

        assertThat(content.body).isEqualTo(
            "Sua transferência de R$ 123,45 para Ana Souza foi recusada: saldo disponível insuficiente. Nenhum valor saiu da sua conta.",
        )
    }

    @Test
    fun `a compensated transfer names the receiver, with or without a reason`() {
        assertThat(NotificationText.render(transfer(NotificationKind.TRANSFER_COMPENSATED), "Ana Souza").body).isEqualTo(
            "Sua transferência de R$ 123,45 para Ana Souza não pôde ser concluída e o valor voltou para a sua conta.",
        )
        assertThat(
            NotificationText.render(transfer(NotificationKind.TRANSFER_COMPENSATED, "reason" to "account-closed"), "Ana Souza").body,
        ).isEqualTo(
            "Sua transferência de R$ 123,45 para Ana Souza não pôde ser concluída: " +
                "uma das contas está encerrada. O valor voltou para a sua conta.",
        )
    }

    @Test
    fun `without the name yet the text says another customer and never the account`() {
        val sent = NotificationText.render(transfer(NotificationKind.TRANSFER_SENT), null)
        val received = NotificationText.render(transfer(NotificationKind.TRANSFER_RECEIVED), null)

        assertThat(sent.body).isEqualTo("Você enviou R$ 123,45 para outro cliente.")
        assertThat(received.body).isEqualTo("Você recebeu R$ 123,45 de outro cliente.")
        assertThat(sent.body + received.body).doesNotContain(source.toString()).doesNotContain(destination.toString())
    }

    @Test
    fun `the other party is the receiver for the sender and the sender for the receiver`() {
        assertThat(transfer(NotificationKind.TRANSFER_SENT).counterpartAccountId()).isEqualTo(destination)
        assertThat(transfer(NotificationKind.TRANSFER_FAILED).counterpartAccountId()).isEqualTo(destination)
        assertThat(transfer(NotificationKind.TRANSFER_COMPENSATED).counterpartAccountId()).isEqualTo(destination)
        assertThat(transfer(NotificationKind.TRANSFER_RECEIVED).counterpartAccountId()).isEqualTo(source)
    }

    @Test
    fun `a notification about the customer's own account has no other party`() {
        val welcome = notification(NotificationKind.WELCOME, mapOf("accountId" to source.toString()))

        assertThat(welcome.counterpartAccountId()).isNull()
    }

    @Test
    fun `a stored welcome with the old text is rendered with the new one, from the account type`() {
        val stored = notification(NotificationKind.WELCOME, mapOf("accountId" to source.toString(), "accountType" to "GOLD"))

        assertThat(NotificationText.render(stored, null)).isEqualTo(NotificationText.welcome("GOLD"))
    }

    @Test
    fun `a stored welcome without the account type is rendered without breaking and without an id`() {
        val stored = notification(NotificationKind.WELCOME, mapOf("accountId" to source.toString()))

        val content = NotificationText.render(stored, null)

        assertThat(content.title).isEqualTo("Bem-vindo ao capzzPay!")
        assertThat(content.body).doesNotContain(source.toString()).startsWith("Sua conta está aberta.")
    }

    @Test
    fun `stored account status notifications are rendered with the new text`() {
        val data = mapOf<String, Any>("accountId" to source.toString())

        assertThat(NotificationText.render(notification(NotificationKind.ACCOUNT_BLOCKED, data), null))
            .isEqualTo(NotificationText.accountBlocked())
        assertThat(NotificationText.render(notification(NotificationKind.ACCOUNT_UNBLOCKED, data), null))
            .isEqualTo(NotificationText.accountUnblocked())
        assertThat(NotificationText.render(notification(NotificationKind.ACCOUNT_CLOSED, data), null))
            .isEqualTo(NotificationText.accountClosed())
    }

    @Test
    fun `an account type change is rendered from its data`() {
        val stored = notification(NotificationKind.ACCOUNT_TYPE_CHANGED, mapOf("accountType" to "BLACK", "overdraftLimitCents" to 500_000L))

        assertThat(NotificationText.render(stored, null)).isEqualTo(NotificationText.accountTypeChanged("BLACK", 500_000))
    }

    @Test
    fun `no kind renders an id in its title or body, even when the stored text has one`() {
        val uuid = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

        NotificationKind.entries.forEach { kind ->
            val stored =
                notification(
                    kind,
                    mapOf(
                        "accountId" to source.toString(),
                        "sourceAccountId" to source.toString(),
                        "destinationAccountId" to destination.toString(),
                        "amountCents" to 12345L,
                        "accountType" to "GOLD",
                    ),
                ).copy(title = "old $source", body = "old $source")

            val content = NotificationText.render(stored, null)

            assertThat(content.title).describedAs("$kind title").doesNotContainPattern(uuid.pattern)
            assertThat(content.body).describedAs("$kind body").doesNotContainPattern(uuid.pattern)
        }
    }

    private fun transfer(
        kind: NotificationKind,
        vararg extra: Pair<String, Any>,
    ) = notification(
        kind,
        mapOf(
            "transferId" to "0190f7a0-0000-7000-8000-00000000f001",
            "sourceAccountId" to source.toString(),
            "destinationAccountId" to destination.toString(),
            "amountCents" to 12345L,
        ) + extra,
    )

    private fun notification(
        kind: NotificationKind,
        data: Map<String, Any>,
    ) = Notification(
        id = UUID.fromString("0190f7a0-0000-7000-8000-000000000010"),
        customerId = UUID.fromString("0190f7a0-0000-7000-8000-000000000011"),
        kind = kind,
        title = "stored title",
        body = "stored body",
        data = data,
        sourceEventId = UUID.fromString("0190f7a0-0000-7000-8000-000000000012"),
        createdAt = Instant.parse("2026-11-20T10:00:00Z"),
        readAt = null,
    )
}
