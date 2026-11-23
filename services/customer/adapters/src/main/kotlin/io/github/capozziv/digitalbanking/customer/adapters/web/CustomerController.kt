package io.github.capozziv.digitalbanking.customer.adapters.web

import io.github.capozziv.digitalbanking.customer.adapters.web.dto.ChangePhoneDTO
import io.github.capozziv.digitalbanking.customer.adapters.web.dto.CustomerDTO
import io.github.capozziv.digitalbanking.customer.adapters.web.dto.CustomerLookupByPhoneDTO
import io.github.capozziv.digitalbanking.customer.adapters.web.dto.RegisterCustomerDTO
import io.github.capozziv.digitalbanking.customer.application.usecase.ChangePhoneUseCase
import io.github.capozziv.digitalbanking.customer.application.usecase.FindCustomerByPhoneUseCase
import io.github.capozziv.digitalbanking.customer.application.usecase.GetOwnCustomerUseCase
import io.github.capozziv.digitalbanking.customer.application.usecase.RegisterCustomerCommand
import io.github.capozziv.digitalbanking.customer.application.usecase.RegisterCustomerUseCase
import io.github.capozziv.digitalbanking.customer.domain.PhoneNumber
import io.github.capozziv.digitalbanking.security.currentCaller
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

/** The customer acting on its own record: the id is always the `sub` of the token, never a parameter. */
@RestController
@RequestMapping("/customers")
class CustomerController(
    private val registerCustomer: RegisterCustomerUseCase,
    private val getOwnCustomer: GetOwnCustomerUseCase,
    private val changePhone: ChangePhoneUseCase,
    private val findCustomerByPhone: FindCustomerByPhoneUseCase,
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
            description = ApiDocs.VALIDATION_ERROR + " (the phone must be a Brazilian mobile number in E.164)",
        ),
        ApiResponse(
            responseCode = "409",
            description = "/problems/phone-already-in-use: another customer has the phone",
        ),
    )
    @PostMapping
    fun register(
        @Valid @RequestBody request: RegisterCustomerDTO,
    ): ResponseEntity<CustomerDTO> {
        val result =
            registerCustomer.execute(
                RegisterCustomerCommand(checkNotNull(request.name), checkNotNull(request.email), PhoneNumber(checkNotNull(request.phone))),
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
        ),
    )
    @GetMapping("/me")
    fun me(): CustomerDTO = CustomerDTO.from(getOwnCustomer.execute(currentCaller()))

    @Operation(summary = "Change the phone of the customer of the token; the same number again changes nothing")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "The customer with the new phone; CustomerPhoneChanged is published when it changed",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = CustomerDTO::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = ApiDocs.VALIDATION_ERROR + " (the phone must be a Brazilian mobile number in E.164)",
        ),
        ApiResponse(
            responseCode = "404",
            description = ApiDocs.NOT_FOUND + ": the user has not registered yet",
        ),
        ApiResponse(
            responseCode = "409",
            description = "/problems/phone-already-in-use: another customer has the phone",
        ),
    )
    @PutMapping("/me/phone")
    fun changePhone(
        @Valid @RequestBody request: ChangePhoneDTO,
    ): CustomerDTO = CustomerDTO.from(changePhone.execute(PhoneNumber(checkNotNull(request.phone)), currentCaller()))

    @Operation(
        summary = "The id and the full name of the customer that has a phone",
        description =
            "Reveals that the number has an owner and the owner's full name; the phone is not verified. " +
                "Encode the plus sign of the phone as %2B (an unencoded plus reaches the service as a space, which is read as a plus).",
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "The customer id and full name",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = CustomerLookupByPhoneDTO::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = ApiDocs.VALIDATION_ERROR + " (the phone is missing or not a Brazilian mobile number in E.164)",
        ),
        ApiResponse(
            responseCode = "404",
            description = ApiDocs.NOT_FOUND + ": no customer has the phone",
        ),
    )
    @GetMapping("/lookup")
    fun lookup(
        @RequestParam("phone") phone: String,
    ): CustomerLookupByPhoneDTO = CustomerLookupByPhoneDTO.from(findCustomerByPhone.execute(parsePhone(phone)))

    /** A `+` that was not encoded in the query string reaches the service as a space: read it back as the plus. */
    private fun parsePhone(raw: String): PhoneNumber =
        try {
            PhoneNumber(if (raw.startsWith(" ")) "+" + raw.trimStart() else raw)
        } catch (e: IllegalArgumentException) {
            throw InvalidPhoneException("phone").also { it.initCause(e) }
        }
}
