# Etapa 5 — cache do extrato: plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** cache-aside em Redis da 1ª página do extrato (`limit` 20, sem cursor), invalidado ao consumir lançamento, com fallback para o DynamoDB quando o Redis falha.

**Architecture:** porta `StatementFirstPageCache` no `application`, usada pelo `GetStatementUseCase` (leitura/escrita) e pelo `RecordStatementEntryUseCase` (invalidação). Implementação `RedisStatementFirstPageCache` em `adapters/cache/` com `StringRedisTemplate`, JSON próprio, TTL com jitter e métricas Micrometer; toda falha do Redis é engolida no adapter. `statement.cache.enabled=false` troca por `NoOpStatementFirstPageCache`.

**Tech Stack:** Kotlin, Spring Boot 4.1.1, Spring Data Redis (Lettuce), Jackson 3 (`tools.jackson`), Micrometer, Testcontainers (`redis:7-alpine` via `GenericContainer`), LocalStack.

**Spec:** [`docs/superpowers/specs/2026-11-12-etapa-5-cache-do-extrato-design.md`](../specs/2026-11-12-etapa-5-cache-do-extrato-design.md)

## Global Constraints
- Código, testes, comentários e logs em inglês; nomes do `CONTEXT.md` (`Statement`, `StatementEntry`).
- `domain` sem Spring nem Redis; a porta fica em `application`, a implementação em `adapters` (ArchUnit).
- Valores em centavos `Long`.
- Chave: `statement:first-page:v1:{accountId}`.
- TTL: 60 s ± 10 s, sorteado a cada `put` (`statement.cache.ttl: 60s`, `statement.cache.ttl-jitter: 10s`).
- Timeout de comando e de conexão: `${STATEMENT_CACHE_TIMEOUT:100ms}` em `spring.data.redis.timeout` e `spring.data.redis.connect-timeout`.
- Redis: `spring.data.redis.host: ${STATEMENT_REDIS_HOST:localhost}`, `spring.data.redis.port: ${STATEMENT_REDIS_PORT:6379}`.
- `management.health.redis.enabled: false`.
- Métricas: `statement.cache.requests{result=hit|miss|error}`, `statement.cache.evictions{result=ok|error}`.
- Falha do Redis: WARN só com a mensagem (sem stack trace) + métrica de erro; nunca propaga.
- Só cacheia `cursor == null && limit == GetStatementUseCase.DEFAULT_LIMIT` (20). `404` nunca é guardado; página vazia de conta conhecida é.
- Testes: classe termina em `Test` (unidade) ou `IntegrationTest` (integração).
- Agentes em worktree não regeneram nem commitam `graphify-out/`.
- Pronto = `./gradlew build` verde (ktlint, unidade, integração, ArchUnit, e2e), com a saída real no relato.

## Rulings deste plano
- `statement.cache.timeout` da spec vira só a variável `STATEMENT_CACHE_TIMEOUT`, aplicada às duas chaves `spring.data.redis.*` — evita propriedade duplicada. Custo se errado: renomear uma variável.
- Sem `libs/testing-redis` agora (YAGNI): o `statement` sobe o Redis com `GenericContainer` na própria base de testes; a lib nasce na etapa 6, quando houver o segundo usuário. Custo se errado: mover ~15 linhas.
- O teste de fallback pausa o contêiner compartilhado e o retoma num `finally` (JUnit roda as classes em série).

## Review Focus
1. Redis fora no meio de uma leitura: o extrato responde `200` pelo DynamoDB em < 1 s (Task 3, teste de fallback).
2. Página guardada em formato antigo ou corrompido: tratada como falta e regravada (Task 2, JSON inválido).
3. Lançamento consumido depois de uma leitura que aqueceu o cache: a leitura seguinte mostra o lançamento (Task 3 e Task 4 e2e).
4. Conta desconhecida lida duas vezes: `404` nas duas, nada guardado (Task 1).
5. Cliente que pede `limit` 20 explícito recebe o mesmo cache que o padrão; `limit` 21 ou cursor nunca tocam o cache (Task 1).

