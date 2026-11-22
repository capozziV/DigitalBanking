package io.github.capozziv.digitalbanking.notification.application.usecase

import io.github.capozziv.digitalbanking.notification.application.ReceivedEvent
import io.github.capozziv.digitalbanking.notification.application.UnknownAccountException
import io.github.capozziv.digitalbanking.notification.application.fake.ImmediateTransactionRunner
import io.github.capozziv.digitalbanking.notification.application.fake.InMemoryCustomerNameRepository
import io.github.capozziv.digitalbanking.notification.application.fake.InMemoryKnownAccountRepository
import io.github.capozziv.digitalbanking.notification.application.fake.InMemoryNotificationRepository
import io.github.capozziv.digitalbanking.notification.application.fake.InMemoryProcessedEventRepository
import io.github.capozziv.digitalbanking.notification.application.fake.MutableClock
import io.github.capozziv.digitalbanking.notification.application.fake.SequentialIdGenerator
import io.github.capozziv.digitalbanking.notification.domain.NotificationContent
import io.github.capozziv.digitalbanking.notification.domain.NotificationKind
import io.github.capozziv.digitalbanking.notification.domain.NotificationText
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import java.util.UUID

class ConsumeEventUseCaseTest {
    private val now = Instant.parse("2026-10-28T12:00:00Z")
    private val transactions = ImmediateTransactionRunner()
    private val processed = InMemoryProcessedEventRepository(transactions)
    private val accounts = InMemoryKnownAccountRepository(transactions)
    private val notifications = InMemoryNotificationRepository(transactions)
    private val names = InMemoryCustomerNameRepository(transactions, accounts)
    private val useCase =
        ConsumeEventUseCase(processed, accounts, names, notifications, transactions, SequentialIdGenerator(), MutableClock(now))

    private val alice = UUID.randomUUID()
    private val bob = UUID.randomUUID()
    private val aliceAccount = UUID.randomUUID()
    private val aliceSecondAccount = UUID.randomUUID()
    private val bobAccount = UUID.randomUUID()

    @Test
    fun `account opened stores the owner and welcomes the customer`() {
        val event = ReceivedEvent.AccountOpened(UUID.randomUUID(), aliceAccount, alice, "GOLD")

        val outcome = useCase.execute(event)

        assertThat(outcome).isEqualTo(ConsumeOutcome.PROCESSED)
        assertThat(accounts.customerOf(aliceAccount)).isEqualTo(alice)
        val welcome = notifications.all.single()
        val expected = NotificationText.welcome("GOLD")
        assertThat(welcome.customerId).isEqualTo(alice)
        assertThat(welcome.kind).isEqualTo(NotificationKind.WELCOME)
        assertThat(welcome.title).isEqualTo(expected.title)
        assertThat(welcome.body).isEqualTo(expected.body)
        assertThat(welcome.sourceEventId).isEqualTo(event.eventId)
        assertThat(welcome.createdAt).isEqualTo(now)
        assertThat(welcome.readAt).isNull()
        assertThat(welcome.data).containsEntry("accountId", aliceAccount.toString()).containsEntry("accountType", "GOLD")
        assertThat(processed.processed).containsEntry(event.eventId, "AccountOpened")
        assertThat(transactions.transactions).isEqualTo(1)
    }

    @Test
    fun `customer registered keeps the name of the customer and notifies nobody`() {
        accounts.owners[aliceAccount] = alice
        val event = ReceivedEvent.CustomerRegistered(UUID.randomUUID(), alice, "Ana Souza")

        val outcome = useCase.execute(event)

        assertThat(outcome).isEqualTo(ConsumeOutcome.PROCESSED)
        assertThat(names.names).containsExactlyEntriesOf(mapOf(alice to "Ana Souza"))
        assertThat(names.namesOfAccountOwners(setOf(aliceAccount))).isEqualTo(mapOf(aliceAccount to "Ana Souza"))
        assertThat(notifications.all).isEmpty()
        assertThat(processed.processed).containsEntry(event.eventId, "CustomerRegistered")
    }

