package io.github.capozziv.digitalbanking.transfers.adapters.accounts

import io.github.capozziv.digitalbanking.transfers.domain.StepOutcome
import org.slf4j.LoggerFactory

/**
 * Turns a response of `accounts` into a [StepOutcome]. A wrong answer here makes the saga compensate when it
 * should not, or the opposite, so anything that is not clearly a success or a definitive refusal is transient.
 */
class AccountsResponseClassifier {
    /**
     * @param status HTTP status, or null when no response arrived (timeout, refused or cut connection).
     * @param problemType last segment of the Problem Details `type` (`/problems/<slug>`), when the body had one.
     */
    fun classify(
        status: Int?,
        problemType: String?,
    ): StepOutcome =
        when {
            status == null -> StepOutcome.TransientFailure

            status in HTTP_OK_RANGE -> StepOutcome.Success

            // Throttling or a gateway giving up says nothing about the request itself: try again.
            status == HTTP_REQUEST_TIMEOUT || status == HTTP_TOO_MANY_REQUESTS -> StepOutcome.TransientFailure

            // A rejected service token (expired, clock skew, new key or issuer, wrong route rule) is a configuration or
            // infrastructure failure, not a business decision: the saga retries instead of failing or compensating.
            status == HTTP_UNAUTHORIZED || status == HTTP_FORBIDDEN -> rejectedServiceToken(status, problemType)

            status in HTTP_CLIENT_ERROR_RANGE -> StepOutcome.Refused(refusalReason(status, problemType))

            else -> StepOutcome.TransientFailure
        }

    private fun rejectedServiceToken(
        status: Int,
        problemType: String?,
    ): StepOutcome {
        log.error("accounts rejected the service token: status {}, problem type {}", status, problemType)
        return StepOutcome.TransientFailure
    }

    private fun refusalReason(
        status: Int,
        problemType: String?,
    ): String =
        when {
            status == HTTP_UNPROCESSABLE && problemType == StepOutcome.INSUFFICIENT_AVAILABLE_BALANCE -> {
                StepOutcome.INSUFFICIENT_AVAILABLE_BALANCE
            }

            status == HTTP_NOT_FOUND && problemType == StepOutcome.ENTRY_NOT_FOUND -> {
                StepOutcome.ENTRY_NOT_FOUND
            }

            else -> {
                problemType ?: "http-$status"
            }
        }

    companion object {
        /**
         * Last path segment of a Problem Details `type`, or null when it carries no information: absent, blank or
         * `about:blank` (what Spring answers for routes it does not map).
         */
        fun problemSlug(type: String?): String? =
            type
                ?.takeUnless { it == ABOUT_BLANK }
                ?.trimEnd('/')
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() }

        private val log = LoggerFactory.getLogger(AccountsResponseClassifier::class.java)
        private const val ABOUT_BLANK = "about:blank"
        private val HTTP_OK_RANGE = 200..299
        private val HTTP_CLIENT_ERROR_RANGE = 400..499
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_NOT_FOUND = 404
        private const val HTTP_REQUEST_TIMEOUT = 408
        private const val HTTP_UNPROCESSABLE = 422
        private const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
