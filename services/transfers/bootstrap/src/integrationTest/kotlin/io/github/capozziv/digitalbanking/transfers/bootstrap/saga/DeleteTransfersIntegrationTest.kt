package io.github.capozziv.digitalbanking.transfers.bootstrap.saga

import io.github.capozziv.digitalbanking.transfers.bootstrap.TransfersIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.junit.jupiter.api.Test
import java.sql.DriverManager
import java.time.Duration
import java.util.UUID
import java.util.concurrent.Executors

/**
 * The cleanup helper of the saga tests must be atomic against a writer that is in the middle of recording a step, as
 * the scheduler of a previous test can be: with the steps deleted in one transaction and the transfers in the next, a
 * step committed in between made `delete from transfer` violate the foreign key of `transfer_step`.
 */
class DeleteTransfersIntegrationTest : TransfersIntegrationTest() {
    @Test
    fun `deleting the transfers waits for a writer recording a step instead of failing on the foreign key`() {
        val id = UUID.randomUUID()
        insertTransfer(id)
        val executor = Executors.newSingleThreadExecutor()
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { writer ->
            writer.autoCommit = false
            writer
                .prepareStatement(
                    "insert into transfer_step (transfer_id, step, outcome, created_at) values (?, 'DEBIT', 'SUCCESS', now())",
                ).use {
                    it.setObject(1, id)
                    it.executeUpdate()
                }

            val cleanup = executor.submit { ownerDataSource.deleteTransfers() }
            Awaitility
                .await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(20))
                .until { ownerJdbc.sql("select count(*) from pg_locks where not granted").query(Int::class.java).single() > 0 }
            writer.commit()

            cleanup.get() // fails with the foreign key violation while the cleanup is not atomic
        }
        executor.shutdown()

        assertThat(ownerJdbc.sql("select count(*) from transfer").query(Int::class.java).single()).isZero()
        assertThat(ownerJdbc.sql("select count(*) from transfer_step").query(Int::class.java).single()).isZero()
    }

    private fun insertTransfer(id: UUID) {
        ownerJdbc
            .sql(
                """
                insert into transfer (id, source_account_id, destination_account_id, amount, idempotency_key, status, step,
                                      deadline_at, next_attempt_at, created_at, updated_at, limit_day)
                values (:id, gen_random_uuid(), gen_random_uuid(), 100, :key, 'PENDING', 'DEBIT', now(), now(), now(), now(),
                        current_date)
                """.trimIndent(),
            ).param("id", id)
            .param("key", "key-$id")
            .update()
    }
}
