# Etapa 10 — observabilidade: plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Uma transferência feita no app aparece no painel "Transferências" do Grafana local e vira um trace só (BFF → saga → outbox/SNS/SQS → `notification`/`statement`), achado pelo `transfer.id`; a mesma instrumentação fica pronta para o X-Ray e o CloudWatch na AWS, validada sem aplicar.

**Architecture:** Os serviços e o BFF exportam traces, métricas e logs por OTLP para um coletor OpenTelemetry. Localmente, o coletor repassa ao Tempo, ao Prometheus e ao Loki, lidos pelo Grafana; na AWS, o ADOT Collector repassa ao X-Ray e ao CloudWatch. A continuidade da saga e da mensageria vem de um `traceparent` W3C gravado em `transfer.trace_parent` e `outbox.trace_parent` e levado num atributo de mensagem SNS/SQS.

**Tech Stack:** Spring Boot 4.1.1 (Micrometer Tracing + ponte OpenTelemetry, exportação OTLP), Kotlin 2.3, Spring Cloud AWS 4.2.1, NestJS 12 + SDK OpenTelemetry para Node, pino; OpenTelemetry Collector, Grafana, Tempo, Prometheus e Loki no docker-compose; ADOT Collector, X-Ray e CloudWatch em Helm e Terraform.

**Spec:** [docs/superpowers/specs/2026-11-23-etapa-10-observabilidade-design.md](../specs/2026-11-23-etapa-10-observabilidade-design.md) (aprovada em 2026-11-23). Quem executa lê a spec e este plano.

## Global Constraints
- Código, testes, nomes de span, atributos, métricas e logs em inglês, com os termos do `CONTEXT.md` (`Transfer`, `TransferSaga`, `TransferStep`, `TransferStatus`, `LedgerEntry`, `Notification`, `StatementEntry`).
- O `domain` não depende de OpenTelemetry, de Micrometer nem de Spring (ArchUnit). Spans e métricas ficam atrás de portas no `application`, implementadas em `adapters`.
- Propagação W3C Trace Context (`traceparent`) nos dois ambientes; amostragem de 100%.
- Atributos de span permitidos: `transfer.id`, `account.id`, `event.id`, `event.type`, `correlation.id`, `transfer.step`, `transfer.status`. Nada de nome, telefone, e-mail, token, cookie, header `Authorization` ou query string. IDs nunca como rótulo de métrica.
- Métricas novas: `transfers.finished` (contador; rótulos `status` ∈ {`COMPLETED`, `FAILED`, `COMPENSATED`} e `reason`, que só aparece em `FAILED`/`COMPENSATED`, num conjunto fechado mais `other`) e `transfers.saga.duration` (timer do `created_at` ao status final; rótulo `status`; histograma publicado). No Prometheus: `transfers_finished_total` e `transfers_saga_duration_seconds_bucket`.
- Coletor ou backend fora não derruba nem atrasa requisição: exportação assíncrona em lote, fila limitada, descarte.
- O correlation ID continua como está (filtro, MDC, outbox, atributo `correlationId`) e vira também o atributo de span `correlation.id`.
- Logs em JSON no stdout com `traceId`, `spanId` e `correlationId`, e também exportados por OTLP.
- Endpoint OTLP por variável de ambiente: `OTEL_EXPORTER_OTLP_ENDPOINT` (padrão `http://127.0.0.1:4318`, OTLP/HTTP).
- Healthchecks e chamadas dentro de contêineres usam `127.0.0.1`.
- Agentes em worktree não regeneram nem commitam `graphify-out/`.
- Commits em português, com o autor `capozziV <vitorcapozzi4@gmail.com>`, sem trailers.

## Review Focus
1. **Coletor fora na subida ou no meio:** o serviço sobe, a transferência conclui sem atraso perceptível e o log não ganha uma linha de erro por requisição. Teste na Task 2.
2. **Mensagem ou linha antiga, sem `traceparent` ou com valor malformado:** o consumidor e o relay seguem com um trace novo, sem exceção, e a mensagem é processada. Testes na Task 3.
3. **Transferência criada antes da migration (`trace_parent` nulo), retomada pelo agendador:** a saga conclui num trace novo, sem erro. Teste na Task 4.
4. **Dado pessoal vazando pelo OTLP:** spans HTTP sem query string nem headers sensíveis; logs do BFF exportados com a mesma redação do pino. Testes nas Tasks 2 e 5.
5. **Motivo de recusa fora do conjunto conhecido:** vira `reason="other"`, sem criar série nova. Teste na Task 4.

