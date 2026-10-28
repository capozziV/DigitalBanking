package io.github.capozziv.digitalbanking.transfers.application.port

import java.util.UUID

interface IdGenerator {
    fun newId(): UUID
}
