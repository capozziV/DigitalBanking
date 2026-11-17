package io.github.capozziv.digitalbanking.transfers.adapters.persistence

import io.github.capozziv.digitalbanking.transfers.domain.Money
import io.github.capozziv.digitalbanking.transfers.domain.Transfer
import io.github.capozziv.digitalbanking.transfers.domain.TransferStep
import org.springframework.jdbc.core.RowMapper
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

/** The columns of a `transfer` row and how they become a [Transfer], shared by every read of transfers. */
internal object TransferRows {
    const val COLUMNS =
        "id, source_account_id, destination_account_id, amount, description, idempotency_key, step, failure_reason, " +
            "deadline_at, attempts, correlation_id, created_at, updated_at, limit_day, limit_reserved, source_customer_id"

    val ROW_MAPPER: RowMapper<Transfer> =
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
                limitDay = rs.getObject("limit_day", LocalDate::class.java),
                limitReserved = rs.getBoolean("limit_reserved"),
                sourceCustomerId = rs.getObject("source_customer_id", UUID::class.java),
            )
        }
}
