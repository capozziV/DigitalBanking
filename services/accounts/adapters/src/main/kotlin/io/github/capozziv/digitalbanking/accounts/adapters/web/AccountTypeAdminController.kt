package io.github.capozziv.digitalbanking.accounts.adapters.web

import io.github.capozziv.digitalbanking.accounts.adapters.web.dto.AccountTypeLimitResponse
import io.github.capozziv.digitalbanking.accounts.adapters.web.dto.ChangeOverdraftLimitRequest
import io.github.capozziv.digitalbanking.accounts.application.usecase.ChangeOverdraftLimitUseCase
import io.github.capozziv.digitalbanking.accounts.application.usecase.ListAccountTypesUseCase
import io.github.capozziv.digitalbanking.accounts.domain.AccountType
import io.github.capozziv.digitalbanking.accounts.domain.OverdraftLimit
import jakarta.validation.Valid
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
    @GetMapping
    fun list(): List<AccountTypeLimitResponse> = listAccountTypes.execute().map(AccountTypeLimitResponse::from)

    /** The type in the path is a resource name: unknown or lowercase is 404, not 400. */
    @PutMapping("/{type}")
    fun changeLimit(
        @PathVariable type: String,
        @Valid @RequestBody request: ChangeOverdraftLimitRequest,
    ): AccountTypeLimitResponse {
        val accountType = AccountType.entries.find { it.name == type } ?: throw AccountTypeNotFoundException(type)
        val limit = OverdraftLimit(checkNotNull(request.overdraftLimit))
        return AccountTypeLimitResponse.from(changeOverdraftLimit.execute(accountType, limit))
    }
}
