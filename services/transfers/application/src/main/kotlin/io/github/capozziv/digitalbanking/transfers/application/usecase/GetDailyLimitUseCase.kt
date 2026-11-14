package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.security.Caller
import io.github.capozziv.digitalbanking.transfers.application.port.AccountLookup
import io.github.capozziv.digitalbanking.transfers.application.port.AccountsGateway
import io.github.capozziv.digitalbanking.transfers.application.port.DailyLimitGateway
import io.github.capozziv.digitalbanking.transfers.application.port.DailyLimitUsage
import io.github.capozziv.digitalbanking.transfers.domain.DailyLimitDay
import java.time.Clock
import java.util.UUID

sealed interface DailyLimitOutcome {
    data class Found(
        val usage: DailyLimitUsage,
    ) : DailyLimitOutcome

    /** The account does not exist or is not the caller's: the same answer, so its existence does not leak. */
    data object AccountNotFound : DailyLimitOutcome

    data object AccountsUnavailable : DailyLimitOutcome
}

/**
 * The daily limit of an account today (São Paulo day); an account that never transferred has nothing used.
 * This service does not know accounts, so the owner is checked with `accounts`; an admin sees any account, without
 * that lookup.
 */
class GetDailyLimitUseCase(
    private val dailyLimits: DailyLimitGateway,
    private val accounts: AccountsGateway,
    private val clock: Clock,
) {
    fun execute(
        accountId: UUID,
        caller: Caller,
    ): DailyLimitOutcome {
        if (caller !is Caller.Admin) {
            when (val account = accounts.lookupAccount(accountId)) {
                is AccountLookup.Found -> if (!caller.mayAccess(account.customerId)) return DailyLimitOutcome.AccountNotFound
                AccountLookup.NotFound -> return DailyLimitOutcome.AccountNotFound
                AccountLookup.Unavailable -> return DailyLimitOutcome.AccountsUnavailable
            }
        }
        return DailyLimitOutcome.Found(dailyLimits.usage(accountId, DailyLimitDay.of(clock.instant())))
    }
}
