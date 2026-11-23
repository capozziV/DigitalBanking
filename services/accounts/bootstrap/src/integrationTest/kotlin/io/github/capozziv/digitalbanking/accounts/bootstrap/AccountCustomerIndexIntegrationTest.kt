package io.github.capozziv.digitalbanking.accounts.bootstrap

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * The welcome bonus check looks for another account of the same customer in any status (JdbcAccountRepository), but the
 * only index on account(customer_id) is partial (status <> 'CLOSED', V9), which cannot serve it: it needs a full one.
 */
class AccountCustomerIndexIntegrationTest : AccountsIntegrationTest() {
    @Test
    fun `account has a full index on customer_id, not only the partial one`() {
        val fullIndexes =
            ownerJdbc
                .sql(
                    """
                    select i.indexrelid::regclass::text
                    from pg_index i
                    where i.indrelid = 'account'::regclass
                      and i.indpred is null
                      and i.indisvalid
                      and i.indkey[0] = (select attnum from pg_attribute where attrelid = 'account'::regclass and attname = 'customer_id')
                    """.trimIndent(),
                ).query(String::class.java)
                .list()

        assertThat(fullIndexes).isNotEmpty()
    }
}
