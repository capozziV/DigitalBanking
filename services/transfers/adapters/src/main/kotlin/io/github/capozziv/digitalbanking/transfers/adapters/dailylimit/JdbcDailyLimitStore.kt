package io.github.capozziv.digitalbanking.transfers.adapters.dailylimit

import org.springframework.jdbc.core.simple.JdbcClient
import java.time.LocalDate
import java.util.UUID

/**
 * The daily limit in PostgreSQL, the source of truth: the reservations of an account on a day are its transfers with
 * that `limit_day`, `limit_reserved` and status PENDING or COMPLETED. Every method runs in the caller's transaction.
 */
class JdbcDailyLimitStore(
    private val jdbc: JdbcClient,
) {
    /** Waits for, then holds until the transaction ends, the lock of the account and day shared by every reservation. */
    fun lock(
        accountId: UUID,
        day: LocalDate,
    ) {
        jdbc
            .sql("select pg_advisory_xact_lock(hashtextextended(cast(:accountId as text) || ':' || cast(:day as text), 0))")
            .param("accountId", accountId.toString())
            .param("day", day.toString())
            .query { _, _ -> Unit }
            .single()
    }

    /** Bumped by every reservation made while Redis was down; 0 when there was none. */
    fun epoch(
        accountId: UUID,
        day: LocalDate,
    ): Long =
        jdbc
            .sql("select epoch from daily_limit_epoch where account_id = :accountId and limit_day = :day")
            .param("accountId", accountId)
            .param("day", day)
            .query(Long::class.java)
            .optional()
            .orElse(0L)

    fun bumpEpoch(
        accountId: UUID,
        day: LocalDate,
    ) {
        jdbc
            .sql(
                """
                insert into daily_limit_epoch (account_id, limit_day, epoch) values (:accountId, :day, 1)
                on conflict (account_id, limit_day) do update set epoch = daily_limit_epoch.epoch + 1
                """.trimIndent(),
            ).param("accountId", accountId)
            .param("day", day)
            .update()
    }

    /** The amount, in cents, of each transfer that holds a reservation of the account on the day. */
    fun reservations(
        accountId: UUID,
        day: LocalDate,
    ): Map<UUID, Long> =
        jdbc
            .sql(
                """
                select id, amount from transfer
                where source_account_id = :accountId and limit_day = :day and limit_reserved
                  and status in ('PENDING', 'COMPLETED')
                """.trimIndent(),
            ).param("accountId", accountId)
            .param("day", day)
            .query { rs, _ -> rs.getObject("id", UUID::class.java) to rs.getLong("amount") }
            .list()
            .toMap()

    fun isReserved(transferId: UUID): Boolean =
        jdbc
            .sql("select limit_reserved from transfer where id = :id")
            .param("id", transferId)
            .query(Boolean::class.java)
            .optional()
            .orElse(false)

    /** Marks the transfer as holding its reservation; the transfer must exist. */
    fun markReserved(transferId: UUID) {
        val updated =
            jdbc
                .sql("update transfer set limit_reserved = true where id = :id")
                .param("id", transferId)
                .update()
        check(updated == 1) { "transfer $transferId does not exist" }
    }
}
