package io.github.capozziv.digitalbanking.notification.bootstrap.persistence

import io.github.capozziv.digitalbanking.notification.application.port.CustomerNameRepository
import io.github.capozziv.digitalbanking.notification.application.port.KnownAccountRepository
import io.github.capozziv.digitalbanking.notification.bootstrap.NotificationIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

/** Runs as the application user, so the grants of the migration are exercised too. */
class JdbcCustomerNameRepositoryIntegrationTest : NotificationIntegrationTest() {
    @Autowired
    private lateinit var names: CustomerNameRepository

    @Autowired
    private lateinit var accounts: KnownAccountRepository

    @BeforeEach
    fun cleanTables() {
        ownerJdbc.sql("delete from customer_names").update()
        ownerJdbc.sql("delete from known_accounts").update()
    }

    @Test
    fun `finds the name of the owner of each known account`() {
        val (ana, anaAccount, anaSecondAccount) = Triple(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        val (bruno, brunoAccount) = UUID.randomUUID() to UUID.randomUUID()
        accounts.save(anaAccount, ana)
        accounts.save(anaSecondAccount, ana)
        accounts.save(brunoAccount, bruno)
        names.save(ana, "Ana Souza")
        names.save(bruno, "Bruno Lima")

        val found = names.namesOfAccountOwners(setOf(anaAccount, anaSecondAccount, brunoAccount))

        assertThat(found).isEqualTo(mapOf(anaAccount to "Ana Souza", anaSecondAccount to "Ana Souza", brunoAccount to "Bruno Lima"))
    }

    @Test
    fun `saving the same customer again replaces the name`() {
        val (ana, anaAccount) = UUID.randomUUID() to UUID.randomUUID()
        accounts.save(anaAccount, ana)
        names.save(ana, "Ana Souza")

        names.save(ana, "Ana Souza Lima")

        assertThat(names.namesOfAccountOwners(setOf(anaAccount))).isEqualTo(mapOf(anaAccount to "Ana Souza Lima"))
        assertThat(ownerJdbc.sql("select count(*) from customer_names").query(Int::class.java).single()).isEqualTo(1)
    }

    @Test
    fun `an account without a known owner or without a name is left out`() {
        val (ana, anaAccount) = UUID.randomUUID() to UUID.randomUUID()
        accounts.save(anaAccount, ana)
        val unknownAccount = UUID.randomUUID()

        assertThat(names.namesOfAccountOwners(setOf(anaAccount, unknownAccount))).isEmpty()
        assertThat(names.namesOfAccountOwners(emptySet())).isEmpty()
    }
}
