package io.github.capozziv.digitalbanking.customer.adapters.web

import io.github.capozziv.digitalbanking.customer.adapters.web.dto.CustomerDTO
import io.github.capozziv.digitalbanking.customer.adapters.web.dto.RegisterCustomerDTO
import io.github.capozziv.digitalbanking.customer.application.usecase.GetOwnCustomerUseCase
import io.github.capozziv.digitalbanking.customer.application.usecase.RegisterCustomerCommand
import io.github.capozziv.digitalbanking.customer.application.usecase.RegisterCustomerUseCase
import io.github.capozziv.digitalbanking.security.currentCaller
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

/** The customer acting on its own record: the id is always the `sub` of the token, never a parameter. */
@RestController
@RequestMapping("/customers")
class CustomerController(
    private val registerCustomer: RegisterCustomerUseCase,
    private val getOwnCustomer: GetOwnCustomerUseCase,
) {
    @Operation(summary = "Register the customer of the token (id = sub); idempotent by the user")
    @ApiResponses(
        ApiResponse(
            responseCode = "201",
            description = "Customer registered",
            headers = [Header(name = "Location", description = "/customers/me")],
            content = [Content(mediaType = "application/json", schema = Schema(implementation = CustomerDTO::class))],
        ),
        ApiResponse(
            responseCode = "200",
            description = "The user was already registered: the stored customer, unchanged (the body of this request is ignored)",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = CustomerDTO::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = ApiDocs.VALIDATION_ERROR,
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @PostMapping
    fun register(
        @Valid @RequestBody request: RegisterCustomerDTO,
    ): ResponseEntity<CustomerDTO> {
        val result =
            registerCustomer.execute(
                RegisterCustomerCommand(checkNotNull(request.name), checkNotNull(request.email)),
                currentCaller(),
            )
        val body = CustomerDTO.from(result.customer)
        return if (result.created) {
            ResponseEntity.created(URI.create("/customers/me")).body(body)
        } else {
            ResponseEntity.ok(body)
        }
    }

    @Operation(summary = "The customer of the token, with its status")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "The customer",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = CustomerDTO::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = ApiDocs.NOT_FOUND + ": the user has not registered yet",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @GetMapping("/me")
    fun me(): CustomerDTO = CustomerDTO.from(getOwnCustomer.execute(currentCaller()))
}