## Ordem e paralelismo
- **Onda 1 (paralelas):** Task 1 (Marina) e Task 4 (Rafael). Arquivos disjuntos.
- **Onda 2:** Task 2 (Marina), depois da Task 1 revisada.
- **Onda 3:** Task 3 (Marina), depois da Task 2 revisada.
- Worktrees nascem de `etapa-5-cache`.

---

### Task 1: porta do cache e casos de uso

**Files:**
- Create: `services/statement/application/src/main/kotlin/io/github/capozziv/digitalbanking/statement/application/port/StatementFirstPageCache.kt`
- Modify: `.../application/usecase/GetStatementUseCase.kt`, `.../application/usecase/RecordStatementEntryUseCase.kt`
- Create: `services/statement/application/src/test/kotlin/io/github/capozziv/digitalbanking/statement/application/fake/InMemoryStatementFirstPageCache.kt`
- Modify tests: `GetStatementUseCaseTest.kt`, `RecordStatementEntryUseCaseTest.kt`
- Modify: `services/statement/bootstrap/src/main/kotlin/.../bootstrap/UseCaseConfiguration.kt` (injeta `StatementFirstPageCache`)
- Create: `services/statement/adapters/src/main/kotlin/io/github/capozziv/digitalbanking/statement/adapters/cache/NoOpStatementFirstPageCache.kt` (`@Component` temporário até a Task 2 trocar por configuração condicional; `get` → `null`, `put`/`evict` sem efeito)

**Interfaces:**
- Produces:
  ```kotlin
  /** Cache of the first statement page (default limit, no cursor). Implementations never throw: a failure reads as a miss. */
  interface StatementFirstPageCache {
      fun get(accountId: UUID): StatementPage?
      fun put(accountId: UUID, page: StatementPage)
      fun evict(accountId: UUID)
  }
  class GetStatementUseCase(repository: StatementRepository, cache: StatementFirstPageCache)
  class RecordStatementEntryUseCase(repository: StatementRepository, cache: StatementFirstPageCache)
  ```
- `InMemoryStatementFirstPageCache`: mapa + contadores públicos `gets`, `puts`, `evictions` (por `accountId`).

- [ ] **Step 1: testes que falham** (em `GetStatementUseCaseTest`, com `InMemoryStatementRepository` + `InMemoryStatementFirstPageCache`):
  - `a cached first page is returned without reading the repository` — página pré-gravada no cache; repositório vazio e conta inexistente; resultado = página do cache.
  - `a miss reads the repository and caches the page` — `cache.puts == 1` e `cache.get(id) == page`.
  - `an empty first page of a known account is cached`.
  - `an unknown account is not cached` — `AccountNotFoundException` duas vezes; `cache.puts == 0`.
  - `a cursor never touches the cache` e `a limit other than the default never touches the cache` (limit 21) — `gets == 0 && puts == 0`.
  - Em `RecordStatementEntryUseCaseTest`: `a recorded entry evicts the first page` e `a duplicate entry evicts the first page` (`evictions == 1` para o `accountId`); `a conflicting entry does not evict` (`evictions == 0`).
- [ ] **Step 2:** `./gradlew :services:statement:application:test` → FAIL (construtor/porta inexistentes).
- [ ] **Step 3:** implementar. No `RecordStatementEntryUseCase`, `evict` depois de decidir `RECORDED`/`DUPLICATE` e antes do `return`; o conflito lança antes do `evict`.
- [ ] **Step 4:** `./gradlew :services:statement:application:test :services:statement:bootstrap:test` → PASS; `./gradlew :services:statement:build` verde.
- [ ] **Step 5:** commit `feat(statement): porta do cache da 1ª página nos casos de uso`.

### Task 2: adapter Redis

