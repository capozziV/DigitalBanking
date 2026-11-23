package io.github.capozziv.digitalbanking.notification.domain

data class NotificationContent(
    val title: String,
    val body: String,
)

object NotificationText {
    /** [accountType] is null when a stored notification did not keep it. */
    fun welcome(accountType: String?) =
        NotificationContent(
            title = "Bem-vindo ao capzzPay!",
            body =
                "Sua conta ${accountType?.let { "${displayType(it)} " } ?: ""}está aberta. " +
                    "Você pode receber transferências pelo seu celular cadastrado e acompanhar tudo em Notificações.",
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

    /**
     * The text shown to the customer. A transfer is told with the name of the other party, looked up when the
     * notification is read ([counterpartName] null while the name is not known: "outro cliente"); the account
     * kinds are built here too, so rows stored with an older text (with an account number) are corrected; only a
     * type change stored without its type keeps the stored text.
     */
    fun render(
        notification: Notification,
        counterpartName: String?,
    ): NotificationContent {
        val data = notification.data
        return when (notification.kind) {
            NotificationKind.TRANSFER_SENT -> {
                transferSent(data.cents("amountCents"), counterpartName)
            }

            NotificationKind.TRANSFER_RECEIVED -> {
                transferReceived(data.cents("amountCents"), counterpartName)
            }

            NotificationKind.TRANSFER_FAILED -> {
                transferFailed(data.cents("amountCents"), counterpartName, data["reason"] as? String ?: "")
            }

            NotificationKind.TRANSFER_COMPENSATED -> {
                transferCompensated(data.cents("amountCents"), counterpartName, data["reason"] as? String)
            }

            NotificationKind.WELCOME -> {
                welcome(data["accountType"] as? String)
            }

            NotificationKind.ACCOUNT_BLOCKED -> {
                accountBlocked()
            }

            NotificationKind.ACCOUNT_UNBLOCKED -> {
                accountUnblocked()
            }

            NotificationKind.ACCOUNT_CLOSED -> {
                accountClosed()
            }

            NotificationKind.ACCOUNT_TYPE_CHANGED -> {
                val newType = data["accountType"] as? String
                if (newType == null) {
                    NotificationContent(notification.title, notification.body)
                } else {
                    accountTypeChanged(newType, (data["overdraftLimitCents"] as? Number)?.toLong())
                }
            }
        }
    }

    fun transferSent(
        amountCents: Long,
        receiverName: String?,
    ) = NotificationContent(
        title = "Transferência enviada",
        body = "Você enviou ${MoneyFormat.brl(amountCents)} para ${party(receiverName)}.",
    )

    fun transferReceived(
        amountCents: Long,
        senderName: String?,
    ) = NotificationContent(
        title = "Você recebeu uma transferência",
        body = "Você recebeu ${MoneyFormat.brl(amountCents)} de ${party(senderName)}.",
    )

    fun transferFailed(
        amountCents: Long,
        receiverName: String?,
        reason: String,
    ) = NotificationContent(
        title = "Transferência recusada",
        body =
            "Sua transferência de ${MoneyFormat.brl(amountCents)} para ${party(receiverName)} " +
                "foi recusada: ${reasonText(reason)}. Nenhum valor saiu da sua conta.",
    )

    /** [reason] is null for events that did not carry it (version 1) or when the saga compensated without a refusal. */
    fun transferCompensated(
        amountCents: Long,
        receiverName: String?,
        reason: String? = null,
    ) = NotificationContent(
        title = "Transferência devolvida",
        body =
            "Sua transferência de ${MoneyFormat.brl(amountCents)} para ${party(receiverName)} " +
                if (reason == null) {
                    "não pôde ser concluída e o valor voltou para a sua conta."
                } else {
                    "não pôde ser concluída: ${reasonText(reason)}. O valor voltou para a sua conta."
                },
    )

    fun accountBlocked() =
        NotificationContent(
            title = "Conta bloqueada",
            body =
                "Sua conta foi bloqueada. " +
                    "Ela continua recebendo transferências, mas não pode enviar valores enquanto estiver bloqueada.",
        )

    fun accountUnblocked() =
        NotificationContent(
            title = "Conta desbloqueada",
            body = "Sua conta foi desbloqueada e já pode enviar transferências de novo.",
        )

    fun accountClosed() =
        NotificationContent(
            title = "Conta encerrada",
            body =
                "Sua conta foi encerrada. " +
                    "O extrato continua disponível para consulta, e você pode abrir uma conta nova quando quiser.",
        )

    /** Only the customer's full name is shown, never an account number. */
    private fun party(name: String?) = name ?: UNKNOWN_PARTY

    private fun Map<String, Any>.cents(key: String): Long = (get(key) as Number).toLong()

    private const val UNKNOWN_PARTY = "outro cliente"

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
