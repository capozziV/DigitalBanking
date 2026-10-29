package io.github.capozziv.digitalbanking.e2e

import java.sql.Connection
import java.sql.DriverManager
import java.time.Duration
import java.time.Instant

/**
 * Read-only look into the `transfers` database as its owner, for what the API does not show: the lease of a transfer
 * and the audit trail of its steps.
 */
class TransfersDatabase(
    private val jdbcUrl: String,
    private val user: String,
    private val password: String,
) {
    data class Lease(
        val status: String,
        val step: String,
        val lockedUntil: Instant?,
        val lockOwner: String?,
    )

    fun transferCount(idempotencyKey: String): Int =
        connection().use { connection ->
            connection.prepareStatement("select count(*) from transfer where idempotency_key = ?").use { statement ->
                statement.setString(1, idempotencyKey)
                statement.executeQuery().use { rows ->
                    rows.next()
                    rows.getInt(1)
                }
            }
        }

    fun lease(transferId: String): Lease =
        connection().use { connection ->
            connection
                .prepareStatement("select status, step, locked_until, lock_owner from transfer where id = ?::uuid")
                .use { statement ->
                    statement.setString(1, transferId)
                    statement.executeQuery().use { rows ->
                        check(rows.next()) { "transfer $transferId not found" }
                        Lease(
                            status = rows.getString("status"),
                            step = rows.getString("step"),
                            lockedUntil = rows.getTimestamp("locked_until")?.toInstant(),
                            lockOwner = rows.getString("lock_owner"),
                        )
                    }
                }
        }

    /** The moment after which the saga stops retrying the credit and resolves it (`deadline_at`). */
    fun deadline(transferId: String): Instant =
        connection().use { connection ->
            connection.prepareStatement("select deadline_at from transfer where id = ?::uuid").use { statement ->
                statement.setString(1, transferId)
                statement.executeQuery().use { rows ->
                    check(rows.next()) { "transfer $transferId not found" }
                    rows.getTimestamp("deadline_at").toInstant()
                }
            }
        }

    /** When each step of the transfer was recorded, in order. */
    fun stepTimes(transferId: String): List<Instant> = steps(transferId).map { it.third }

    /** The recorded steps (name and time), in order. */
    fun recordedSteps(transferId: String): List<Pair<String, Instant>> = steps(transferId).map { it.first to it.third }

    /** Polls until the transfer has a step record with [step] and [outcome]. */
    fun awaitStepOutcome(
        transferId: String,
        step: String,
        outcome: String,
        timeout: Duration,
    ) {
        val deadline = Instant.now().plus(timeout)
        while (steps(transferId).none { it.first == step && it.second == outcome }) {
            check(Instant.now().isBefore(deadline)) {
                "transfer $transferId has no $step $outcome after $timeout; steps: ${steps(transferId)}"
            }
            Thread.sleep(POLL.toMillis())
        }
    }

    private fun steps(transferId: String): List<Triple<String, String, Instant>> =
        connection().use { connection ->
            connection
                .prepareStatement("select step, outcome, created_at from transfer_step where transfer_id = ?::uuid order by id")
                .use { statement ->
                    statement.setString(1, transferId)
                    statement.executeQuery().use { rows ->
                        buildList {
                            while (rows.next()) {
                                add(Triple(rows.getString("step"), rows.getString("outcome"), rows.getTimestamp("created_at").toInstant()))
                            }
                        }
                    }
                }
        }

    private fun connection(): Connection = DriverManager.getConnection(jdbcUrl, user, password)

    private companion object {
        val POLL: Duration = Duration.ofMillis(200)
    }
}
