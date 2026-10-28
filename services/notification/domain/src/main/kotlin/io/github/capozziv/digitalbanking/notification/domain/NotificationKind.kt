package io.github.capozziv.digitalbanking.notification.domain

enum class NotificationKind {
    WELCOME,
    ACCOUNT_TYPE_CHANGED,
    TRANSFER_SENT,
    TRANSFER_RECEIVED,
    TRANSFER_FAILED,
    TRANSFER_COMPENSATED,
}
