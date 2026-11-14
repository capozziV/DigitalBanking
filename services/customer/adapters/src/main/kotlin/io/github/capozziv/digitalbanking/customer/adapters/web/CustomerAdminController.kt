package io.github.capozziv.digitalbanking.customer.adapters.web

import io.github.capozziv.digitalbanking.customer.adapters.web.dto.ChangeCustomerStatusDTO
import io.github.capozziv.digitalbanking.customer.adapters.web.dto.CustomerDTO
import io.github.capozziv.digitalbanking.customer.application.usecase.ChangeCustomerStatusUseCase
import io.github.capozziv.digitalbanking.customer.application.usecase.GetCustomerUseCase
import io.swagger.v3.oas.annotations.Operation
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
import java.util.UUID

/** Admin operations on any customer; the security library lets only the admin reach the routes under `/admin`. */
@RestController
@RequestMapping("/admin/customers")
class CustomerAdminController(
    private val getCustomer: GetCustomerUseCase,
    private val changeCustomerStatus: ChangeCustomerStatusUseCase,
) {
    @Operation(summary = "A customer by id (admin)")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "The customer",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = CustomerDTO::class))],
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
    ): CustomerDTO = CustomerDTO.from(getCustomer.execute(id))

    @Operation(summary = "Block or unblock a customer (admin); the same status again changes nothing")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "The customer with its status; CustomerBlocked or CustomerUnblocked is published when it changed",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = CustomerDTO::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = ApiDocs.VALIDATION_ERROR + " (the id is not a UUID, or the status is missing or not ACTIVE or BLOCKED)",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = ApiDocs.NOT_FOUND,
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @PutMapping("/{id}/status")
    fun changeStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ChangeCustomerStatusDTO,
    ): CustomerDTO = CustomerDTO.from(changeCustomerStatus.execute(id, checkNotNull(request.status)))
}
