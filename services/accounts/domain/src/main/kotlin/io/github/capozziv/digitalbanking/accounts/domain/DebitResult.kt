package io.github.capozziv.digitalbanking.accounts.domain

data class EntryRecorded(
    val entry: LedgerEntry,
    val event: LedgerEntryRecorded,
)

sealed interface DebitResult {
    data class Recorded(
        val recorded: EntryRecorded,
    ) : DebitResult

    data class Refused(
        val availableBalance: AvailableBalance,
    ) : DebitResult
}
