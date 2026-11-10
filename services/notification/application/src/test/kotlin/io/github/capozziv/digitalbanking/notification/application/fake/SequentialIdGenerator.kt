package io.github.capozziv.digitalbanking.notification.application.fake

import io.github.capozziv.digitalbanking.notification.application.port.IdGenerator
import java.util.UUID

/** Hands out increasing ids, so their string order follows creation order like UUID v7. */
class SequentialIdGenerator : IdGenerator {
    private var next = 1L

    override fun newId(): UUID = UUID(0x0190_0000_0000_7000L, next++)
}
