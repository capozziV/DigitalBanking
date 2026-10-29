package io.github.capozziv.digitalbanking.e2e

import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.UUID

private val mapper: JsonMapper = JsonMapper.builder().build()

private val http: HttpClient =
    HttpClient
        .newBuilder()
        .version(HttpClient.Version.HTTP_1_1)
        .connectTimeout(Duration.ofSeconds(2))
        .build()

/** Status and raw body of a response. */
data class HttpResult(
    val status: Int,
    val body: String,
) {
    fun json(): JsonNode = mapper.readTree(body)

    /** `id` of a transfer body, or `transferId` of a Problem Details about a transfer. */
    fun transferId(): String {
        val json = json()
        return (json.get("id") ?: json.get("transferId") ?: error("no transfer id in $status $body")).asString()
    }
}

private fun send(
    method: String,
    url: String,
    body: String? = null,
    headers: Map<String, String> = emptyMap(),
): HttpResult {
    val request =
        HttpRequest
            .newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .method(method, body?.let(HttpRequest.BodyPublishers::ofString) ?: HttpRequest.BodyPublishers.noBody())
    if (body != null) request.header("Content-Type", "application/json")
    headers.forEach(request::header)
    val response = http.send(request.build(), HttpResponse.BodyHandlers.ofString())
    return HttpResult(response.statusCode(), response.body())
}

/** Talks to `accounts` directly (not through the proxy), for setup and for checking balances and ledger entries. */
class AccountsApiClient(
    private val baseUrl: String,
) {
    fun openAccount(): UUID {
        val result =
            send("POST", "$baseUrl/accounts", """{"customerId":"${UUID.randomUUID()}"}""", mapOf("Idempotency-Key" to newKey()))
        check(result.status == 201) { "open account: ${result.status} ${result.body}" }
        return UUID.fromString(result.json().get("id").asString())
    }

    fun credit(
        account: UUID,
        amount: Long,
    ) {
        val result =
            send(
                "POST",
                "$baseUrl/accounts/$account/credits",
                """{"amount":$amount,"description":"e2e setup"}""",
                mapOf("Idempotency-Key" to newKey()),
            )
        check(result.status == 201) { "credit: ${result.status} ${result.body}" }
    }

    fun account(account: UUID): JsonNode {
        val result = send("GET", "$baseUrl/accounts/$account")
        check(result.status == 200) { "get account: ${result.status} ${result.body}" }
        return result.json()
    }

    fun balance(account: UUID): Long = account(account).get("balance").asLong()

    /** `200` when the ledger has an entry with [idempotencyKey], `404` when not. */
    fun entryStatus(
        account: UUID,
        idempotencyKey: String,
    ): Int = send("GET", "$baseUrl/accounts/$account/entries?idempotencyKey=${URLEncoder.encode(idempotencyKey, Charsets.UTF_8)}").status

    private fun newKey() = UUID.randomUUID().toString()
}

class TransfersApiClient(
    private val baseUrl: String,
) {
    fun requestTransfer(
        source: UUID,
        destination: UUID,
        amount: Long,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): HttpResult =
        send(
            "POST",
            "$baseUrl/transfers",
            """{"sourceAccountId":"$source","destinationAccountId":"$destination","amount":$amount,"description":"e2e"}""",
            mapOf("Idempotency-Key" to idempotencyKey, "X-Correlation-Id" to correlationIdFor(idempotencyKey)),
        )

    fun status(id: String): String {
        val result = send("GET", "$baseUrl/transfers/$id")
        check(result.status == 200) { "get transfer: ${result.status} ${result.body}" }
        return result.json().get("status").asString()
    }

    /** Polls until the transfer leaves PENDING; returns the terminal status. */
    fun awaitTerminal(
        id: String,
        timeout: Duration,
    ): String {
        val deadline = Instant.now().plus(timeout)
        while (true) {
            val status = status(id)
            if (status != "PENDING") return status
            check(Instant.now().isBefore(deadline)) { "transfer $id still PENDING after $timeout" }
            Thread.sleep(POLL.toMillis())
        }
    }

    companion object {
        private val POLL: Duration = Duration.ofMillis(200)

        /** The `X-Correlation-Id` [requestTransfer] sends for [idempotencyKey]. */
        fun correlationIdFor(idempotencyKey: String) = "e2e-$idempotencyKey"
    }
}
