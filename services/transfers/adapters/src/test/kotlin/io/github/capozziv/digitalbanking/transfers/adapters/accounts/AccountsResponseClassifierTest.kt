package io.github.capozziv.digitalbanking.transfers.adapters.accounts

import io.github.capozziv.digitalbanking.transfers.domain.StepOutcome
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class AccountsResponseClassifierTest {
    private val registry = SimpleMeterRegistry()
    private val classifier = AccountsResponseClassifier(registry)

    @Test
    fun `every rejected service token is counted by status, and nothing else is`() {
        classifier.classify(401, "unauthorized")
        classifier.classify(401, null)
        classifier.classify(403, "forbidden")
        classifier.classify(404, "entry-not-found")
        classifier.classify(503, null)
        classifier.classify(null, null)

        assertThat(rejected("401")).isEqualTo(2.0)
        assertThat(rejected("403")).isEqualTo(1.0)
        assertThat(registry.find(AccountsResponseClassifier.SERVICE_TOKEN_REJECTED).counters().sumOf { it.count() }).isEqualTo(3.0)
    }

    private fun rejected(status: String) =
        registry
            .find(AccountsResponseClassifier.SERVICE_TOKEN_REJECTED)
            .tag("status", status)
            .counter()
            ?.count()

    @ParameterizedTest
    @ValueSource(ints = [200, 201, 204])
    fun `2xx is success`(status: Int) {
        assertThat(classifier.classify(status, null)).isEqualTo(StepOutcome.Success)
    }

    @Test
    fun `422 insufficient-available-balance is a refusal with the balance reason`() {
        assertThat(classifier.classify(422, "insufficient-available-balance"))
            .isEqualTo(StepOutcome.Refused(StepOutcome.INSUFFICIENT_AVAILABLE_BALANCE))
    }

    @Test
    fun `404 entry-not-found is a refusal with the entry-not-found reason`() {
        assertThat(classifier.classify(404, "entry-not-found"))
            .isEqualTo(StepOutcome.Refused(StepOutcome.ENTRY_NOT_FOUND))
    }

    @Test
    fun `404 account-not-found is a definitive refusal carrying the problem type`() {
        assertThat(classifier.classify(404, "account-not-found")).isEqualTo(StepOutcome.Refused("account-not-found"))
    }

    @Test
    fun `422 idempotency-key-reused is a definitive refusal carrying the problem type`() {
        assertThat(classifier.classify(422, "idempotency-key-reused"))
            .isEqualTo(StepOutcome.Refused("idempotency-key-reused"))
    }

    @Test
    fun `422 account-blocked and account-closed are definitive refusals carrying the problem type`() {
        assertThat(classifier.classify(422, "account-blocked")).isEqualTo(StepOutcome.Refused("account-blocked"))
        assertThat(classifier.classify(422, "account-closed")).isEqualTo(StepOutcome.Refused("account-closed"))
    }

    @Test
    fun `400 validation-error is a definitive refusal carrying the problem type`() {
        assertThat(classifier.classify(400, "validation-error")).isEqualTo(StepOutcome.Refused("validation-error"))
    }

    @Test
    fun `4xx without a readable problem type is a refusal named after the status`() {
        assertThat(classifier.classify(404, null)).isEqualTo(StepOutcome.Refused("http-404"))
    }

    @Test
    fun `problem type slug ignores about-blank and keeps the last path segment`() {
        assertThat(AccountsResponseClassifier.problemSlug("about:blank")).isNull()
        assertThat(AccountsResponseClassifier.problemSlug("/problems/account-busy/")).isEqualTo("account-busy")
        assertThat(AccountsResponseClassifier.problemSlug(null)).isNull()
        assertThat(AccountsResponseClassifier.problemSlug("")).isNull()
    }

    @ParameterizedTest
    @ValueSource(ints = [408, 429])
    fun `request timeout and too many requests are transient`(status: Int) {
        assertThat(classifier.classify(status, null)).isEqualTo(StepOutcome.TransientFailure)
    }

    @ParameterizedTest
    @ValueSource(ints = [401, 403])
    fun `a rejected service token (401 or 403) is transient, never a refusal`(status: Int) {
        assertThat(classifier.classify(status, null)).isEqualTo(StepOutcome.TransientFailure)
        assertThat(classifier.classify(status, "unauthorized")).isEqualTo(StepOutcome.TransientFailure)
        assertThat(classifier.classify(status, "forbidden")).isEqualTo(StepOutcome.TransientFailure)
    }

    @ParameterizedTest
    @ValueSource(ints = [500, 502, 503, 504])
    fun `5xx is transient, whatever the problem type`(status: Int) {
        assertThat(classifier.classify(status, null)).isEqualTo(StepOutcome.TransientFailure)
        assertThat(classifier.classify(status, "account-busy")).isEqualTo(StepOutcome.TransientFailure)
    }

    @Test
    fun `no response (timeout, refused or cut connection) is transient`() {
        assertThat(classifier.classify(null, null)).isEqualTo(StepOutcome.TransientFailure)
    }

    @ParameterizedTest
    @ValueSource(ints = [100, 301, 302])
    fun `informational and redirect statuses are transient`(status: Int) {
        assertThat(classifier.classify(status, null)).isEqualTo(StepOutcome.TransientFailure)
    }
}
