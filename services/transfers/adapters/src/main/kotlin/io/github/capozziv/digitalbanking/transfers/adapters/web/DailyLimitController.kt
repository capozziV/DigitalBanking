package io.github.capozziv.digitalbanking.transfers.adapters.web

import io.github.capozziv.digitalbanking.transfers.adapters.web.dto.DailyLimitDTO
import io.github.capozziv.digitalbanking.transfers.application.usecase.GetDailyLimitUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/accounts/{accountId}/daily-limit")
class DailyLimitController(
    private val getDailyLimit: GetDailyLimitUseCase,
) {
    @Operation(summary = "Get the daily transfer limit of an account for today (São Paulo day)")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description =
                "The limit, what is reserved today and what is left. An unknown account is not an error: " +
                    "this service does not know accounts, so it answers with zero used and the whole limit available.",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = DailyLimitDTO::class))],
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
    ): DailyLimitDTO = DailyLimitDTO.from(getDailyLimit.execute(accountId))
}
