package io.github.capozziv.digitalbanking.accounts.adapters.web

import io.github.capozziv.digitalbanking.accounts.adapters.web.dto.AccountDTO
import io.github.capozziv.digitalbanking.accounts.application.usecase.CloseAccountUseCase
import io.github.capozziv.digitalbanking.security.currentCaller
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.slf4j.LoggerFactory
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/** Closing an account, by its owner or an admin. */
@RestController
@RequestMapping("/accounts")
class AccountClosingController(
    private val closeAccount: CloseAccountUseCase,
) {
    @Operation(
        summary = "Close an account for good",
        description =
            "No body. Only an account with a balance of exactly zero and no outgoing transfer still pending is closed; " +
                "the transfers service is asked on every call. A closed account takes no debits nor credits, its ledger " +
                "stays readable, and its customer may open a new account.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "The account, now CLOSED",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = AccountDTO::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "/problems/validation-error: the id is not a UUID",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "/problems/account-not-found: no such account, or an account of another customer",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "422",
            description =
                "/problems/balance-not-zero: the balance (member `balance`, in cents) is not zero. " +
                    "/problems/pending-transfers: outgoing transfers still pending (member `count`). " +
                    "/problems/account-closed: the account is already closed",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "503",
            description =
                "/problems/transfers-unavailable: the transfer service is down, slower than the timeout or answered an " +
                    "error; the account was not closed, retry later. " + ApiDocs.ACCOUNT_BUSY,
            headers = [Header(name = "Retry-After", description = ApiDocs.RETRY_AFTER, schema = Schema(type = "integer"))],
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @PostMapping("/{id}/close")
    fun close(
        @PathVariable id: UUID,
    ): AccountDTO {
        val view = closeAccount.execute(id, currentCaller())
        log.info("account closed: accountId={}", id)
        return AccountDTO.from(view)
    }

    private companion object {
        val log = LoggerFactory.getLogger(AccountClosingController::class.java)
    }
}
