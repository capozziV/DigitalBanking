package io.github.capozziv.digitalbanking.transfers.application.fake

import io.github.capozziv.digitalbanking.transfers.domain.AccountsCall
import io.github.capozziv.digitalbanking.transfers.domain.Money
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class FakeAccountsGatewayTest {
    private val tx = ImmediateTransactionRunner()
    private val gateway = FakeAccountsGateway(tx)
    private val call = AccountsCall.Debit(UUID.randomUUID(), Money(1), "key", "description")

    @Test
    fun `calling accounts inside a transaction is detected`() {
        gateway.reply(FakeAccountsGateway.created)

        assertThatThrownBy { tx.inTransaction { gateway.perform(call) } }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("accounts called inside a database transaction")
        assertThat(gateway.calls).isEmpty()
    }

    @Test
    fun `calling accounts outside a transaction is allowed`() {
        gateway.reply(FakeAccountsGateway.created)

        assertThat(gateway.perform(call)).isEqualTo(FakeAccountsGateway.created)
    }
}
