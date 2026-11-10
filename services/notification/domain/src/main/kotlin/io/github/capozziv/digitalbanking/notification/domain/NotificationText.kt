package io.github.capozziv.digitalbanking.notification.domain

import java.util.UUID

data class NotificationContent(
    val title: String,
    val body: String,
)

object NotificationText {
    fun welcome(
        accountType: String,
        accountId: UUID,
    ) = NotificationContent(
        title = "Bem-vindo ao banco!",
        body =
            "Sua conta ${displayType(accountType)} está aberta. " +
                "Você pode receber transferências pelo número $accountId e acompanhar tudo por aqui no sininho.",
    )

    fun accountTypeChanged(
        newType: String,
        overdraftLimitCents: Long?,
    ): NotificationContent {
        val base = "Sua conta agora é ${displayType(newType)}."
        val body =
            if (overdraftLimitCents == null) {
                base
            } else {
                "$base Seu limite de cheque especial passou a ser ${MoneyFormat.brl(overdraftLimitCents)}."
            }
        return NotificationContent(title = "Sua conta mudou de tipo", body = body)
    }

    fun transferSent(
        amountCents: Long,
        destinationAccountId: UUID,
    ) = NotificationContent(
        title = "Transferência enviada",
        body = "Você enviou ${MoneyFormat.brl(amountCents)} para a conta $destinationAccountId.",
    )

    fun transferReceived(
        amountCents: Long,
        sourceAccountId: UUID,
    ) = NotificationContent(
        title = "Você recebeu uma transferência",
        body = "Você recebeu ${MoneyFormat.brl(amountCents)} da conta $sourceAccountId.",
    )

    fun transferFailed(
        amountCents: Long,
        destinationAccountId: UUID,
        reason: String,
    ) = NotificationContent(
        title = "Transferência recusada",
        body =
            "Sua transferência de ${MoneyFormat.brl(amountCents)} para a conta $destinationAccountId " +
                "foi recusada: ${reasonText(reason)}. Nenhum valor saiu da sua conta.",
    )

    fun transferCompensated(
        amountCents: Long,
        destinationAccountId: UUID,
    ) = NotificationContent(
        title = "Transferência devolvida",
        body =
            "Sua transferência de ${MoneyFormat.brl(amountCents)} para a conta $destinationAccountId " +
                "não pôde ser concluída e o valor voltou para a sua conta.",
    )

    private fun displayType(type: String) = type.lowercase().replaceFirstChar { it.uppercase() }

    private fun reasonText(reason: String) =
        when (reason) {
            "insufficient-available-balance" -> "saldo disponível insuficiente"
            "debit-not-confirmed" -> "não foi possível confirmar o débito"
            else -> "não foi possível processar a transferência"
        }
}
