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

    /** [reason] is null for events that did not carry it (version 1) or when the saga compensated without a refusal. */
    fun transferCompensated(
        amountCents: Long,
        destinationAccountId: UUID,
        reason: String? = null,
    ) = NotificationContent(
        title = "Transferência devolvida",
        body =
            "Sua transferência de ${MoneyFormat.brl(amountCents)} para a conta $destinationAccountId " +
                if (reason == null) {
                    "não pôde ser concluída e o valor voltou para a sua conta."
                } else {
                    "não pôde ser concluída: ${reasonText(reason)}. O valor voltou para a sua conta."
                },
    )

    fun accountBlocked(accountId: UUID) =
        NotificationContent(
            title = "Conta bloqueada",
            body =
                "Sua conta $accountId foi bloqueada. " +
                    "Ela continua recebendo transferências, mas não pode enviar valores enquanto estiver bloqueada.",
        )

    fun accountUnblocked(accountId: UUID) =
        NotificationContent(
            title = "Conta desbloqueada",
            body = "Sua conta $accountId foi desbloqueada e já pode enviar transferências de novo.",
        )

    fun accountClosed(accountId: UUID) =
        NotificationContent(
            title = "Conta encerrada",
            body =
                "Sua conta $accountId foi encerrada. " +
                    "O extrato continua disponível para consulta, e você pode abrir uma conta nova quando quiser.",
        )

    private fun displayType(type: String) = type.lowercase().replaceFirstChar { it.uppercase() }

    private fun reasonText(reason: String) =
        when (reason) {
            "insufficient-available-balance" -> "saldo disponível insuficiente"
            "debit-not-confirmed" -> "não foi possível confirmar o débito"
            "daily-limit-exceeded" -> "o limite diário de transferências da conta seria ultrapassado"
            "account-blocked" -> "uma das contas está bloqueada"
            "account-closed" -> "uma das contas está encerrada"
            "credit-not-confirmed" -> "não foi possível confirmar o crédito na conta de destino"
            "daily-limit-unavailable" -> "não foi possível conferir o limite diário agora, tente novamente"
            else -> "não foi possível processar a transferência"
        }
}