## Mapa de arquivos e paralelismo
- Branch da etapa: `etapa-10` (já tem a spec). Os worktrees nascem dela.
- **Rodada 1, em paralelo:** Task 1 (Rafael, compose e Grafana), Task 2 (Marina, base dos serviços), Task 5 (Lucas, BFF) e Task 6 (Rafael, AWS; worktree separado do da Task 1).
- **Rodada 2, depois da Task 2 revisada e juntada:** Task 3 (Marina, outbox e consumo) e Task 4 (Marina, saga e métricas), em paralelo.
- **Rodada 3, depois das Tasks 1–5 juntadas:** Task 7 (Rafael, smoke e CI) e Task 8 (Clara, documentação).
- **Arquivos compartilhados, combinados antes do despacho:**
  - `infra/local/docker-compose.yml` é só da Task 1, que já põe `OTEL_EXPORTER_OTLP_ENDPOINT` e `OTEL_SERVICE_NAME` nos cinco serviços e no BFF.
  - `services/*/bootstrap/src/main/resources/application.yml` é só da Task 2.
  - Migrations do `transfers`: `V10__outbox_trace_parent.sql` na Task 3 e `V11__transfer_trace_parent.sql` na Task 4.
  - `libs/observabilidade/build.gradle.kts` é da Task 2. A Task 3 só acrescenta linhas no bloco de SQS.
  - `deploy/helm/values/*.yaml` e `infra/terraform/**` são só da Task 6.

---

### Task 1: Stack local de observabilidade (Rafael, `infra`)

**Files:**
- Modify: `infra/local/docker-compose.yml`, com os serviços `otel-collector`, `tempo`, `prometheus`, `loki` e `grafana` no profile `observability`, volumes `tempo-data`, `prometheus-data`, `loki-data` e `grafana-data`, e as variáveis OTEL nos cinco serviços e no BFF
- Modify: `infra/local/.env.example`, com as versões das imagens fixadas, como as outras
- Create: `infra/local/otel-collector/config.yaml`
- Create: `infra/local/tempo/tempo.yaml`, `infra/local/prometheus/prometheus.yml`, `infra/local/loki/loki.yaml`
- Create: `infra/local/grafana/provisioning/datasources/datasources.yaml`, `infra/local/grafana/provisioning/dashboards/dashboards.yaml`, `infra/local/grafana/dashboards/transfers.json`
- Create: `infra/local/observability-check.sh`

**Interfaces:**
- Produces:
  - coletor em `otel-collector:4318` (HTTP) e `:4317` (gRPC); nos serviços do compose, `OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector:4318`;
  - `OTEL_SERVICE_NAME` com os valores `accounts`, `transfers`, `notification`, `statement`, `customer` e `bff`;
  - Grafana em `http://127.0.0.1:3000`, com acesso anônimo de leitura;
  - APIs no host: Tempo em `127.0.0.1:3200`, Prometheus em `127.0.0.1:9090`, Loki em `127.0.0.1:3100`, todas presas a `127.0.0.1`;
  - `observability-check.sh <transfer-id>`, que sai com 0 quando o Tempo devolve um trace com spans dos serviços `bff`, `transfers`, `accounts`, `notification` e `statement` e o Prometheus tem `transfers_finished_total`. Usado pela Task 7.
- Consumes: os nomes de métrica e os atributos do Global Constraints.