    @Test
    fun `customer registered delivered again is a duplicate`() {
        val event = ReceivedEvent.CustomerRegistered(UUID.randomUUID(), alice, "Ana Souza")
        useCase.execute(event)

        assertThat(useCase.execute(event.copy(name = "changed"))).isEqualTo(ConsumeOutcome.DUPLICATE)
        assertThat(names.names[alice]).isEqualTo("Ana Souza")
    }

    @Test
    fun `another registration of the same customer replaces the name`() {
        useCase.execute(ReceivedEvent.CustomerRegistered(UUID.randomUUID(), alice, "Ana Souza"))

        useCase.execute(ReceivedEvent.CustomerRegistered(UUID.randomUUID(), alice, "Ana Souza Lima"))

        assertThat(names.names).containsExactlyEntriesOf(mapOf(alice to "Ana Souza Lima"))
    }

    @Test
    fun `the same event delivered again is a duplicate and creates nothing`() {
        val event = ReceivedEvent.AccountOpened(UUID.randomUUID(), aliceAccount, alice, "GOLD")
        useCase.execute(event)

        val outcome = useCase.execute(event)

        assertThat(outcome).isEqualTo(ConsumeOutcome.DUPLICATE)
        assertThat(notifications.all).hasSize(1)
    }

    @Test
    fun `transfer completed notifies the sender and the receiver`() {
        openAccounts()
        val event = transferCompleted(aliceAccount, bobAccount, 12_345)

        val outcome = useCase.execute(event)

        assertThat(outcome).isEqualTo(ConsumeOutcome.PROCESSED)
        val sent = notifications.all.single { it.kind == NotificationKind.TRANSFER_SENT }
        val received = notifications.all.single { it.kind == NotificationKind.TRANSFER_RECEIVED }
        assertThat(sent.customerId).isEqualTo(alice)
        assertThat(sent.body).isEqualTo(NotificationText.transferSent(12_345, null).body)
        assertThat(sent.data)
            .containsEntry("transferId", event.transferId.toString())
            .containsEntry("amountCents", 12_345L)
        assertThat(received.customerId).isEqualTo(bob)
        assertThat(received.body).isEqualTo(NotificationText.transferReceived(12_345, null).body)
        assertThat(sent.sourceEventId).isEqualTo(event.eventId)
        assertThat(received.sourceEventId).isEqualTo(event.eventId)
    }

    @Test
    fun `a transfer between two accounts of the same customer sends both notifications to them`() {
        openAccounts()
        val event = transferCompleted(aliceAccount, aliceSecondAccount, 500)

        useCase.execute(event)

        val forEvent = notifications.all.filter { it.sourceEventId == event.eventId }
        assertThat(forEvent.map { it.customerId }).containsOnly(alice)
        assertThat(forEvent.map { it.kind })
            .containsExactlyInAnyOrder(NotificationKind.TRANSFER_SENT, NotificationKind.TRANSFER_RECEIVED)
    }

    @Test
    fun `transfer failed notifies only the sender with the reason`() {
        openAccounts()
        val event =
            ReceivedEvent.TransferFailed(
                UUID.randomUUID(),
                UUID.randomUUID(),
                aliceAccount,
                bobAccount,
                700,
                "insufficient-available-balance",
            )

        useCase.execute(event)

        val failed = notifications.all.single { it.sourceEventId == event.eventId }
        assertThat(failed.customerId).isEqualTo(alice)
        assertThat(failed.kind).isEqualTo(NotificationKind.TRANSFER_FAILED)
        assertThat(failed.body)
            .isEqualTo(NotificationText.transferFailed(700, null, "insufficient-available-balance").body)
        assertThat(failed.data).containsEntry("reason", "insufficient-available-balance")
    }

