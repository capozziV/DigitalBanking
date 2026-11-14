package io.github.capozziv.digitalbanking.customer.adapters.web

import io.github.capozziv.digitalbanking.customer.adapters.web.dto.CustomerLookupDTO
import io.github.capozziv.digitalbanking.customer.application.usecase.GetCustomerUseCase
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

/**
 * The lookup the accounts and transfers services make before opening an account or starting a transfer. The security
 * library lets only a service token (never a user's, not even an admin's) reach the routes under `/internal`.
 */
@RestController
@RequestMapping("/internal/customers")
class InternalCustomerController(
    private val getCustomer: GetCustomerUseCase,
) {
    @Operation(summary = "Id and status of a customer (service token only)")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "The id and the status (ACTIVE or BLOCKED)",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = CustomerLookupDTO::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "/problems/validation-error: the id is not a UUID",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = ApiDocs.NOT_FOUND,
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @GetMapping("/{id}")
    fun get(
        @PathVariable id: UUID,
    ): CustomerLookupDTO = CustomerLookupDTO.from(getCustomer.execute(id))
}