- [ ] **Step 1:** Coletor com receptor OTLP (HTTP e gRPC). Pipelines: traces → Tempo (OTLP); métricas → Prometheus (`otlphttp` para o receptor OTLP do Prometheus, ligado com `--web.enable-otlp-receiver`); logs → Loki (`otlphttp` no endpoint OTLP nativo do Loki). Processadores `memory_limiter` e `batch`. Healthcheck pela extensão `health_check`, em `127.0.0.1`.
- [ ] **Step 2:** Tempo com armazenamento local no volume e `metrics_generator` desligado. Loki em modo monolítico, com volume. O Prometheus guarda 7 dias.
- [ ] **Step 3:** Grafana com as fontes provisionadas. Na do Tempo, `tracesToLogsV2` liga o span ao Loki por `traceId`, com `filterByTraceID: true`. Na do Loki, um campo derivado `traceId` leva de volta ao Tempo.
- [ ] **Step 4:** Painel `transfers.json` (uid `transfers`, título "Transferências"):
  - "Por minuto": `sum by (status) (rate(transfers_finished_total[1m])) * 60`;
  - "Duração da saga": p50 e p95 com `histogram_quantile` sobre `transfers_saga_duration_seconds_bucket`;
  - "Recusas por motivo": `sum by (reason) (increase(transfers_finished_total{status!="COMPLETED"}[$__range]))`;
  - "DLQs": `notification_dlq_messages` e `statement_dlq_messages`;
  - "Outbox pendente": `outbox_pending`;
  - "Últimas transferências": tabela TraceQL `{ resource.service.name = "transfers" && span.transfer.id != "" }`, com link "ver saga" para o trace;
  - variável de texto `transfer_id` e um painel de trace com `{ span.transfer.id = "$transfer_id" }`.
- [ ] **Step 5:** Escrever `observability-check.sh`, que usa `curl` na API de busca do Tempo (`/api/search?q=...` e depois `/api/traces/<id>`) e na do Prometheus (`/api/v1/query`). Repete por até 60 s, porque os lotes demoram.
- [ ] **Step 6: Verificar.** Rode `docker compose --env-file infra/local/.env.example -f infra/local/docker-compose.yml -p obs-check --profile observability up -d --wait`; esperado: os cinco contêineres `healthy`. Envie um span de teste: `curl -s -XPOST 127.0.0.1:4318/v1/traces -H 'Content-Type: application/json' -d @<span com o atributo transfer.id=test>`. Ele tem de aparecer em `curl -s '127.0.0.1:3200/api/search?q=%7Bspan.transfer.id%3D%22test%22%7D'`. Rode também `docker compose ... config -q`. Derrube com `down -v`, **só no projeto `obs-check`**, nunca no `capzzpay`.
- [ ] **Step 7: Commit** (`infra: stack local de observabilidade com Grafana, Tempo, Prometheus e Loki`)

### Task 2: Base de tracing, métricas e logs nos serviços Kotlin (Marina, `dev-kotlin`)

