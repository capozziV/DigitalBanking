package io.github.capozziv.digitalbanking.outbox.relay.web

import io.github.capozziv.digitalbanking.outbox.relay.DiscardOutcome
import io.github.capozziv.digitalbanking.outbox.relay.OutboxStore
import io.github.capozziv.digitalbanking.outbox.relay.web.dto.DiscardOutboxEventDTO
import io.github.capozziv.digitalbanking.outbox.relay.web.dto.StuckOutboxEventDTO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.net.URLEncoder
import java.security.Principal
import java.time.Duration
import java.util.UUID

/**
 * Manual unlock of an aggregate held back by an event the broker keeps refusing (a poison event): an admin lists the
 * stuck events and discards one, so the later events of its aggregate get published. The discarded event is never
 * published; its consumers will not see it. Registered by `OutboxRelayConfigurationSupport` in every service with a
 * relay. Admins only: every route under `/admin/` is a rule of libs/security.
 *
 * Errors are answered here as Problem Details, independent of each service's exception handler.
 */
@RestController
@RequestMapping("/admin/outbox")
@Tag(name = "outbox", description = "Manual unlock of aggregates stuck in the outbox relay")
class OutboxAdminController(
    private val store: OutboxStore,
    private val relayLockKey: Long,
    private val lockTimeout: Duration,
) {
    @Operation(summary = "List the outbox events that failed to publish and hold their aggregate back, oldest first")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "The stuck events, at most $MAX_LISTED",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = ArraySchema(schema = Schema(implementation = StuckOutboxEventDTO::class)),
                ),
            ],
        ),
    )
    @GetMapping("/stuck-events")
    fun stuckEvents(): List<StuckOutboxEventDTO> = store.stuckEvents(MAX_LISTED).map(StuckOutboxEventDTO::from)

    /** Idempotent: discarding an event that is already discarded answers 204 and changes nothing. */
    @Operation(
        summary = "Discard a stuck outbox event, so the later events of its aggregate get published",
        description =
            "The event is never published and its consumers will not see it. Discarding the event that creates its " +
                "aggregate (such as AccountOpened) leaves every later event of that aggregate without a useful consumer: " +
                "notification sends them to its DLQ and statement answers 404 for the account. " +
                "Waits for the relay cycle in progress, up to the configured discard lock timeout. " +
                "Discarding an event that is already discarded answers 204 and changes nothing.",
    )
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "The event is discarded"),
        ApiResponse(
            responseCode = "400",
            description =
                "/problems/validation-error: the event id is not a UUID, or the reason is missing, blank or longer " +
                    "than $MAX_REASON_LENGTH characters",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = Schema(implementation = ProblemDetail::class),
                ),
            ],
        ),
        ApiResponse(
            responseCode = "404",
            description = "/problems/outbox-event-not-found: no outbox event with this id",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = Schema(implementation = ProblemDetail::class),
                ),
            ],
        ),
        ApiResponse(
            responseCode = "409",
            description = "/problems/outbox-event-not-stuck: the event is already published, or never failed to publish",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = Schema(implementation = ProblemDetail::class),
                ),
            ],
        ),
        ApiResponse(
            responseCode = "503",
            description =
                "/problems/outbox-relay-busy: the relay cycle held its lock longer than the discard waits; " +
                    "nothing changed, try again after Retry-After seconds",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = Schema(implementation = ProblemDetail::class),
                ),
            ],
        ),
    )
    @PostMapping("/events/{eventId}/discard")
    fun discard(
        @PathVariable eventId: String,
        @RequestBody request: DiscardOutboxEventDTO,
        principal: Principal,
    ): ResponseEntity<Any> {
        val instance = "/admin/outbox/events/${URLEncoder.encode(eventId, Charsets.UTF_8)}/discard"
        val id =
            runCatching { UUID.fromString(eventId) }.getOrNull()
                ?: return invalid(instance, "eventId", "eventId: must be a UUID")
        val reason = request.reason?.trim()
        if (reason.isNullOrEmpty() || reason.length > MAX_REASON_LENGTH) return invalid(instance, "reason", invalidReason())
        return when (store.discard(id, principal.name, reason, relayLockKey, lockTimeout)) {
            DiscardOutcome.DISCARDED -> {
                log.warn("Outbox event discarded by hand (eventId={}, discardedBy={}, reason={})", id, principal.name, reason)
                ResponseEntity.noContent().build()
            }

            DiscardOutcome.ALREADY_DISCARDED -> {
                ResponseEntity.noContent().build()
            }

            DiscardOutcome.NOT_FOUND -> {
                problem(
                    instance,
                    HttpStatus.NOT_FOUND,
                    "outbox-event-not-found",
                    "Outbox event not found",
                    "No outbox event with this id",
                )
            }

            DiscardOutcome.RELAY_BUSY -> {
                log.warn("Outbox event not discarded: the relay held its lock past {} (eventId={})", lockTimeout, id)
                problem(
                    instance,
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "outbox-relay-busy",
                    "Outbox relay busy",
                    "The relay cycle held its lock longer than the discard waits; nothing changed, try again",
                    retryAfterSeconds = RETRY_AFTER_SECONDS,
                )
            }

            DiscardOutcome.ALREADY_PUBLISHED, DiscardOutcome.NOT_FAILED -> {
                problem(
                    instance,
                    HttpStatus.CONFLICT,
                    "outbox-event-not-stuck",
                    "Outbox event not stuck",
                    "Only an unpublished event that failed to publish can be discarded",
                )
            }
        }
    }

    private fun invalidReason() = "reason: must be present, not blank, and at most $MAX_REASON_LENGTH characters"

    private fun invalid(
        instance: String,
        field: String,
        message: String,
    ): ResponseEntity<Any> =
        problem(instance, HttpStatus.BAD_REQUEST, "validation-error", "Validation error", "The request has invalid fields")
            .also { (it.body as ProblemDetail).setProperty("errors", listOf(mapOf("field" to field, "message" to message))) }

    private fun problem(
        instance: String,
        status: HttpStatus,
        slug: String,
        title: String,
        detail: String,
        retryAfterSeconds: Int? = null,
    ): ResponseEntity<Any> {
        val problem =
            ProblemDetail.forStatusAndDetail(status, detail).apply {
                type = URI.create("/problems/$slug")
                this.title = title
                this.instance = URI.create(instance)
            }
        return ResponseEntity
            .status(status)
            .apply { if (retryAfterSeconds != null) header(HttpHeaders.RETRY_AFTER, retryAfterSeconds.toString()) }
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem)
    }

    private companion object {
        val log = LoggerFactory.getLogger(OutboxAdminController::class.java)
        const val MAX_LISTED = 100
        const val MAX_REASON_LENGTH = 500

        /** The relay frees its lock between cycles (every poll interval), so a retry soon after usually gets it. */
        const val RETRY_AFTER_SECONDS = 5
    }
}
