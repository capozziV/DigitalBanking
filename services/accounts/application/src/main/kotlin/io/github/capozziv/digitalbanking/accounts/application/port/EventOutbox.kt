package io.github.capozziv.digitalbanking.accounts.application.port

import io.github.capozziv.digitalbanking.accounts.domain.AccountEvent

interface EventOutbox {
    fun append(event: AccountEvent)
}
