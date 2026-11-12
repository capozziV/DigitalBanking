package io.github.capozziv.digitalbanking.transfers.application.usecase

import io.github.capozziv.digitalbanking.transfers.application.port.DailyLimitGateway
import io.github.capozziv.digitalbanking.transfers.application.port.DailyLimitUsage
import io.github.capozziv.digitalbanking.transfers.domain.DailyLimitDay
import java.time.Clock
import java.util.UUID

/** The daily limit of an account today (São Paulo day); an account that never transferred has nothing used. */
class GetDailyLimitUseCase(
    private val dailyLimits: DailyLimitGateway,
    private val clock: Clock,
) {
    fun execute(accountId: UUID): DailyLimitUsage = dailyLimits.usage(accountId, DailyLimitDay.of(clock.instant()))
}
