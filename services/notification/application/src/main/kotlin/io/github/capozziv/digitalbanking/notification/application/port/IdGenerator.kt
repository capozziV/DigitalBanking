package io.github.capozziv.digitalbanking.notification.application.port

import java.util.UUID

interface IdGenerator {
    fun newId(): UUID
}
