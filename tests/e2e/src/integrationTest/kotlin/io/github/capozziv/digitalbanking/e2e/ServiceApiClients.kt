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
import java.util.concurrent.ConcurrentHashMap

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

/** Sends the request with `Authorization: Bearer [token]`, or without the header when [token] is null. */
private fun send(
    method: String,
    url: String,
    token: String?,
    body: String? = null,
    headers: Map<String, String> = emptyMap(),
): HttpResult {
    val request =
        HttpRequest
            .newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .method(method, body?.let(HttpRequest.BodyPublishers::ofString) ?: HttpRequest.BodyPublishers.noBody())
    if (body != null) request.header("Content-Type", "application/json")
    if (token != null) request.header("Authorization", "Bearer $token")
    headers.forEach(request::header)
    val response = http.send(request.build(), HttpResponse.BodyHandlers.ofString())
    return HttpResult(response.statusCode(), response.body())
}

/**
 * Talks to `accounts` directly (not through the proxy), for setup and for checking balances and ledger entries. An
 * account is opened by its customer (registered in `customer` first, as the spec asks); the setup credits, debits and
 * reads go with an admin token, and the entry lookup with a machine token, as the saga makes it. It remembers the owner
 * of every account it opened ([ownerOf]), so a transfer goes with the token of the customer of its source.
 */
class AccountsApiClient(
    private val baseUrl: String,
    private val customers: CustomerApiClient,
    private val tokens: TokenIssuer,
) {
    private val owners = ConcurrentHashMap<UUID, UUID>()

    /** Registers [customerId] in `customer` (idempotent) and opens an account for them with their own token. */
    fun openAccount(customerId: UUID = UUID.randomUUID()): UUID {
        customers.register(customerId)
        val result =
            send("POST", "$baseUrl/accounts", tokens.customer(customerId), headers = mapOf("Idempotency-Key" to newKey()))
        check(result.status == 201) { "open account: ${result.status} ${result.body}" }
        val account = UUID.fromString(result.json().get("id").asString())
        owners[account] = customerId
        return account
    }

    /** The customer an account was opened for by [openAccount], or null for one it did not open. */
    fun ownerOf(account: UUID): UUID? = owners[account]

    fun changeType(
        account: UUID,
        type: String,
    ) {
        val result = send("PUT", "$baseUrl/accounts/$account/type", tokens.admin(), """{"type":"$type"}""")
        check(result.status == 200) { "change type: ${result.status} ${result.body}" }
    }

    fun credit(
        account: UUID,
        amount: Long,
    ) {
        val result =
            send(
                "POST",
                "$baseUrl/accounts/$account/credits",
                tokens.admin(),
                """{"amount":$amount,"description":"e2e setup"}""",
                mapOf("Idempotency-Key" to newKey()),
            )
        check(result.status == 201) { "credit: ${result.status} ${result.body}" }
    }

    fun debit(
        account: UUID,
        amount: Long,
    ) {
        val result =
            send(
                "POST",
                "$baseUrl/accounts/$account/debits",
                tokens.admin(),
                """{"amount":$amount,"description":"e2e setup"}""",
                mapOf("Idempotency-Key" to newKey()),
            )
        check(result.status == 201) { "debit: ${result.status} ${result.body}" }
    }

    fun account(account: UUID): JsonNode {
        val result = send("GET", "$baseUrl/accounts/$account", tokens.admin())
        check(result.status == 200) { "get account: ${result.status} ${result.body}" }
        return result.json()
    }

    fun balance(account: UUID): Long = account(account).get("balance").asLong()

    /** `200` when the ledger has an entry with [idempotencyKey], `404` when not. */
    fun entryStatus(
        account: UUID,
        idempotencyKey: String,
    ): Int =
        send(
            "GET",
            "$baseUrl/accounts/$account/entries?idempotencyKey=${URLEncoder.encode(idempotencyKey, Charsets.UTF_8)}",
            tokens.service(),
        ).status

    private fun newKey() = UUID.randomUUID().toString()
}

/**
 * Talks to `transfers`. A transfer goes with the token of the customer that owns the source ([ownerOf]; an admin token
 * for a source of unknown owner); the reads go with an admin token.
 */
