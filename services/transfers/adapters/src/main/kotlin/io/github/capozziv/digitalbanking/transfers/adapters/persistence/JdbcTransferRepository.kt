package io.github.capozziv.digitalbanking.transfers.adapters.persistence

import io.github.capozziv.digitalbanking.transfers.application.port.TransferRepository
import io.github.capozziv.digitalbanking.transfers.domain.Money
import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import io.github.capozziv.digitalbanking.transfers.domain.TransferStep
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Plain SQL over [JdbcClient]. The lease is `lock_owner` + `locked_until`: a single conditional UPDATE decides who
 * runs a saga, so several instances can share the table without further coordination.
 */
@Repository
class JdbcTransferRepository(
    private val jdbcClient: JdbcClient,
) : TransferRepository {
    override fun findById(id: UUID): Transfer? =
        jdbcClient
            .sql("select $COLUMNS from transfer where id = :id")
            .param("id", id)
            .query(ROW_MAPPER)
            .optional()
            .orElse(null)

    override fun findByIdempotencyKey(key: String): Transfer? =
        jdbcClient
            .sql("select $COLUMNS from transfer where idempotency_key = :key")
            .param("key", key)
            .query(ROW_MAPPER)
            .optional()
            .orElse(null)

    override fun insert(transfer: Transfer): Boolean =
        jdbcClient
            .sql(
                """
                insert into transfer (id, source_account_id, destination_account_id, amount, description, idempotency_key,
                                      status, step, failure_reason, deadline_at, next_attempt_at, attempts, correlation_id,
                                      created_at, updated_at)
                values (:id, :source, :destination, :amount, :description, :key,
                        :status, :step, :failureReason, :deadline, :nextAttemptAt, :attempts, :correlationId,
                        :createdAt, :updatedAt)
                on conflict (idempotency_key) do nothing
                """.trimIndent(),
            ).param("id", transfer.id)
            .param("source", transfer.sourceAccountId)
            .param("destination", transfer.destinationAccountId)
            .param("amount", transfer.amount.cents)
            .param("description", transfer.description)
            .param("key", transfer.idempotencyKey)
            .param("status", transfer.status.name)
            .param("step", transfer.step.name)
            .param("failureReason", transfer.failureReason)
            .param("deadline", transfer.deadline.utc())
            .param("nextAttemptAt", transfer.createdAt.utc())
            .param("attempts", transfer.attempts)
            .param("correlationId", transfer.correlationId)
            .param("createdAt", transfer.createdAt.utc())
            .param("updatedAt", transfer.updatedAt.utc())
            .update() == 1

    override fun tryAcquireLease(
        id: UUID,
        owner: String,
        until: Instant,
        now: Instant,
    ): Boolean =
        jdbcClient
            .sql(
                """
                update transfer set locked_until = :until, lock_owner = :owner
                where id = :id and status = 'PENDING'
                  and (locked_until is null or locked_until < :now or lock_owner = :owner)
                """.trimIndent(),
            ).param("until", until.utc())
            .param("owner", owner)
            .param("id", id)
            .param("now", now.utc())
            .update() == 1

    /** The lease is valid at the instant the saga stamped on the transfer (`updatedAt`). */
    override fun saveIfOwner(
        transfer: Transfer,
        owner: String,
        nextAttemptAt: Instant?,
        leaseUntil: Instant,
    ): Boolean =
        jdbcClient
            .sql(
                """
                update transfer set status = :status, step = :step, failure_reason = :failureReason, attempts = :attempts,
                                    next_attempt_at = :nextAttemptAt, locked_until = :leaseUntil, updated_at = :updatedAt
                where id = :id and lock_owner = :owner and locked_until >= :now
                """.trimIndent(),
            ).param("status", transfer.status.name)
            .param("step", transfer.step.name)
            .param("failureReason", transfer.failureReason)
            .param("attempts", transfer.attempts)
            .param("nextAttemptAt", nextAttemptAt?.utc())
            .param("leaseUntil", leaseUntil.utc())
            .param("updatedAt", transfer.updatedAt.utc())
            .param("id", transfer.id)
            .param("owner", owner)
            .param("now", transfer.updatedAt.utc())
            .update() == 1

    override fun releaseLease(
        id: UUID,
        owner: String,
    ) {
        jdbcClient
            .sql("update transfer set locked_until = null, lock_owner = null where id = :id and lock_owner = :owner")
            .param("id", id)
            .param("owner", owner)
            .update()
    }

    /**
     * FOR UPDATE SKIP LOCKED only guards the selection, so concurrent schedulers do not queue on the same rows; the
     * lease, not this lock, keeps the transfer for the one that wins [tryAcquireLease]. A lease is free with the same
     * boundary as there: `locked_until < now`.
     */
    override fun findDue(
        now: Instant,
        limit: Int,
    ): List<UUID> =
        jdbcClient
            .sql(
                """
                select id from transfer
                where status = 'PENDING' and next_attempt_at <= :now and (locked_until is null or locked_until < :now)
                order by next_attempt_at
                limit :limit
                for update skip locked
                """.trimIndent(),
            ).param("now", now.utc())
            .param("limit", limit)
            .query { rs, _ -> rs.getObject("id", UUID::class.java) }
            .list()

    private fun Instant.utc(): OffsetDateTime = OffsetDateTime.ofInstant(this, ZoneOffset.UTC)

    private companion object {
        const val COLUMNS =
            "id, source_account_id, destination_account_id, amount, description, idempotency_key, step, failure_reason, " +
                "deadline_at, attempts, correlation_id, created_at, updated_at"

        val ROW_MAPPER =
            RowMapper { rs, _ ->
                Transfer(
                    id = rs.getObject("id", UUID::class.java),
                    sourceAccountId = rs.getObject("source_account_id", UUID::class.java),
                    destinationAccountId = rs.getObject("destination_account_id", UUID::class.java),
                    amount = Money(rs.getLong("amount")),
                    description = rs.getString("description"),
                    idempotencyKey = rs.getString("idempotency_key"),
                    step = TransferStep.fromName(rs.getString("step")),
                    failureReason = rs.getString("failure_reason"),
                    deadline = rs.getObject("deadline_at", OffsetDateTime::class.java).toInstant(),
                    attempts = rs.getInt("attempts"),
                    correlationId = rs.getString("correlation_id"),
                    createdAt = rs.getObject("created_at", OffsetDateTime::class.java).toInstant(),
                    updatedAt = rs.getObject("updated_at", OffsetDateTime::class.java).toInstant(),
                )
            }
    }
}
