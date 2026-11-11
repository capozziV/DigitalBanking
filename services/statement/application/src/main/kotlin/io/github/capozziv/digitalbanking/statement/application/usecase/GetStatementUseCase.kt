package io.github.capozziv.digitalbanking.statement.application.usecase

import io.github.capozziv.digitalbanking.statement.application.AccountNotFoundException
import io.github.capozziv.digitalbanking.statement.application.port.StatementRepository
import io.github.capozziv.digitalbanking.statement.domain.StatementCursor
import io.github.capozziv.digitalbanking.statement.domain.StatementPage
import java.util.UUID

class GetStatementUseCase(
    private val repository: StatementRepository,
) {
    fun execute(
        accountId: UUID,
        cursor: StatementCursor?,
        limit: Int,
    ): StatementPage {
        require(limit in MIN_LIMIT..MAX_LIMIT) { "limit must be between $MIN_LIMIT and $MAX_LIMIT, was $limit" }
        val page = repository.findPage(accountId, cursor, limit)
        // Only an empty first page needs the account item: entries prove the account exists even before
        // its AccountOpened arrives, and a cursor means an earlier page was already served.
        if (cursor == null && page.entries.isEmpty() && !repository.accountExists(accountId)) {
            throw AccountNotFoundException(accountId)
        }
        return page
    }

    companion object {
        const val MIN_LIMIT = 1
        const val MAX_LIMIT = 100
        const val DEFAULT_LIMIT = 20
    }
}
