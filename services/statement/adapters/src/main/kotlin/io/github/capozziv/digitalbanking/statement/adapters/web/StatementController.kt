package io.github.capozziv.digitalbanking.statement.adapters.web

import io.github.capozziv.digitalbanking.security.currentCaller
import io.github.capozziv.digitalbanking.statement.adapters.web.dto.StatementDTO
import io.github.capozziv.digitalbanking.statement.application.usecase.GetStatementUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/accounts/{accountId}/statement")
class StatementController(
    private val getStatement: GetStatementUseCase,
) {
    @Operation(
        summary = "List the entries of an account, newest first, with cursor pagination",
        description = "Only the customer that owns the account, or an admin, can read it.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "The page, with the cursor of the next one; no entries when the account has none yet",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = StatementDTO::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description =
                "/problems/validation-error: accountId not a UUID, limit not a number or outside 1..100, " +
                    "or a cursor that was not issued by this API",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "401",
            description = "/problems/unauthorized: no token, or an invalid or expired one",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description =
                "/problems/account-not-found: the statement knows no account with this id, or the account belongs " +
                    "to another customer (same answer, so it does not reveal that the account exists)",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @GetMapping
    fun get(
        @Parameter(description = "Account whose statement is listed (UUID)", required = true)
        @PathVariable accountId: UUID,
        @Parameter(description = "`nextCursor` of the previous page; absent for the first page", required = false)
        @RequestParam(required = false) cursor: String?,
        @Parameter(description = "Entries per page, 1 to 100", required = false)
        @RequestParam(defaultValue = "${GetStatementUseCase.DEFAULT_LIMIT}") limit: Int,
    ): StatementDTO {
        if (limit !in GetStatementUseCase.MIN_LIMIT..GetStatementUseCase.MAX_LIMIT) {
            throw InvalidRequestException(
                listOf(InvalidField("limit", "must be between ${GetStatementUseCase.MIN_LIMIT} and ${GetStatementUseCase.MAX_LIMIT}")),
            )
        }
        val decoded = cursor?.let(StatementCursorCodec::decode)
        return StatementDTO.from(accountId, getStatement.execute(accountId, decoded, limit, currentCaller()))
    }
}
