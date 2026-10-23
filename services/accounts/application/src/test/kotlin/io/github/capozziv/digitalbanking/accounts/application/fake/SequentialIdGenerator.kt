package io.github.capozziv.digitalbanking.accounts.application.fake

import io.github.capozziv.digitalbanking.accounts.application.port.IdGenerator
import java.util.UUID

class SequentialIdGenerator : IdGenerator {
    private var next = 1L

    override fun newId(): UUID = idAt(next++)

    companion object {
        fun idAt(position: Long): UUID = UUID.fromString("0199a0b0-0000-7000-8000-%012d".format(position))
    }
}
