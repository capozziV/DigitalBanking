package io.github.capozziv.digitalbanking.statement.domain

/** Whether a statement entry raised (credit) or lowered (debit) the balance; owned by this service. */
enum class EntryKind { CREDIT, DEBIT }
