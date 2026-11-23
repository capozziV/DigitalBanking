package io.github.capozziv.digitalbanking.notification.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NotificationTextTest {
    @Test
    fun `welcome names the account type and never the account number`() {
        val content = NotificationText.welcome("GOLD")
        assertThat(content.title).isEqualTo("Bem-vindo ao capzzPay!")
        assertThat(content.body).isEqualTo(
            "Sua conta Gold está aberta. Você pode receber transferências pelo seu celular cadastrado e acompanhar tudo em Notificações.",
        )
    }

    @Test
    fun `account status texts never carry an account number`() {
        assertThat(NotificationText.accountBlocked()).isEqualTo(
            NotificationContent(
                "Conta bloqueada",
                "Sua conta foi bloqueada. Ela continua recebendo transferências, mas não pode enviar valores enquanto estiver bloqueada.",
            ),
        )
        assertThat(NotificationText.accountUnblocked()).isEqualTo(
            NotificationContent("Conta desbloqueada", "Sua conta foi desbloqueada e já pode enviar transferências de novo."),
        )
        assertThat(NotificationText.accountClosed()).isEqualTo(
            NotificationContent(
                "Conta encerrada",
                "Sua conta foi encerrada. O extrato continua disponível para consulta, e você pode abrir uma conta nova quando quiser.",
            ),
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
        val content = NotificationText.transferSent(12345, "Ana Souza")
        assertThat(content.title).isEqualTo("Transferência enviada")
        assertThat(content.body).isEqualTo("Você enviou R$ 123,45 para Ana Souza.")
    }

    @Test
    fun `transfer received`() {
        val content = NotificationText.transferReceived(12345, "Ana Souza")
        assertThat(content.title).isEqualTo("Você recebeu uma transferência")
        assertThat(content.body).isEqualTo("Você recebeu R$ 123,45 de Ana Souza.")
    }

    @Test
    fun `transfer failed maps known reasons`() {
        val content = NotificationText.transferFailed(1000, "Ana Souza", "insufficient-available-balance")
        assertThat(content.title).isEqualTo("Transferência recusada")
        assertThat(content.body).isEqualTo(
            "Sua transferência de R$ 10,00 para Ana Souza foi recusada: saldo disponível insuficiente. Nenhum valor saiu da sua conta.",
        )
        assertThat(NotificationText.transferFailed(1000, "Ana Souza", "debit-not-confirmed").body).isEqualTo(
            "Sua transferência de R$ 10,00 para Ana Souza foi recusada: não foi possível confirmar o débito. Nenhum valor saiu da sua conta.",
        )
    }

    @Test
    fun `transfer failed explains the daily limit reasons`() {
        assertThat(NotificationText.transferFailed(1000, "Ana Souza", "daily-limit-exceeded").body).isEqualTo(
            "Sua transferência de R$ 10,00 para Ana Souza foi recusada: o limite diário de transferências da conta seria ultrapassado. Nenhum valor saiu da sua conta.",
        )
        assertThat(NotificationText.transferFailed(1000, "Ana Souza", "daily-limit-unavailable").body).isEqualTo(
            "Sua transferência de R$ 10,00 para Ana Souza foi recusada: não foi possível conferir o limite diário agora, tente novamente. Nenhum valor saiu da sua conta.",
        )
    }

    @Test
    fun `transfer failed explains a blocked or closed account`() {
        assertThat(NotificationText.transferFailed(1000, "Ana Souza", "account-blocked").body).isEqualTo(
            "Sua transferência de R$ 10,00 para Ana Souza foi recusada: uma das contas está bloqueada. Nenhum valor saiu da sua conta.",
        )
        assertThat(NotificationText.transferFailed(1000, "Ana Souza", "account-closed").body).isEqualTo(
            "Sua transferência de R$ 10,00 para Ana Souza foi recusada: uma das contas está encerrada. Nenhum valor saiu da sua conta.",
        )
    }

    @Test
    fun `transfer failed falls back for unknown reasons`() {
        assertThat(NotificationText.transferFailed(1000, "Ana Souza", "anything-else").body).isEqualTo(
            "Sua transferência de R$ 10,00 para Ana Souza foi recusada: não foi possível processar a transferência. Nenhum valor saiu da sua conta.",
        )
    }

    @Test
    fun `transfer compensated`() {
        val content = NotificationText.transferCompensated(1000, "Ana Souza")
        assertThat(content.title).isEqualTo("Transferência devolvida")
        assertThat(content.body).isEqualTo(
            "Sua transferência de R$ 10,00 para Ana Souza não pôde ser concluída e o valor voltou para a sua conta.",
        )
    }

    @Test
    fun `transfer compensated says why the credit was refused`() {
        assertThat(NotificationText.transferCompensated(1000, "Ana Souza", "account-closed").body).isEqualTo(
            "Sua transferência de R$ 10,00 para Ana Souza não pôde ser concluída: " +
                "uma das contas está encerrada. O valor voltou para a sua conta.",
        )
        assertThat(NotificationText.transferCompensated(1000, "Ana Souza", "credit-not-confirmed").body).isEqualTo(
            "Sua transferência de R$ 10,00 para Ana Souza não pôde ser concluída: " +
                "não foi possível confirmar o crédito na conta de destino. O valor voltou para a sua conta.",
        )
    }
}