**Files:**
- Modify: `libs/observabilidade/build.gradle.kts`: `api("org.springframework.boot:spring-boot-starter-opentelemetry")` (conferir o nome exato do starter do Boot 4.1 na documentação oficial) e o appender de logback do OpenTelemetry para os logs OTLP
- Create: `libs/observabilidade/src/main/kotlin/io/github/capozziv/digitalbanking/observability/TracingAutoConfiguration.kt`
- Create: `libs/observabilidade/src/main/kotlin/io/github/capozziv/digitalbanking/observability/CorrelationIdSpanAttribute.kt`
- Modify: `libs/observabilidade/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify: os cinco `services/*/bootstrap/src/main/resources/application.yml`
- Modify: `services/accounts/adapters/.../transfers/TransfersClientConfiguration.kt`, `services/accounts/adapters/.../customer/CustomerClientConfiguration.kt`, `services/transfers/adapters/.../accounts/AccountsClientConfiguration.kt`, `services/transfers/adapters/.../customer/CustomerClientConfiguration.kt` e `libs/security/src/main/kotlin/.../ServiceTokenClient.kt`
- Modify: `libs/testing-architecture` (regra ArchUnit)
- Test: `libs/observabilidade/src/test/kotlin/.../TracingAutoConfigurationTest.kt`, `services/transfers/bootstrap/src/integrationTest/kotlin/.../OtlpExportUnavailableIntegrationTest.kt` e `services/transfers/bootstrap/src/integrationTest/kotlin/.../HttpTracePropagationIntegrationTest.kt`

**Interfaces:**
- Produces:
  - um bean `io.micrometer.tracing.Tracer` em todos os serviços e um `ObservationRegistry` com spans de servidor HTTP;
  - todo `RestClient` de chamada entre serviços construído com `.observationRegistry(observationRegistry)`, mandando `traceparent` junto do `X-Correlation-Id`;
  - MDC com `traceId` e `spanId` (as chaves padrão do Micrometer Tracing);
  - propriedade `digitalbanking.observability.otlp-endpoint`, ligada a `OTEL_EXPORTER_OTLP_ENDPOINT`.
- Consumes: nada de outras tasks.

- [ ] **Step 1: Teste que falha** `TracingAutoConfigurationTest`:
  - `tracer bean exists and http server spans carry correlation id attribute`: um `MockMvc` com `X-Correlation-Id: abc` gera um span com `correlation.id = "abc"`, conferido num `InMemorySpanExporter`;
  - `server span does not record query string or authorization header`: o atributo `url.query` está ausente, e nenhum atributo contém o valor do header `Authorization`.
- [ ] **Step 2:** Rode `./gradlew :libs:observabilidade:test`; esperado: FAIL.
- [ ] **Step 3:** Autoconfiguração que registra `CorrelationIdSpanAttribute`, um `ObservationFilter` ou `SpanHandler` que copia o `correlationId` do MDC para o atributo. Desligue a gravação de query string se o padrão gravar.
- [ ] **Step 4:** Rode o teste; esperado: PASS.
- [ ] **Step 5:** Nos cinco `application.yml`:
  - amostragem 1.0;
  - exportação OTLP de traces, métricas (passo de 10 s) e logs para `${OTEL_EXPORTER_OTLP_ENDPOINT:http://127.0.0.1:4318}`;
  - `management.metrics.distribution.percentiles-histogram.transfers.saga.duration: true` no `transfers`;
  - `logging.structured.format.console: ecs`, mantendo `traceId`, `spanId` e `correlationId`;
  - remover `logging.pattern.correlation`, que o JSON substitui.
  
  Antes, procure quem lê o formato de texto dos logs (`grep -rn "correlation\]\|\[%X" infra tests docs`, mais o `awaitLogLine` do e2e) e ajuste.
- [ ] **Step 6: Teste que falha** `HttpTracePropagationIntegrationTest` (`transfers` chamando um stub de `accounts`): a chamada de saída leva um `traceparent` cujo trace ID é o do span de servidor de entrada, e também `X-Correlation-Id`.
- [ ] **Step 7:** Passe o `ObservationRegistry` nos quatro `*ClientConfiguration.kt` e no `ServiceTokenClient`. Rode o teste; esperado: PASS.
- [ ] **Step 8: Teste** `OtlpExportUnavailableIntegrationTest`: com `OTEL_EXPORTER_OTLP_ENDPOINT=http://127.0.0.1:1` (porta fechada), 20 `POST /transfers` seguidos concluem com 201/202. O p95 fica abaixo de 2× o p95 do mesmo teste com um receptor em memória. Nenhuma linha de log em ERROR com "otlp" por requisição (tolerada uma linha de aviso do exportador por minuto).
- [ ] **Step 9:** Regra ArchUnit em `libs/testing-architecture`: nenhuma classe de `..domain..` depende de `io.opentelemetry..` nem de `io.micrometer..`. Rode `./gradlew build`; esperado: verde.
- [ ] **Step 10: Commit** (`feat: tracing e exportação OTLP nos serviços`)

### Task 3: Contexto do trace no outbox, no SNS e no consumo SQS (Marina, `dev-kotlin`; depois da Task 2)

**Files:**
- Modify: `libs/outbox/src/main/resources/outbox/outbox-table.sql` (`trace_parent text`, anulável)
- Modify: `libs/outbox/src/main/kotlin/.../JdbcOutboxWriter.kt`, `PendingEvent.kt` e `libs/outbox/src/testFixtures/.../OutboxSchemaAssertions.kt`
- Modify: `libs/outbox-relay/src/main/kotlin/.../JdbcOutboxStore.kt`, `SnsEventPublisher.kt` e `OutboxRelay.kt`
- Create: as migrations `services/accounts/adapters/src/main/resources/db/migration/V11__outbox_trace_parent.sql`, `services/transfers/.../V10__outbox_trace_parent.sql` e `services/customer/.../V6__outbox_trace_parent.sql`
- Create: `libs/observabilidade/src/main/kotlin/.../TraceParentSqsMessageInterceptor.kt`
- Modify: `SqsCorrelationIdAutoConfiguration.kt`, para registrar o interceptor novo ao lado do de correlation ID
- Test: `JdbcOutboxWriterIntegrationTest`, `SnsEventPublisherTest`, `OutboxRelayTest`, `TraceParentSqsMessageInterceptorTest` e `services/notification/bootstrap/src/integrationTest/.../SqsTraceContinuationIntegrationTest.kt`

**Interfaces:**
- Consumes: `Tracer` (Task 2).
- Produces:
  - `PendingEvent.traceParent: String?`;
  - atributo de mensagem SNS/SQS `traceparent`, como `const val TRACE_PARENT_ATTRIBUTE = "traceparent"` em `SnsEventPublisher`;
  - spans `outbox publish <eventType>` (produtor) e `<queue> process` (consumidor), com `event.id` e `event.type`.

- [ ] **Step 1: Testes que falham:**
  - `JdbcOutboxWriterIntegrationTest`: `write stores the traceparent of the current span` e `write stores null trace_parent without a current span`;
  - `SnsEventPublisherTest`: `publish sends traceparent attribute when present` e `publish omits traceparent when null`;
  - `OutboxRelayTest`: `publish span is a child of the event trace parent` e `malformed trace parent starts a new trace and still publishes`.
- [ ] **Step 2:** Rode `./gradlew :libs:outbox:check :libs:outbox-relay:check`; esperado: FAIL.
- [ ] **Step 3:** Implemente: o writer grava `trace_parent` a partir do contexto corrente, formatado como `traceparent` W3C pelo propagador; o store o lê; o relay abre o span de publicação extraindo o contexto; o publisher põe o atributo.
- [ ] **Step 4:** Crie as três migrations (`alter table outbox add column trace_parent text;`), sem `grant` novo, porque a tabela já tem.
- [ ] **Step 5:** Rode os testes; esperado: PASS.
- [ ] **Step 6: Testes que falham:**
  - `TraceParentSqsMessageInterceptorTest`: `consumer span continues the trace of the traceparent attribute`, `missing attribute starts a new trace` e `malformed attribute starts a new trace without exception`;
  - `SqsTraceContinuationIntegrationTest` (LocalStack): uma mensagem com `traceparent` gera, no `notification`, um span com o mesmo trace ID.
  
  Antes, confira se a observação de SQS do Spring Cloud AWS 4.2.1 já lê `traceparent` dos atributos. Se ler, use-a e o interceptor só cobre o que faltar.
- [ ] **Step 7:** Implemente e rode `./gradlew build`; esperado: verde.
- [ ] **Step 8: Commit** (`feat: traceparent no outbox, no SNS e no consumo SQS`)

### Task 4: Spans da saga e métricas das transferências (Marina, `dev-kotlin`; depois da Task 2, em paralelo com a 3)

**Files:**
- Create: `services/transfers/adapters/src/main/resources/db/migration/V11__transfer_trace_parent.sql` (`trace_parent text`, anulável)
- Create: `services/transfers/application/src/main/kotlin/.../application/port/SagaTracing.kt` e `.../application/port/TransferOutcomeMetrics.kt`
- Create: `services/transfers/adapters/src/main/kotlin/.../adapters/tracing/MicrometerSagaTracing.kt` e `.../adapters/metrics/MicrometerTransferOutcomeMetrics.kt`
- Modify: `services/transfers/domain/.../Transfer.kt` (`traceParent: String? = null`), `JdbcTransferRepository.kt`, `TransferRows.kt`, `RequestTransferUseCase.kt`, `RunTransferSagaUseCase.kt`, `ResumeDueTransfersUseCase.kt` e `UseCaseConfiguration.kt`
- Test: `RunTransferSagaUseCaseTest` (com `InMemorySagaTracing` e `InMemoryTransferOutcomeMetrics`), `services/transfers/bootstrap/src/integrationTest/.../SagaTraceIntegrationTest.kt` e `MicrometerTransferOutcomeMetricsTest`

**Interfaces:**
- Consumes: `Tracer`/`ObservationRegistry` (Task 2).
- Produces:
```kotlin
// application/port/SagaTracing.kt
interface SagaTracing {
    /** W3C traceparent of the current span, or null when none. */
    fun currentTraceParent(): String?
    /** Runs [block] under a span named "transfer saga" that is a child of [traceParent] (new trace when null or malformed). */
    fun <T> resume(traceParent: String?, transferId: UUID, block: () -> T): T
    /** Runs one step under a span named after [step], with transfer.step and transfer.id; [outcome] marks it. */
    fun <T> step(step: TransferStep, transferId: UUID, block: () -> T, outcome: (T) -> StepSpanOutcome): T
}
sealed interface StepSpanOutcome { data object Ok : StepSpanOutcome; data class Error(val reason: String) : StepSpanOutcome }