class TransfersApiClient(
    private val baseUrl: String,
    private val tokens: TokenIssuer,
    private val ownerOf: (UUID) -> UUID?,
) {
    fun requestTransfer(
        source: UUID,
        destination: UUID,
        amount: Long,
        idempotencyKey: String = UUID.randomUUID().toString(),
        token: String = ownerOf(source)?.let(tokens::customer) ?: tokens.admin(),
    ): HttpResult =
        send(
            "POST",
            "$baseUrl/transfers",
            token,
            """{"sourceAccountId":"$source","destinationAccountId":"$destination","amount":$amount,"description":"e2e"}""",
            mapOf("Idempotency-Key" to idempotencyKey, "X-Correlation-Id" to correlationIdFor(idempotencyKey)),
        )

    fun status(id: String): String {
        val result = send("GET", "$baseUrl/transfers/$id", tokens.admin())
        check(result.status == 200) { "get transfer: ${result.status} ${result.body}" }
        return result.json().get("status").asString()
    }

    /** `failureReason` of the transfer, or null when it has none. */
    fun failureReason(id: String): String? {
        val result = send("GET", "$baseUrl/transfers/$id", tokens.admin())
        check(result.status == 200) { "get transfer: ${result.status} ${result.body}" }
        return result
            .json()
            .get("failureReason")
            ?.takeUnless { it.isNull }
            ?.asString()
    }

    /** The daily limit of [account] today: the JSON of `GET /accounts/{id}/daily-limit`. */
    fun dailyLimit(account: UUID): JsonNode {
        val result = send("GET", "$baseUrl/accounts/$account/daily-limit", tokens.admin())
        check(result.status == 200) { "get daily limit: ${result.status} ${result.body}" }
        return result.json()
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

/** Talks to `notification` as the customer: the token says whose notifications are listed. */
class NotificationApiClient(
    private val baseUrl: String,
    private val tokens: TokenIssuer,
) {
    /** Every notification of [customerId], with their own token, following the cursor through all pages, most recent first. */
    fun all(customerId: UUID): List<JsonNode> {
        val items = mutableListOf<JsonNode>()
        var cursor: String? = null
        do {
            val query = cursor?.let { "?cursor=${URLEncoder.encode(it, Charsets.UTF_8)}" } ?: ""
            val result = send("GET", "$baseUrl/notifications$query", tokens.customer(customerId))
            check(result.status == 200) { "list notifications: ${result.status} ${result.body}" }
            val page = result.json()
            val pageItems = page.get("items")
            (0 until pageItems.size()).forEach { items.add(pageItems.get(it)) }
            cursor = page.get("nextCursor")?.takeUnless { it.isNull }?.asString()
        } while (cursor != null)
        return items
    }

    /**
     * Polls until the notifications of [customerId] are exactly [kinds] (in any order, duplicates counted) and returns
     * them; fails with what was seen when the time runs out.
     */
    fun awaitKinds(
        customerId: UUID,
        kinds: List<String>,
        timeout: Duration,
    ): List<JsonNode> {
        val deadline = Instant.now().plus(timeout)
        while (true) {
            val items = all(customerId)
            val seen = items.map { it.get("kind").asString() }
            if (seen.sorted() == kinds.sorted()) return items
            check(Instant.now().isBefore(deadline)) { "notifications of customer $customerId: expected $kinds, got $seen after $timeout" }
            Thread.sleep(POLL.toMillis())
        }
    }

    private companion object {
        val POLL: Duration = Duration.ofMillis(200)
    }
}

/** Talks to `statement`, with an admin token unless a call names another one. */
class StatementApiClient(
    private val baseUrl: String,
    private val tokens: TokenIssuer,
) {
    /** HTTP status of the first page of the statement of [account], read with [token] (an admin token by default). */
    fun status(
        account: UUID,
        token: String = tokens.admin(),
    ): Int = send("GET", "$baseUrl/accounts/$account/statement", token).status

    /**
     * Entries of the first page with the default limit and no cursor, the only read the cache serves. Fails unless the
     * status is 200.
     */
    fun firstPage(account: UUID): List<JsonNode> {
        val result = send("GET", "$baseUrl/accounts/$account/statement", tokens.admin())
        check(result.status == 200) { "get first page: ${result.status} ${result.body}" }
        val entries = result.json().get("entries")
        return (0 until entries.size()).map { entries.get(it) }
    }

    /**
     * Polls the cached read path (first page, default limit) until it has [count] entries; fails with what was seen when
     * the time runs out. A 404 counts as no entries. One GET per lap.
     */
    fun awaitFirstPage(
        account: UUID,
        count: Int,
        timeout: Duration,
    ): List<JsonNode> {
        val deadline = Instant.now().plus(timeout)
        while (true) {
            val result = send("GET", "$baseUrl/accounts/$account/statement", tokens.admin())
            check(result.status == 200 || result.status == 404) { "get first page: ${result.status} ${result.body}" }
            val entries =
                if (result.status == 200) result.json().get("entries").let { e -> (0 until e.size()).map { e.get(it) } } else emptyList()
            if (entries.size == count) return entries
            check(Instant.now().isBefore(deadline)) {
                "first page of $account: expected $count entries, got ${entries.size} (last status ${result.status}) after $timeout"
            }
            Thread.sleep(POLL.toMillis())
        }
    }

    /** Every entry of the statement of [account], following the cursor through all pages, newest first. */
    fun all(
        account: UUID,
        limit: Int = 100,
    ): List<JsonNode> = checkNotNull(allOrNull(account, limit)) { "get statement of $account: 404" }

    /** Like [all], but null when the statement does not know the account yet (404). Never touches the cache. */
    private fun allOrNull(
        account: UUID,
        limit: Int,
    ): List<JsonNode>? {
        val entries = mutableListOf<JsonNode>()
        var cursor: String? = null
        do {
            val query = "limit=$limit" + (cursor?.let { "&cursor=${URLEncoder.encode(it, Charsets.UTF_8)}" } ?: "")
            val result = send("GET", "$baseUrl/accounts/$account/statement?$query", tokens.admin())
            if (result.status == 404) return null
            check(result.status == 200) { "get statement: ${result.status} ${result.body}" }
            val page = result.json()
            val pageEntries = page.get("entries")
            (0 until pageEntries.size()).forEach { entries.add(pageEntries.get(it)) }
            cursor = page.get("nextCursor")?.takeUnless { it.isNull }?.asString()
        } while (cursor != null)
        return entries
    }

    /**
     * Polls until the statement of [account] has [count] entries and returns them; fails with what was seen when the
     * time runs out. An account the statement does not know yet (404) counts as no entries. One GET per lap, with
     * `limit=100`: it bypasses the first-page cache, so polling cannot pin a stale page in it.
     */
    fun awaitEntries(
        account: UUID,
        count: Int,
        timeout: Duration,
    ): List<JsonNode> {
        val deadline = Instant.now().plus(timeout)
        while (true) {
            val entries = allOrNull(account, 100) ?: emptyList()
            if (entries.size == count) return entries
            check(Instant.now().isBefore(deadline)) {
                "statement of $account: expected $count entries, got ${entries.size} after $timeout"
            }
            Thread.sleep(POLL.toMillis())
        }
    }

    private companion object {
        val POLL: Duration = Duration.ofMillis(200)
    }
}

/** Talks to `customer`: a customer registers with their own token, an admin changes the status. */
class CustomerApiClient(
    private val baseUrl: String,
    private val tokens: TokenIssuer,
) {
    /** Registers [customerId] with fictitious data; `201` the first time, `200` when already registered. */
    fun register(customerId: UUID) {
        val result =
            send(
                "POST",
                "$baseUrl/customers",
                tokens.customer(customerId),
                """{"name":"E2E Customer","email":"e2e-$customerId@example.test"}""",
            )
        check(result.status == 201 || result.status == 200) { "register customer: ${result.status} ${result.body}" }
    }

    /** `ACTIVE` or `BLOCKED`, set by an admin. */
    fun changeStatus(
        customerId: UUID,
        status: String,
    ) {
        val result = send("PUT", "$baseUrl/admin/customers/$customerId/status", tokens.admin(), """{"status":"$status"}""")
        check(result.status == 200) { "change customer status: ${result.status} ${result.body}" }
    }
}
