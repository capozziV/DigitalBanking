package io.github.capozziv.digitalbanking.notification.adapters.web

import io.github.capozziv.digitalbanking.notification.adapters.web.dto.MarkNotificationsReadDTO
import io.github.capozziv.digitalbanking.notification.adapters.web.dto.MarkedNotificationsDTO
import io.github.capozziv.digitalbanking.notification.adapters.web.dto.NotificationPageDTO
import io.github.capozziv.digitalbanking.notification.application.usecase.ListNotificationsUseCase
import io.github.capozziv.digitalbanking.notification.application.usecase.MarkNotificationsReadUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/notifications")
class NotificationController(
    private val listNotifications: ListNotificationsUseCase,
    private val markNotificationsRead: MarkNotificationsReadUseCase,
) {
    @Operation(summary = "List the notifications of a customer, newest first, 20 per page")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "The page, with the number of unread notifications of the customer and the cursor of the next page",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = NotificationPageDTO::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "/problems/validation-error: customerId missing or not a UUID, or a cursor that was not issued by this API",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @GetMapping
    fun list(
        @Parameter(description = "Customer whose notifications are listed (UUID)", required = true)
        @RequestParam customerId: UUID,
        @Parameter(description = "`nextCursor` of the previous page; absent for the first page", required = false)
        @RequestParam(required = false) cursor: String?,
    ): NotificationPageDTO {
        val decoded =
            cursor?.let {
                NotificationCursorCodec.decode(it) ?: throw InvalidRequestException(listOf(InvalidField("cursor", "is invalid")))
            }
        return NotificationPageDTO.from(listNotifications.execute(customerId, decoded))
    }

    @Operation(summary = "Mark as read the notifications of a customer up to one of them, inclusive; idempotent")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "How many notifications changed; zero when they were already read",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = MarkedNotificationsDTO::class))],
        ),
        ApiResponse(
            responseCode = "400",
            description = "/problems/validation-error: customerId or upTo missing or not a UUID, or malformed JSON",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
        ApiResponse(
            responseCode = "404",
            description = "/problems/notification-not-found: upTo does not exist or belongs to another customer; nothing is marked",
            content = [Content(mediaType = ApiDocs.PROBLEM_JSON, schema = Schema(implementation = ProblemDetail::class))],
        ),
    )
    @PostMapping("/read")
    fun markRead(
        @Valid @RequestBody request: MarkNotificationsReadDTO,
    ): MarkedNotificationsDTO =
        MarkedNotificationsDTO(markNotificationsRead.execute(checkNotNull(request.customerId), checkNotNull(request.upTo)))
}
