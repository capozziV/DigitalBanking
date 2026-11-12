package io.github.capozziv.digitalbanking.statement.adapters.web.dto

import io.github.capozziv.digitalbanking.statement.adapters.web.StatementCursorCodec
import io.github.capozziv.digitalbanking.statement.domain.StatementPage
import java.util.UUID

/** A page of the statement, newest entry first. `nextCursor` is opaque and null on the last page. */
data class StatementDTO(
    val accountId: UUID,
    val entries: List<StatementEntryDTO>,
    val nextCursor: String?,
) {
    companion object {
        fun from(
            accountId: UUID,
            page: StatementPage,
        ) = StatementDTO(
            accountId = accountId,
            entries = page.entries.map(StatementEntryDTO::from),
            nextCursor = page.nextCursor?.let(StatementCursorCodec::encode),
        )
    }
}