**Files:**
- Create em `services/statement/adapters/src/main/kotlin/.../statement/adapters/cache/`: `RedisStatementFirstPageCache.kt`, `CachedStatementPage.kt` (JSON próprio: `entries` com todos os campos de `StatementEntry`, `kind` como texto, `createdAt` ISO-8601; `nextCursor: Long?`), `StatementCacheProperties.kt` (`@ConfigurationProperties("statement.cache")`: `enabled: Boolean = true`, `ttl: Duration = 60s`, `ttlJitter: Duration = 10s`).
- Modify: `NoOpStatementFirstPageCache.kt` (tira o `@Component`).
- Create: `services/statement/bootstrap/src/main/kotlin/.../bootstrap/CacheConfiguration.kt` — Redis quando `statement.cache.enabled` for `true` (padrão, `matchIfMissing = true`), senão NoOp.
- Modify: `services/statement/adapters/build.gradle.kts` e `bootstrap/build.gradle.kts` (`spring-boot-starter-data-redis`), `application.yml` (chaves das Global Constraints).
- Modify: `services/statement/bootstrap/src/integrationTest/.../StatementIntegrationTest.kt` — `GenericContainer("redis:7-alpine")` com porta 6379, iniciado como o LocalStack, `spring.data.redis.host/port` no `@DynamicPropertySource`; expor `redis` e um `StringRedisTemplate` ou cliente para inspeção.
- Create: `services/statement/bootstrap/src/integrationTest/.../cache/RedisStatementFirstPageCacheIntegrationTest.kt`
- Create: `services/statement/adapters/src/test/kotlin/.../adapters/cache/RedisStatementFirstPageCacheTest.kt` (só o sorteio do TTL, se extraído como função pura)

**Interfaces:**
- Consumes: `StatementFirstPageCache` (Task 1).
- Produces: `class RedisStatementFirstPageCache(redis: StringRedisTemplate, mapper: JsonMapper, properties: StatementCacheProperties, meterRegistry: MeterRegistry, random: Random = Random.Default) : StatementFirstPageCache`; `fun nextTtl(): Duration` dentro de `[ttl - ttlJitter, ttl + ttlJitter]`.

- [ ] **Step 1: testes que falham** (`RedisStatementFirstPageCacheIntegrationTest`, contra o Redis do Testcontainers):
  - `a stored page is read back equal` — página com 2 lançamentos (um com `description = null`, saldo negativo) e `nextCursor`; e outra vazia sem cursor.
  - `the key is statement:first-page:v1:{accountId} and its ttl is between 50 and 70 seconds`.
  - `evict removes the key` — e `statement.cache.evictions{result=ok}` += 1.
  - `get counts hit and miss` — `statement.cache.requests{result=miss}` e `{result=hit}`.
  - `an invalid json value is a miss` — `SET` direto com `"{not json"`; `get` → `null`; conta `miss` (não `error`) e não lança.
  - `with redis paused, get returns null within 1 second and counts an error` — pausa via `redis.dockerClient.pauseContainerCmd(redis.containerId).exec()`, retoma no `finally`; `put` e `evict` também não lançam, com `requests{result=error}` e `evictions{result=error}`.
- [ ] **Step 2:** rodar `./gradlew :services:statement:bootstrap:integrationTest --tests '*RedisStatementFirstPageCache*'` → FAIL.
- [ ] **Step 3:** implementar. Toda exceção (`RuntimeException`) do Redis vira `log.warn("statement cache {} failed for account {}: {}", op, accountId, e.message)` + contador de erro; JSON inválido vira `miss`.
- [ ] **Step 4:** teste da Step 1 PASS; `./gradlew :services:statement:build` verde (inclui ArchUnit).
- [ ] **Step 5:** commit `feat(statement): cache da 1ª página no Redis com TTL e fallback`.

### Task 3: integração do cache no serviço

**Files:**
- Create: `services/statement/bootstrap/src/integrationTest/.../cache/StatementCacheIntegrationTest.kt` (web environment, como `StatementApiIntegrationTest`; reaproveita `ApiClient`).
- Modify (se preciso): `StatementConsumptionIntegrationTest.kt` só para não depender de cache frio.