    @Test
    fun `transfer compensated notifies only the sender`() {
        openAccounts()
        val event = ReceivedEvent.TransferCompensated(UUID.randomUUID(), UUID.randomUUID(), aliceAccount, bobAccount, 900)

        useCase.execute(event)

        val compensated = notifications.all.single { it.sourceEventId == event.eventId }
        assertThat(compensated.customerId).isEqualTo(alice)
        assertThat(compensated.kind).isEqualTo(NotificationKind.TRANSFER_COMPENSATED)
        assertThat(compensated.body).isEqualTo(NotificationText.transferCompensated(900, null).body)
    }

    @Test
    fun `transfer compensated with a reason says it in the text and keeps it in the data`() {
        openAccounts()
        val event = ReceivedEvent.TransferCompensated(UUID.randomUUID(), UUID.randomUUID(), aliceAccount, bobAccount, 900, "account-closed")

        useCase.execute(event)

        val compensated = notifications.all.single { it.sourceEventId == event.eventId }
        assertThat(compensated.body).isEqualTo(NotificationText.transferCompensated(900, null, "account-closed").body)
        assertThat(compensated.body).contains("encerrada")
        assertThat(compensated.data).containsEntry("reason", "account-closed")
    }

    @Test
    fun `account type changed with the overdraft limit mentions it`() {
        openAccounts()
        val event = ReceivedEvent.AccountTypeChanged(UUID.randomUUID(), aliceAccount, "BLACK", 500_000)

        useCase.execute(event)

        val changed = notifications.all.single { it.sourceEventId == event.eventId }
        assertThat(changed.customerId).isEqualTo(alice)
        assertThat(changed.kind).isEqualTo(NotificationKind.ACCOUNT_TYPE_CHANGED)
        assertThat(changed.body).isEqualTo(NotificationText.accountTypeChanged("BLACK", 500_000).body)
        assertThat(changed.data).containsEntry("overdraftLimitCents", 500_000L)
    }

    @Test
    fun `account type changed version 1 without the limit uses the text without it`() {
        openAccounts()
        val event = ReceivedEvent.AccountTypeChanged(UUID.randomUUID(), aliceAccount, "GOLD", null)

        val outcome = useCase.execute(event)

        assertThat(outcome).isEqualTo(ConsumeOutcome.PROCESSED)
        val changed = notifications.all.single { it.sourceEventId == event.eventId }
        assertThat(changed.body).isEqualTo("Sua conta agora é Gold.")
        assertThat(changed.data).doesNotContainKey("overdraftLimitCents")
    }

    @Test
    fun `an unsupported event is ignored but marked as processed`() {
        val event = ReceivedEvent.Unsupported(UUID.randomUUID(), "SomethingNew")

        val outcome = useCase.execute(event)

        assertThat(outcome).isEqualTo(ConsumeOutcome.IGNORED)
        assertThat(processed.processed).containsEntry(event.eventId, "SomethingNew")
        assertThat(notifications.all).isEmpty()
    }

    @Test
    fun `an unsupported event delivered again is a duplicate`() {
        val event = ReceivedEvent.Unsupported(UUID.randomUUID(), "SomethingNew")
        useCase.execute(event)

        val outcome = useCase.execute(event)

        assertThat(outcome).isEqualTo(ConsumeOutcome.DUPLICATE)
        assertThat(notifications.all).isEmpty()
    }

    @Test
    fun `a transfer completed delivered again is a duplicate and creates no notification`() {
        openAccounts()
        val event = transferCompleted(aliceAccount, bobAccount, 500)
        useCase.execute(event)
        val before = notifications.all.size

        val outcome = useCase.execute(event)

        assertThat(outcome).isEqualTo(ConsumeOutcome.DUPLICATE)
        assertThat(notifications.all).hasSize(before)
    }

    @Test
    fun `an unknown destination account fails and records nothing`() {
        openAccounts()
        val unknown = UUID.randomUUID()
        val event = transferCompleted(aliceAccount, unknown, 100)
        val before = notifications.all.size

        assertThatThrownBy { useCase.execute(event) }
            .isInstanceOf(UnknownAccountException::class.java)
            .satisfies({ assertThat((it as UnknownAccountException).accountId).isEqualTo(unknown) })

        assertThat(processed.processed).doesNotContainKey(event.eventId)
        assertThat(notifications.all).hasSize(before)
    }

