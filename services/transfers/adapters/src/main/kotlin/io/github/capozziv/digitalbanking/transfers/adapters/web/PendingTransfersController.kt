package io.github.capozziv.digitalbanking.transfers.adapters.web

import io.github.capozziv.digitalbanking.transfers.adapters.web.dto.PendingTransfersDTO
import io.github.capozziv.digitalbanking.transfers.application.usecase.CountPendingTransfersUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * What accounts asks before closing an account: how many PENDING transfers leave it. The security library lets only a
 * service token (never a user's, not even an admin's) reach the routes under `/internal`. Any account id is answered,
 * an unknown one with zero: transfers does not know which accounts exist.
 */
@RestController
@RequestMapping("/internal/accounts/{accountId}/pending-transfers")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_AUTH)
class PendingTransfersController(
    private val countPendingTransfers: CountPendingTransfersUseCase,
) {
    @Operation(summary = "Count the PENDING transfers leaving an account (service token only)")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "The count of PENDING transfers whose source is the account; zero when there is none",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = PendingTransfersDTO::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "/problems/validation-error: the account id is not a UUID",
        ),
    )
    @GetMapping
    fun count(
        @PathVariable accountId: UUID,
    ): PendingTransfersDTO = PendingTransfersDTO(countPendingTransfers.execute(accountId))
}
