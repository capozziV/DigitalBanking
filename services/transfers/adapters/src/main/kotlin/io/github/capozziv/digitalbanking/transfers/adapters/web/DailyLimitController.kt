package io.github.capozziv.digitalbanking.transfers.adapters.web

import io.github.capozziv.digitalbanking.security.currentCaller
import io.github.capozziv.digitalbanking.transfers.adapters.web.dto.DailyLimitDTO
import io.github.capozziv.digitalbanking.transfers.application.usecase.DailyLimitOutcome
import io.github.capozziv.digitalbanking.transfers.application.usecase.GetDailyLimitUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/accounts/{accountId}/daily-limit")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
class DailyLimitController(
    private val getDailyLimit: GetDailyLimitUseCase,
) {
    @Operation(summary = "Get the daily transfer limit of an account for today (São Paulo day)")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description =
                "The limit, what is reserved today and what is left; an account that never transferred has zero used " +
                    "and the whole limit available. A customer sees only its own accounts, checked with accounts; " +
                    "an admin sees any account id without that check (an unknown account shows zero used).",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = DailyLimitDTO::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description =
                "/problems/account-not-found: the account does not exist or is not the caller's (`accountId`); " +
                    "the same answer in both cases",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "503",
            description = "/problems/accounts-unavailable: accounts did not answer the owner check; retry after `Retry-After` seconds",
            headers = [Header(name = "Retry-After", description = "Seconds to wait before retrying (1)")],
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "/problems/validation-error: the account id is not a UUID",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @GetMapping
    fun get(
        @PathVariable accountId: UUID,
    ): DailyLimitDTO =
        when (val outcome = getDailyLimit.execute(accountId, currentCaller())) {
            is DailyLimitOutcome.Found -> DailyLimitDTO.from(outcome.usage)
            DailyLimitOutcome.AccountNotFound -> throw OwnAccountNotFoundException(accountId)
            DailyLimitOutcome.AccountsUnavailable -> throw AccountsUnavailableException()
        }
}
