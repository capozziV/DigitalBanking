package io.github.capozziv.digitalbanking.statement.domain

/** One page of the statement, newest entry first; [nextCursor] is null on the last page. */
data class StatementPage(
    val entries: List<StatementEntry>,
    val nextCursor: StatementCursor?,
)