    @Test
    fun `an unknown account on a type change fails and records nothing`() {
        val event = ReceivedEvent.AccountTypeChanged(UUID.randomUUID(), UUID.randomUUID(), "GOLD", null)

        assertThatThrownBy { useCase.execute(event) }.isInstanceOf(UnknownAccountException::class.java)

        assertThat(processed.processed).isEmpty()
        assertThat(notifications.all).isEmpty()
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("accountStatusEvents")
    fun `an account status event notifies the customer of the payload`(
        eventType: String,
        newEvent: (UUID, UUID, UUID) -> ReceivedEvent,
        kind: NotificationKind,
        text: () -> NotificationContent,
    ) {
        // The customer comes from the payload: the account does not need to be known here.
        val event = newEvent(UUID.randomUUID(), aliceAccount, alice)

        val outcome = useCase.execute(event)

        assertThat(outcome).isEqualTo(ConsumeOutcome.PROCESSED)
        val notification = notifications.all.single()
        assertThat(notification.customerId).isEqualTo(alice)
        assertThat(notification.kind).isEqualTo(kind)
        assertThat(notification.title).isEqualTo(text().title)
        assertThat(notification.body).isEqualTo(text().body)
        assertThat(notification.sourceEventId).isEqualTo(event.eventId)
        assertThat(notification.data).containsEntry("accountId", aliceAccount.toString())
        assertThat(processed.processed).containsEntry(event.eventId, eventType)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("accountStatusEvents")
    fun `an account status event delivered twice notifies only once`(
        eventType: String,
        newEvent: (UUID, UUID, UUID) -> ReceivedEvent,
    ) {
        val event = newEvent(UUID.randomUUID(), aliceAccount, alice)
        useCase.execute(event)

        val outcome = useCase.execute(event)

        assertThat(outcome).isEqualTo(ConsumeOutcome.DUPLICATE)
        assertThat(notifications.all).hasSize(1)
        assertThat(processed.processed).containsEntry(event.eventId, eventType)
    }

    private fun openAccounts() {
        useCase.execute(ReceivedEvent.AccountOpened(UUID.randomUUID(), aliceAccount, alice, "STANDARD"))
        useCase.execute(ReceivedEvent.AccountOpened(UUID.randomUUID(), aliceSecondAccount, alice, "GOLD"))
        useCase.execute(ReceivedEvent.AccountOpened(UUID.randomUUID(), bobAccount, bob, "FREE"))
    }

    private fun transferCompleted(
        source: UUID,
        destination: UUID,
        amountCents: Long,
    ) = ReceivedEvent.TransferCompleted(UUID.randomUUID(), UUID.randomUUID(), source, destination, amountCents)

    companion object {
        @JvmStatic
        fun accountStatusEvents(): List<Arguments> =
            listOf(
                Arguments.of(
                    "AccountBlocked",
                    { e: UUID, a: UUID, c: UUID -> ReceivedEvent.AccountBlocked(e, a, c) },
                    NotificationKind.ACCOUNT_BLOCKED,
                    { NotificationText.accountBlocked() },
                ),
                Arguments.of(
                    "AccountUnblocked",
                    { e: UUID, a: UUID, c: UUID -> ReceivedEvent.AccountUnblocked(e, a, c) },
                    NotificationKind.ACCOUNT_UNBLOCKED,
                    { NotificationText.accountUnblocked() },
                ),
                Arguments.of(
                    "AccountClosed",
                    { e: UUID, a: UUID, c: UUID -> ReceivedEvent.AccountClosed(e, a, c) },
                    NotificationKind.ACCOUNT_CLOSED,
                    { NotificationText.accountClosed() },
                ),
            )
    }
}
