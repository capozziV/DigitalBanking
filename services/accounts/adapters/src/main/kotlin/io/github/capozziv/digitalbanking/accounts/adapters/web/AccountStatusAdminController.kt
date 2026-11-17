package io.github.capozziv.digitalbanking.accounts.adapters.web

import io.github.capozziv.digitalbanking.accounts.adapters.web.dto.AccountDTO
import io.github.capozziv.digitalbanking.accounts.adapters.web.dto.ChangeAccountStatusDTO
import io.github.capozziv.digitalbanking.accounts.application.usecase.ChangeAccountStatusUseCase
import io.github.capozziv.digitalbanking.accounts.domain.AccountStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/** Blocking and unblocking accounts. Administrators only: every route under `/admin/` is a rule of libs/security. */
@RestController
@RequestMapping("/admin/accounts")
class AccountStatusAdminController(
    private val changeAccountStatus: ChangeAccountStatusUseCase,
) {
    @Operation(
        summary = "Block or unblock an account",
        description =
            "BLOCKED refuses debits (422 /problems/account-blocked) and still takes credits; ACTIVE lifts the block. " +
                "The same status again changes nothing and emits no event. CLOSED is not accepted here: an account is " +
                "closed by POST /accounts/{id}/close.",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "The account with its status",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = AccountDTO::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "/problems/validation-error: the id is not a UUID, or the status is missing, unknown or CLOSED",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "/problems/account-not-found",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "422",
            description = "/problems/account-closed: a closed account never changes status",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "503",
            description = ApiDocs.ACCOUNT_BUSY,
            headers = [Header(name = "Retry-After", description = ApiDocs.RETRY_AFTER, schema = Schema(type = "integer"))],
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @PutMapping("/{id}/status")
    fun changeStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ChangeAccountStatusDTO,
    ): AccountDTO {
        val status = checkNotNull(request.status)
        if (status == AccountStatus.CLOSED) {
            throw InvalidRequestException(listOf(InvalidField("status", "must be ACTIVE or BLOCKED")))
        }
        val view = changeAccountStatus.execute(id, status)
        log.info("account status changed: accountId={} status={}", id, status)
        return AccountDTO.from(view)
    }

    private companion object {
        val log = LoggerFactory.getLogger(AccountStatusAdminController::class.java)
    }
}
