package io.github.capozziv.digitalbanking.accounts.bootstrap.web

import org.springframework.http.HttpHeaders
import java.time.Duration

/**
 * Sends [request] and, while the answer is `503 account-busy` (lock timeout on the account), waits the
 * `Retry-After` seconds and sends it again, up to [maxAttempts] attempts in total. The caller reuses the
 * same Idempotency-Key in [request], as a real client must. Returns the last response.
 */
fun retryWhileAccountBusy(
    maxAttempts: Int = 3,
    sleep: (Duration) -> Unit = { Thread.sleep(it) },
    request: () -> ApiResponse,
): ApiResponse {
    var response = request()
    repeat(maxAttempts - 1) {
        if (!response.isAccountBusy()) return response
        sleep(Duration.ofSeconds(response.header(HttpHeaders.RETRY_AFTER)?.toLongOrNull() ?: 1))
        response = request()
    }
    return response
}

private fun ApiResponse.isAccountBusy() = status == 503 && problemType() == "/problems/account-busy"
