package io.github.capozziv.digitalbanking.accounts.adapters.web

import io.github.capozziv.digitalbanking.accounts.adapters.web.dto.AccountDTO
import io.github.capozziv.digitalbanking.accounts.adapters.web.dto.ChangeAccountTypeDTO
import io.github.capozziv.digitalbanking.accounts.adapters.web.dto.LedgerEntryDTO
import io.github.capozziv.digitalbanking.accounts.adapters.web.dto.OpenAccountDTO
import io.github.capozziv.digitalbanking.accounts.adapters.web.dto.RecordEntryDTO
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
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
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
    @Operation(summary = "Open an account (type FREE, zero balance)")
    @ApiResponses(
        ApiResponse(
            responseCode = "201",
            description = "Account opened",
            headers = [Header(name = "Location", description = "/accounts/{id}")],
            content = [Content(mediaType = "application/json", schema = Schema(implementation = AccountDTO::class))],
        ),
        ApiResponse(
            responseCode = "200",
            description = "Retry with the same key: the account opened by the first request",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = AccountDTO::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = ApiDocs.BAD_REQUEST,
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "409",
            description = "/problems/customer-already-has-account: the customer already has an account (opened with another key)",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "422",
            description = "/problems/idempotency-key-reused: the key was used to open the account of another customer",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @PostMapping
    fun open(
        @Parameter(description = ApiDocs.IDEMPOTENCY_KEY)
        @RequestHeader(IdempotencyKeyHeader.NAME) idempotencyKey: String,
        @Valid @RequestBody request: OpenAccountDTO,
    ): ResponseEntity<AccountDTO> {
        val key = IdempotencyKeyHeader.require(idempotencyKey)
        val result = openAccount.execute(OpenAccountCommand(checkNotNull(request.customerId), key))
        val body = AccountDTO.from(getAccount.execute(result.account.id))
        return if (result.created) {
            ResponseEntity.created(URI.create("/accounts/${body.id}")).body(body)
        } else {
            ResponseEntity.ok(body)
        }
    }

    @Operation(summary = "Get an account with its balance, overdraft limit and available balance")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "The account",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = AccountDTO::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "/problems/validation-error: the id is not a UUID",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "/problems/account-not-found",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @GetMapping("/{id}")
    fun get(
        @PathVariable id: UUID,
    ): AccountDTO = AccountDTO.from(getAccount.execute(id))

    @Operation(summary = "Record a credit")
    @ApiResponses(
        ApiResponse(
            responseCode = "201",
            description = "Credit recorded",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = LedgerEntryDTO::class))],
        ),
        ApiResponse(
            responseCode = "200",
            description = "Retry with the same key: the entry recorded by the first request, the same body",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = LedgerEntryDTO::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = ApiDocs.BAD_REQUEST,
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "/problems/account-not-found",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "422",
            description = "/problems/idempotency-key-reused: the key was used for another kind or amount on this account",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "503",
            description = ApiDocs.ACCOUNT_BUSY,
            headers = [Header(name = "Retry-After", description = ApiDocs.RETRY_AFTER, schema = Schema(type = "integer"))],
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @PostMapping("/{id}/credits")
    fun credit(
        @PathVariable id: UUID,
        @Parameter(description = ApiDocs.IDEMPOTENCY_KEY)
        @RequestHeader(IdempotencyKeyHeader.NAME) idempotencyKey: String,
        @Valid @RequestBody request: RecordEntryDTO,
    ): ResponseEntity<LedgerEntryDTO> = entryResponse(recordCredit.execute(command(id, idempotencyKey, request)))

    @Operation(summary = "Record a debit, allowed down to the overdraft limit of the account type")
    @ApiResponses(
        ApiResponse(
            responseCode = "201",
            description = "Debit recorded",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = LedgerEntryDTO::class))],
        ),
        ApiResponse(
            responseCode = "200",
            description = "Retry with the same key: the entry recorded by the first request, the same body",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = LedgerEntryDTO::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = ApiDocs.BAD_REQUEST,
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "/problems/account-not-found",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "422",
            description =
                "/problems/insufficient-available-balance: the available balance (member `availableBalance`, in cents) " +
                    "does not cover the amount; nothing is recorded, so the same key can be retried later. " +
                    "/problems/idempotency-key-reused: the key was used for another kind or amount on this account",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "503",
            description = ApiDocs.ACCOUNT_BUSY,
            headers = [Header(name = "Retry-After", description = ApiDocs.RETRY_AFTER, schema = Schema(type = "integer"))],
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @PostMapping("/{id}/debits")
    fun debit(
        @PathVariable id: UUID,
        @Parameter(description = ApiDocs.IDEMPOTENCY_KEY)
        @RequestHeader(IdempotencyKeyHeader.NAME) idempotencyKey: String,
        @Valid @RequestBody request: RecordEntryDTO,
    ): ResponseEntity<LedgerEntryDTO> =
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

    @Operation(summary = "Change the account type, which sets its overdraft limit")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "The account with its new type and overdraft limit",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = AccountDTO::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "/problems/validation-error: the id is not a UUID or the type is missing or unknown",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "/problems/account-not-found",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "503",
            description = ApiDocs.ACCOUNT_BUSY,
            headers = [Header(name = "Retry-After", description = ApiDocs.RETRY_AFTER, schema = Schema(type = "integer"))],
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @PutMapping("/{id}/type")
    fun changeType(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ChangeAccountTypeDTO,
    ): AccountDTO {
        changeAccountType.execute(id, checkNotNull(request.type))
        return AccountDTO.from(getAccount.execute(id))
    }

    private fun command(
        accountId: UUID,
        idempotencyKey: String,
        request: RecordEntryDTO,
    ) = RecordEntryCommand(
        accountId = accountId,
        amount = Money(checkNotNull(request.amount)),
        description = request.normalizedDescription(),
        idempotencyKey = IdempotencyKeyHeader.require(idempotencyKey),
    )

    private fun entryResponse(result: RecordEntryResult): ResponseEntity<LedgerEntryDTO> =
        ResponseEntity
            .status(if (result.created) HttpStatus.CREATED else HttpStatus.OK)
            .body(LedgerEntryDTO.from(result.entry))

    private companion object {
        val log = LoggerFactory.getLogger(AccountController::class.java)
    }
}