// application/port/TransferOutcomeMetrics.kt
interface TransferOutcomeMetrics {
    fun finished(status: TransferStatus, reason: String?, sagaDuration: Duration)
}
```
  `StepSpanOutcome` mora no `application`, não no `domain`.

- [ ] **Step 1: Testes que falham** (`RunTransferSagaUseCaseTest`):
  - `each step runs under its own span`: uma transferência concluída registra os spans `ReserveLimit`, `Debit` e `Credit`, nessa ordem;
  - `refused step marks its span as error with the reason`: uma recusa por saldo dá `Error("insufficient-available-balance")`;
  - `terminal transfer records outcome metric once`: `finished(COMPLETED, null, d)` exatamente uma vez, com `d = updatedAt − createdAt`;
  - `failed transfer records reason`: `finished(FAILED, "daily-limit-exceeded", _)`;
  - `retry after transient failure does not record the metric again`.
- [ ] **Step 2:** Rode `./gradlew :services:transfers:application:test`; esperado: FAIL.
- [ ] **Step 3:** Ajuste o domínio e a persistência (`traceParent` no `Transfer`, coluna na V11, leitura e escrita no `JdbcTransferRepository`).
- [ ] **Step 4:** Ajuste os casos de uso:
  - `RequestTransferUseCase` grava `currentTraceParent()` no pedido;
  - `ResumeDueTransfersUseCase` envolve cada retomada em `resume(transfer.traceParent, transfer.id)`;
  - `RunTransferSagaUseCase` envolve cada passo em `step(...)` e chama `finished(...)` quando o passo vira terminal.
- [ ] **Step 5:** Adapters:
  - `MicrometerSagaTracing` extrai o contexto do `traceparent`;
  - `MicrometerTransferOutcomeMetrics` registra `transfers.finished` e `transfers.saga.duration`. O rótulo `reason` vem de `KNOWN_REASONS`: as constantes de `StepOutcome` e os valores de `publicReason`/`compensationReason`. Fora desse conjunto, vira `"other"`. Com `COMPLETED`, o rótulo fica `"none"`.
- [ ] **Step 6: Testes:**
  - `MicrometerTransferOutcomeMetricsTest`: `unknown reason becomes other` e `completed has reason none`;
  - `SagaTraceIntegrationTest` (PostgreSQL e Redis por Testcontainers, `InMemorySpanExporter`, `accounts` por stub):
    - `saga resumed by the scheduler stays in the request trace`: a primeira chamada ao `accounts` falha com 503, e o agendador conclui em seguida. Todos os spans têm o trace ID do `POST /transfers`;
    - `transfer without trace_parent resumes in a new trace`: linha inserida com `trace_parent` nulo e `PENDING`; conclui, e os spans saem num trace novo.
  
  Mutante: comente o `resume(...)`. O primeiro teste fica vermelho.
- [ ] **Step 7:** Rode `./gradlew build`; esperado: verde.
- [ ] **Step 8: Commit** (`feat: spans por passo da saga e métricas das transferências`)

### Task 5: Tracing e logs no BFF (Lucas, `dev-frontend`)

**Files:**
- Modify: `frontend/bff/package.json` (`@opentelemetry/sdk-node`, `@opentelemetry/exporter-trace-otlp-http`, `@opentelemetry/exporter-logs-otlp-http` e as instrumentações de `http`, `express`, `undici` e `ioredis`)
- Create: `frontend/bff/src/observability/tracing.ts`, carregado antes do Nest por `node --import ./dist/observability/tracing.js` no `start:prod` e no `Dockerfile`
- Modify: `frontend/bff/src/observability/logger.ts` (`traceId` e `spanId` no `mixin`; logs também por OTLP, com a mesma `redact`)
- Modify: o handler de `POST /bff/transfers` em `frontend/bff/src/screens/` (atributo `transfer.id` no span ativo, a partir da resposta)
- Test: `frontend/bff/src/observability/tracing.spec.ts` e `logger.spec.ts`

**Interfaces:**
- Consumes: `OTEL_EXPORTER_OTLP_ENDPOINT` e `OTEL_SERVICE_NAME=bff` (Task 1 no compose).
- Produces: o span raiz `POST /bff/transfers` com `transfer.id` e o `traceparent` nas chamadas aos serviços.

- [ ] **Step 1: Testes que falham:**
  - `outgoing backend call carries traceparent of the inbound span`;
  - `log line has traceId and spanId`;
  - `exported log keeps redaction of password and token`;
  - `exporter unreachable does not fail the request`.
- [ ] **Step 2:** Rode `npm test -w bff`; esperado: FAIL.
- [ ] **Step 3:** Implemente com o `BatchSpanProcessor` (fila limitada). A instrumentação HTTP não grava query string nem os headers `cookie` e `authorization`.
- [ ] **Step 4:** Rode `npm run build -w bff`, `npm run lint` e `npm test -w bff`; esperado: verdes.
- [ ] **Step 5: Commit** (`feat: tracing OpenTelemetry e logs com trace no BFF`)

### Task 6: Coletor e destinos na AWS, desenhados e validados sem aplicar (Rafael, `infra`)

**Files:**
- Create: `docs/pesquisa/2026-11-23-observabilidade-aws.md` (skill `mattpocock-skills:research`), com:
  - preços atuais em `us-east-1` de X-Ray, CloudWatch Logs, métricas EMF e painel;
  - se o X-Ray aceita trace IDs W3C pelo endpoint OTLP ou pela busca de transações;
  - qual exportador usar (`awsxray` ou OTLP);
  - como pesquisar o `transfer.id`;
  - ADOT como add-on do EKS ou como chart.
- Create: `deploy/helm/otel-collector/` (chart, se a pesquisa não indicar o add-on) com a configuração do ADOT: receptor OTLP; `memory_limiter` e `batch`; um processador que filtra as métricas para os nomes do Global Constraints mais `outbox.*`, `statement.cache.*`, `*.dlq.messages` e `transfers.daily_limit.circuit.state`; exportadores para X-Ray, `awsemf` e `awscloudwatchlogs`
- Modify: `deploy/helm/values/*.yaml` (`OTEL_EXPORTER_OTLP_ENDPOINT` apontando para o `Service` do coletor e `OTEL_SERVICE_NAME`)
- Modify: `infra/terraform/modules/access/main.tf`, com o papel e a Pod Identity do coletor: `xray:PutTraceSegments`, `xray:PutTelemetryRecords`, `cloudwatch:PutMetricData` (condicionado ao namespace `capzzpay`) e `logs:CreateLogStream`/`PutLogEvents` só nos grupos do projeto
- Create: `infra/terraform/modules/observability/` com:
  - um grupo de logs `/capzzpay/<serviço>` para cada um dos seis, com `retention_in_days = 1`;
  - `aws_cloudwatch_dashboard` "Transferências", espelhando os painéis da Task 1;
  - a configuração da busca de transações, se a pesquisa pedir
- Modify: `infra/terraform/envs/aws/main.tf`, que liga o módulo
- Modify: `.github/workflows/aws-up.yml`, para instalar o coletor antes dos serviços (só se for chart)

**Interfaces:**
- Consumes: os nomes de métrica e os atributos do Global Constraints; o endpoint e o nome de serviço por variável, como na Task 1.
- Produces: o relatório da pesquisa, citado pelo ADR da Task 8; e, quando o X-Ray não aceitar IDs W3C, a instrução para a Task 2/5 trocar o gerador de IDs pelo da AWS. **Se isso acontecer, Rafael avisa a coordenadora antes de seguir.**

- [ ] **Step 1:** Faça a pesquisa e grave o relatório, com os links das fontes e a tabela de custo por subida de 8 h, que vai substituir a seção 7 da spec.
- [ ] **Step 2:** Coletor, values, Terraform e workflow.
- [ ] **Step 3: Verificar:**
  - `infra/terraform/envs/aws/validate.sh` e os scripts `check-*.py`;
  - `deploy/helm/validate.sh` e `helm template` do coletor;
  - `otelcol-contrib validate --config` na configuração do ADOT, pela imagem oficial.
  
  Esperado: tudo verde. Não rode `apply`.
- [ ] **Step 4: Commit** (`infra: coletor ADOT, X-Ray e CloudWatch para o ambiente AWS`)

### Task 7: Smoke e CI com observabilidade (Rafael, `infra`; depois das Tasks 1–5)

> Ajuste da spec (seção 10, item 5, decisão da coordenadora): a prova ponta a ponta do trace fica no job `smoke`, que já sobe a stack inteira pelo compose, BFF incluído, e não no e2e Kotlin, que roda sem o BFF. A continuidade dentro do `transfers` e pela fila tem testes de integração próprios nas Tasks 3 e 4.

**Files:**
- Modify: `infra/local/smoke.sh`, que chama `observability-check.sh "$transfer_id"` depois da transferência do smoke e falha se ele falhar
- Modify: `.github/workflows/ci.yml`, com `--profile observability` no `up` dos jobs `smoke` e `frontend-e2e`
- Test: o próprio smoke, local e no CI

**Interfaces:**
- Consumes: `observability-check.sh` (Task 1); spans e métricas das Tasks 2–5.

- [ ] **Step 1:** O smoke faz a transferência pelo BFF (`/bff/transfers`), para o trace ter a raiz no BFF. Se o smoke atual for direto aos serviços, crie uma segunda transferência pelo BFF só para essa checagem.
- [ ] **Step 2: Verificar localmente** num projeto isolado (`-p obs-smoke`): `up --wait` com os profiles `apps` e `observability`, `seed.sh` e `smoke.sh`; esperado: verde, com "observability check ok". Derrube só o `obs-smoke`.
- [ ] **Step 3:** Abra o PR da branch da etapa (feito pela coordenadora). CI: os 17 jobs e o `ci-ok` verdes; o job `smoke` com até ~2 min a mais.
- [ ] **Step 4: Commit** (`infra: smoke e CI conferem traces e métricas`)

### Task 8: Documentação (Clara, `documentador`; depois das Tasks 1–6)

**Files:**
- Create: `docs/adr/0028-tracing-distribuido-com-opentelemetry.md`
- Modify: `docs/adr/0006-observabilidade-com-correlation-id.md` (status "substituído em parte por 0028"), `docs/adr/README.md`, `docs/arquitetura.md` (seção Observabilidade), `docs/roadmap.md` (linha da etapa 10), `libs/observabilidade/README.md` e `docs/rodando-localmente.md` (seção "Observabilidade")
- Modify: a seção 7 da spec, com a tabela de custo da pesquisa da Task 6

- [ ] **Step 1:** Escreva o ADR 0028: contexto, decisão (OpenTelemetry, W3C, coletor; Grafana local; X-Ray e CloudWatch na AWS), consequências e alternativas (B e C da spec, agente Java, links por retomada). Cite o relatório da pesquisa.
- [ ] **Step 2:** No guia, explique como subir com `--profile observability`, abrir o Grafana, ler o painel, colar o ID da transferência e ir do span aos logs, com as portas do Tempo, do Prometheus e do Loki.
- [ ] **Step 3:** Confira que os links relativos resolvem (`grep -o '](\.\./[^)]*)'` em cada arquivo, testando a existência).
- [ ] **Step 4: Commit** (`docs: ADR 0028 e guia de observabilidade`)

### Depois do merge (coordenadora, com o usuário)
- Migrar a stack `capzzpay` com backup em `.superpowers/backups/`: imagens novas, `--no-deps` só nos serviços alterados, mais os contêineres do profile `observability`, sem tocar em volumes nem no LocalStack.
- Sessão de teste com o usuário: transferências concluída, recusada e compensada; painel; busca por ID; logs a partir do span.
