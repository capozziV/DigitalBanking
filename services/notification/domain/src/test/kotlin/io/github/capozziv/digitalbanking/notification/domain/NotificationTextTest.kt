package io.github.capozziv.digitalbanking.notification.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class NotificationTextTest {
    private val account = UUID.fromString("0190f7a0-0000-7000-8000-000000000001")
    private val other = UUID.fromString("0190f7a0-0000-7000-8000-000000000002")

    @Test
    fun `welcome names the account type and number`() {
        val content = NotificationText.welcome("GOLD", account)
        assertThat(content.title).isEqualTo("Bem-vindo ao banco!")
        assertThat(content.body).isEqualTo(
            "Sua conta Gold está aberta. Você pode receber transferências pelo número $account e acompanhar tudo por aqui no sininho.",
        )
    }

    @Test
    fun `account type changed with overdraft limit`() {
        val content = NotificationText.accountTypeChanged("BLACK", 500000)
        assertThat(content.title).isEqualTo("Sua conta mudou de tipo")
        assertThat(content.body).isEqualTo(
            "Sua conta agora é Black. Seu limite de cheque especial passou a ser R$ 5.000,00.",
        )
    }

    @Test
    fun `account type changed without overdraft limit (v1 event)`() {
        val content = NotificationText.accountTypeChanged("GOLD", null)
        assertThat(content.title).isEqualTo("Sua conta mudou de tipo")
        assertThat(content.body).isEqualTo("Sua conta agora é Gold.")
    }

    @Test
    fun `transfer sent`() {
        val content = NotificationText.transferSent(12345, other)
        assertThat(content.title).isEqualTo("Transferência enviada")
        assertThat(content.body).isEqualTo("Você enviou R$ 123,45 para a conta $other.")
    }

    @Test
    fun `transfer received`() {
        val content = NotificationText.transferReceived(12345, other)
        assertThat(content.title).isEqualTo("Você recebeu uma transferência")
        assertThat(content.body).isEqualTo("Você recebeu R$ 123,45 da conta $other.")
    }

    @Test
    fun `transfer failed maps known reasons`() {
        val content = NotificationText.transferFailed(1000, other, "insufficient-available-balance")
        assertThat(content.title).isEqualTo("Transferência recusada")
        assertThat(content.body).isEqualTo(
            "Sua transferência de R$ 10,00 para a conta $other foi recusada: saldo disponível insuficiente. Nenhum valor saiu da sua conta.",
        )
        assertThat(NotificationText.transferFailed(1000, other, "debit-not-confirmed").body).isEqualTo(
            "Sua transferência de R$ 10,00 para a conta $other foi recusada: não foi possível confirmar o débito. Nenhum valor saiu da sua conta.",
        )
    }

    @Test
    fun `transfer failed falls back for unknown reasons`() {
        assertThat(NotificationText.transferFailed(1000, other, "anything-else").body).isEqualTo(
            "Sua transferência de R$ 10,00 para a conta $other foi recusada: não foi possível processar a transferência. Nenhum valor saiu da sua conta.",
        )
    }

    @Test
    fun `transfer compensated`() {
        val content = NotificationText.transferCompensated(1000, other)
        assertThat(content.title).isEqualTo("Transferência devolvida")
        assertThat(content.body).isEqualTo(
            "Sua transferência de R$ 10,00 para a conta $other não pôde ser concluída e o valor voltou para a sua conta.",
        )
    }
}
