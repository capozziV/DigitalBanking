package io.github.capozziv.digitalbanking.statement.domain

/** Position for the next statement page: entries with a sequence lower than [beforeSequence]. */
data class StatementCursor(
    val beforeSequence: Long,
) {
    init {
        require(beforeSequence >= 1) { "beforeSequence must be at least 1, was $beforeSequence" }
    }
}
