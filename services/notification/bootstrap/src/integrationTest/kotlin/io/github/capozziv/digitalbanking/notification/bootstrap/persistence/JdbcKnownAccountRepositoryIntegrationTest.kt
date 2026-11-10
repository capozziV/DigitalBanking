package io.github.capozziv.digitalbanking.notification.bootstrap.persistence

import io.github.capozziv.digitalbanking.notification.application.port.KnownAccountRepository
import io.github.capozziv.digitalbanking.notification.bootstrap.NotificationIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class JdbcKnownAccountRepositoryIntegrationTest : NotificationIntegrationTest() {
    @Autowired
    private lateinit var repository: KnownAccountRepository

    @BeforeEach
    fun cleanTables() {
        ownerJdbc.sql("delete from known_accounts").update()
    }

    @Test
    fun `saves an account and finds its customer`() {
        val accountId = UUID.randomUUID()
        val customerId = UUID.randomUUID()

        repository.save(accountId, customerId)

        assertThat(repository.customerOf(accountId)).isEqualTo(customerId)
    }

    @Test
    fun `saving the same account twice keeps the stored owner without error`() {
        val accountId = UUID.randomUUID()
        val customerId = UUID.randomUUID()

        repository.save(accountId, customerId)
        repository.save(accountId, UUID.randomUUID())

        assertThat(repository.customerOf(accountId)).isEqualTo(customerId)
    }

    @Test
    fun `unknown account has no customer`() {
        assertThat(repository.customerOf(UUID.randomUUID())).isNull()
    }
}
