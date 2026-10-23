package io.github.capozziv.digitalbanking.accounts.adapters.web

import io.github.capozziv.digitalbanking.accounts.adapters.web.dto.AccountResponse
import io.github.capozziv.digitalbanking.accounts.adapters.web.dto.ChangeAccountTypeRequest
import io.github.capozziv.digitalbanking.accounts.adapters.web.dto.LedgerEntryResponse
import io.github.capozziv.digitalbanking.accounts.adapters.web.dto.OpenAccountRequest
import io.github.capozziv.digitalbanking.accounts.adapters.web.dto.RecordEntryRequest
import io.github.capozziv.digitalbanking.accounts.application.usecase.ChangeAccountTypeUseCase
import io.github.capozziv.digitalbanking.accounts.application.usecase.DebitOutcome
import io.github.capozziv.digitalbanking.accounts.application.usecase.GetAccountUseCase
import io.github.capozziv.digitalbanking.accounts.application.usecase.OpenAccountCommand
import io.github.capozziv.digitalbanking.accounts.application.usecase.OpenAccountUseCase
import io.github.capozziv.digitalbanking.accounts.application.usecase.RecordCreditUseCase
import io.github.capozziv.digitalbanking.accounts.application.usecase.RecordDebitUseCase
import io.github.capozziv.digitalbanking.accounts.application.usecase.RecordEntryCommand
import io.github.capozziv.digitalbanking.accounts.application.usecase.RecordEntryResult
import io.github.capozziv.digitalbanking.accounts.domain.Money
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/accounts")
class AccountController(
    private val openAccount: OpenAccountUseCase,
    private val getAccount: GetAccountUseCase,
    private val recordCredit: RecordCreditUseCase,
    private val recordDebit: RecordDebitUseCase,
    private val changeAccountType: ChangeAccountTypeUseCase,
    meterRegistry: MeterRegistry,
) {
    private val refusedDebits =
        Counter
            .builder("accounts.debit.refused")
            .description("Debits refused because the available balance does not cover the amount")
            .register(meterRegistry)

    // The Idempotency-Key header is declared before the body: a missing key is reported (400
    // missing-idempotency-key) before the body is read and validated.

    /** 201 with the new account; 200 with the existing one when the opening key is repeated. */
    @PostMapping
    fun open(
        @RequestHeader(IdempotencyKeyHeader.NAME) idempotencyKey: String,
        @Valid @RequestBody request: OpenAccountRequest,
    ): ResponseEntity<AccountResponse> {
        val key = IdempotencyKeyHeader.require(idempotencyKey)
        val result = openAccount.execute(OpenAccountCommand(checkNotNull(request.customerId), key))
        val body = AccountResponse.from(getAccount.execute(result.account.id))
        return if (result.created) {
            ResponseEntity.created(URI.create("/accounts/${body.id}")).body(body)
        } else {
            ResponseEntity.ok(body)
        }
    }

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: UUID,
    ): AccountResponse = AccountResponse.from(getAccount.execute(id))

    @PostMapping("/{id}/credits")
    fun credit(
        @PathVariable id: UUID,
        @RequestHeader(IdempotencyKeyHeader.NAME) idempotencyKey: String,
        @Valid @RequestBody request: RecordEntryRequest,
    ): ResponseEntity<LedgerEntryResponse> = entryResponse(recordCredit.execute(command(id, idempotencyKey, request)))

    @PostMapping("/{id}/debits")
    fun debit(
        @PathVariable id: UUID,
        @RequestHeader(IdempotencyKeyHeader.NAME) idempotencyKey: String,
        @Valid @RequestBody request: RecordEntryRequest,
    ): ResponseEntity<LedgerEntryResponse> =
        when (val outcome = recordDebit.execute(command(id, idempotencyKey, request))) {
            is DebitOutcome.Recorded -> {
                entryResponse(outcome.result)
            }

            is DebitOutcome.Refused -> {
                log.info("debit refused: accountId={} availableBalance={}", id, outcome.availableBalance.cents)
                refusedDebits.increment()
                throw InsufficientAvailableBalanceException(id, outcome.availableBalance)
            }
        }

    @PutMapping("/{id}/type")
    fun changeType(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ChangeAccountTypeRequest,
    ): AccountResponse {
        changeAccountType.execute(id, checkNotNull(request.type))
        return AccountResponse.from(getAccount.execute(id))
    }

    private fun command(
        accountId: UUID,
        idempotencyKey: String,
        request: RecordEntryRequest,
    ) = RecordEntryCommand(
        accountId = accountId,
        amount = Money(checkNotNull(request.amount)),
        description = request.normalizedDescription(),
        idempotencyKey = IdempotencyKeyHeader.require(idempotencyKey),
    )

    private fun entryResponse(result: RecordEntryResult): ResponseEntity<LedgerEntryResponse> =
        ResponseEntity
            .status(if (result.created) HttpStatus.CREATED else HttpStatus.OK)
            .body(LedgerEntryResponse.from(result.entry))

    private companion object {
        val log = LoggerFactory.getLogger(AccountController::class.java)
    }
}
