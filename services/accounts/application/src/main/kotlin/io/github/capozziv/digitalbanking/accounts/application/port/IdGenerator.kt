package io.github.capozziv.digitalbanking.accounts.application.port

import java.util.UUID

interface IdGenerator {
    fun newId(): UUID
}
