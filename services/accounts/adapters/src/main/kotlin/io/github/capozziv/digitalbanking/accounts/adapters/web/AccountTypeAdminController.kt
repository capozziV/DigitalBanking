package io.github.capozziv.digitalbanking.accounts.adapters.web

import io.github.capozziv.digitalbanking.accounts.adapters.web.dto.AccountTypeLimitDTO
import io.github.capozziv.digitalbanking.accounts.adapters.web.dto.ChangeOverdraftLimitDTO
import io.github.capozziv.digitalbanking.accounts.application.usecase.ChangeOverdraftLimitUseCase
import io.github.capozziv.digitalbanking.accounts.application.usecase.ListAccountTypesUseCase
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.OverdraftLimit
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Overdraft limits per account type. Open until stage 7, which restricts it to administrators. */
@RestController
@RequestMapping("/admin/account-types")
class AccountTypeAdminController(
    private val listAccountTypes: ListAccountTypesUseCase,
    private val changeOverdraftLimit: ChangeOverdraftLimitUseCase,
) {
    @Operation(summary = "List the overdraft limit of each account type")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "The account types with their overdraft limits",
            content = [
                Content(
                    mediaType = "application/json",
                    array = ArraySchema(schema = Schema(implementation = AccountTypeLimitDTO::class)),
                ),
            ],
        ),
    )
    @GetMapping
    fun list(): List<AccountTypeLimitDTO> = listAccountTypes.execute().map(AccountTypeLimitDTO::from)

    /** The type in the path is a resource name: unknown or lowercase is 404, not 400. */
    @Operation(summary = "Change the overdraft limit of an account type")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "The account type with its new overdraft limit",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = AccountTypeLimitDTO::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "/problems/validation-error: the overdraft limit is missing or negative",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "/problems/account-type-not-found: unknown or lowercase type",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @PutMapping("/{type}")
    fun changeLimit(
        @PathVariable type: String,
        @Valid @RequestBody request: ChangeOverdraftLimitDTO,
    ): AccountTypeLimitDTO {
        val accountType = AccountType.entries.find { it.name == type } ?: throw AccountTypeNotFoundException(type)
        val limit = OverdraftLimit(checkNotNull(request.overdraftLimit))
        return AccountTypeLimitDTO.from(changeOverdraftLimit.execute(accountType, limit))
    }
}