**Interfaces:**
- Consumes: bean `StatementFirstPageCache` (Task 2), filas e tabela da base de testes.

- [ ] **Step 1: testes que falham:**
  - `the second read of the first page is served from the cache` — 1ª leitura `GET /accounts/{id}/statement`; grava um item direto no DynamoDB (sem passar pela fila); 2ª leitura não mostra o item e `requests{result=hit}` += 1.
  - `an entry consumed from the queue evicts the page and the next read shows it` — aquece; manda o evento de lançamento pela `statement-accounts.fifo`; Awaitility (máx. 10 s) até a leitura mostrar o lançamento novo.
  - `with redis paused the statement answers 200 from dynamodb in under 1 second` — pausa, mede o tempo da chamada, confere `200` com os lançamentos e `requests{result=error}` += 1; retoma no `finally`.
  - `an unknown account answers 404 twice and stores nothing` — confere ausência da chave no Redis.
- [ ] **Step 2:** FAIL antes de... (se a Task 2 já cobre a fiação, os testes passam de primeira: rode o mutante — comente o `evict` no `RecordStatementEntryUseCase` — e confirme o 2º teste vermelho; reverta).
- [ ] **Step 3:** ajustes de fiação que os testes pedirem.
- [ ] **Step 4:** `./gradlew build` completo verde (todo o repositório; o `statement` mexe na base de testes compartilhada pelo e2e).
- [ ] **Step 5:** commit `test(statement): cache da 1ª página ponta a ponta no serviço`.

### Task 4: ambiente local, e2e e guia

**Files:**
- Modify: `tests/e2e/src/integrationTest/kotlin/.../e2e/EndToEndEnvironment.kt` — `GenericContainer("redis:7-alpine")` iniciado com os demais e fechado no `close()`; `statementEnvironment()` ganha `STATEMENT_REDIS_HOST` e `STATEMENT_REDIS_PORT`.
- Modify: `tests/e2e/src/integrationTest/kotlin/.../e2e/StatementEndToEndIntegrationTest.kt` — novo teste.
- Modify: `infra/local/.env.example` (`STATEMENT_REDIS_HOST=localhost`, `STATEMENT_REDIS_PORT=6379`, `# STATEMENT_CACHE_TIMEOUT=100ms`).
- Modify: `docs/rodando-localmente.md` — variáveis, `redis-cli --scan --pattern 'statement:first-page:v1:*'`, métricas `statement.cache.requests` e `statement.cache.evictions` em `/actuator/metrics`.
- Check: `.github/workflows/ci.yml` — o job do `statement` e o do e2e não precisam de serviço Redis (Testcontainers sobe); corrigir o comentário "três jars" (ci.yml:280-281, :301) para quatro.

**Interfaces:**
- Consumes: nomes das variáveis (Global Constraints). Antes da Task 2 o `statement` ignora essas variáveis; o teste novo passa igual e vira prova real depois do merge da Task 2.

- [ ] **Step 1: teste** `a read before a transfer does not hide the new debit` — lê o extrato da origem (aquece o cache), faz a transferência, Awaitility com **no máximo 30 s** (abaixo do TTL mínimo de 50 s) até o extrato mostrar o débito.
- [ ] **Step 2:** `./gradlew :tests:e2e:integrationTest` → PASS.
- [ ] **Step 3:** guia e `.env.example`; `infra/local/smoke.sh` e `bru run` das quatro coleções com o compose de pé (o Redis já existe no compose).
- [ ] **Step 4:** commit `test(e2e): Redis do statement e leitura antes da transferência`.

## Revisão final da branch (Beatriz)
Mutantes da spec, cada um tem de deixar teste vermelho:
- sem o `evict` no `RecordStatementEntryUseCase` → Task 3 (invalidação) e Task 4 (e2e, com a Task 2 já no merge);
- `get` relançando a exceção do Redis → fallback da Task 2 e da Task 3;
- `put` também com cursor → `a cursor never touches the cache` (Task 1).
