# Etapa 5 — cache do extrato: design

- **Data:** 2026-11-12
- **Status:** aguardando revisão do usuário
- **Roadmap:** etapa 5, "Cache-aside da 1ª página, TTL com jitter, invalidação por evento, fallback".
- **ADR base:** [0009 — Cache do extrato](../../adr/0009-cache-do-extrato.md).

## Objetivo
A primeira página do extrato é a consulta mais comum, e o cache tira essa leitura do DynamoDB. Ele não pode servir dado velho por mais que uma janela curta. Se o Redis cair, o extrato continua respondendo pelo DynamoDB.

## Decisões desta etapa (usuário, 2026-11-12)
1. **Só Redis**, sem Caffeine local na frente. Encerra a decisão #18. Um cache por instância exigiria invalidação em todas as instâncias, e o ganho sobre o Redis é pequeno. O Caffeine fica para depois do MVP, se uma medição pedir.
2. **Invalidação por remoção da chave** ao consumir o lançamento, sem reescrever a página.
3. **TTL de 60 s com jitter de ±10 s** (entre 50 e 70 s).
4. **Só a 1ª página com o `limit` padrão (20) entra no cache.** Cursor ou outro `limit` vão direto ao DynamoDB.
5. **Redis lento ou fora:** timeout de 100 ms, WARN no log, métrica de erro e leitura no DynamoDB, sem circuit breaker.
6. **Métricas de acerto, falta e erro** no Micrometer, cada uma provada por teste.
7. **Escopo só do cache.** O filtro por data (#116) fica para a etapa 9 (frontend).

## Critério de pronto
- Build completo verde (`./gradlew build`, com ktlint, unidade, integração, ArchUnit e e2e).
- Smoke e Bruno das quatro coleções verdes.
- Testes de integração provam acerto, falta, invalidação ao consumir e fallback com o Redis fora.
- Revisão de cada task e revisão final da branch pela Beatriz, com mutantes vermelhos.

## Restrições (de `CLAUDE.md` e ADRs)
- O cache é só de leitura do extrato e nunca autoriza débito (ADR 0007).
- Clean Architecture: o cache é uma porta no `application`, implementada no `adapters`. O `domain` não conhece Redis.
- Código, testes e logs em inglês; valores em centavos `Long`.
- O consumidor continua idempotente; a invalidação não muda a deduplicação pela chave do item (ADR 0018).

## 1. Porta e casos de uso
- Nova porta `StatementFirstPageCache` no `application`:
  - `get(accountId): StatementPage?`;
  - `put(accountId, page)`;
  - `evict(accountId)`.
- O `GetStatementUseCase` consulta o cache só quando `cursor == null` e `limit == DEFAULT_LIMIT`:
  - **acerto:** devolve a página guardada;
  - **falta:** lê no repositório e guarda a página, se ela não for virar `404`.
- O `404` de conta desconhecida nunca entra no cache. A página vazia de conta conhecida entra, porque é uma resposta `200`.
- O `RecordStatementEntryUseCase` chama `evict(accountId)` depois do `insertEntry`, tanto em `RECORDED` quanto em `DUPLICATE`. No `DUPLICATE` é para cobrir uma entrega anterior que gravou o lançamento e caiu antes de invalidar.
- O `RegisterAccountUseCase` não invalida: o `AccountOpened` não muda os lançamentos e o `404` nunca foi guardado.

## 2. Adapter Redis
- `RedisStatementFirstPageCache` em `adapters/cache/`, com Spring Data Redis (Lettuce) e `StringRedisTemplate`.
- Chave `statement:first-page:v1:{accountId}` (a versão muda se o formato do valor mudar). O valor é o JSON da página (lançamentos e `nextCursor`), lido e escrito por um mapeamento próprio do adapter; o domínio não ganha anotação.
- O TTL é sorteado a cada `put`: 60 s ± 10 s, com `statement.cache.ttl` e `statement.cache.ttl-jitter` configuráveis.
- Timeout de comando e de conexão de 100 ms (`statement.cache.timeout`).
- Toda exceção do Redis vira, dentro do adapter, um WARN só com a mensagem (sem stack trace) e a métrica de erro.
  - `get` com erro devolve `null` e o caso de uso lê no DynamoDB.
  - `put` e `evict` com erro não propagam: a mensagem do SQS é confirmada, e o dado velho dura no máximo um TTL.
- O cache pode ser desligado por `statement.cache.enabled=false`, que troca o adapter por uma implementação vazia. É útil em testes e como válvula de operação.

## 3. Consistência
- **Janela normal:** entre a gravação do lançamento no DynamoDB e o `evict`, uma leitura ainda pode receber a página antiga. A janela vai do consumo até o `DEL`, coisa de milissegundos.
- **Corrida conhecida:**
  1. uma leitura começa;
  2. ela erra o cache e lê a página antiga no DynamoDB;
  3. o consumidor grava e faz o `evict`;
  4. a leitura grava no cache a página antiga.

  Essa página velha dura no máximo um TTL (até 70 s). É aceito, conforme o ADR 0009: o extrato é leitura e o saldo de autorização nunca vem de cache. Fica registrado num teste que documenta o limite pelo TTL.
- **Redis fora no `evict`:** o mesmo limite de um TTL.

## 4. Observabilidade
- Contador `statement.cache.requests`, com a tag `result` = `hit`, `miss` ou `error`.
- Contador `statement.cache.evictions`, com a tag `result` = `ok` ou `error`.
- Os logs de WARN levam o correlation ID do MDC, como o resto do serviço.

## 5. Configuração
- `application.yml` do `statement`:
  - `spring.data.redis.host: ${STATEMENT_REDIS_HOST:localhost}`;
  - `spring.data.redis.port: ${STATEMENT_REDIS_PORT:6379}`;
  - `spring.data.redis.timeout: ${STATEMENT_CACHE_TIMEOUT:100ms}`;
  - `statement.cache.enabled`, `statement.cache.ttl` e `statement.cache.ttl-jitter`.
- O health do actuator não pode ficar `DOWN` por causa do Redis, porque o serviço funciona sem ele. Desligar o indicador de Redis (`management.health.redis.enabled=false`) e expor o estado só pela métrica.

## 6. Testes
- **Unidade (`application`, com um cache falso em memória):**
  - acerto não consulta o repositório;
  - falta consulta e guarda;
  - cursor e `limit` diferente de 20 não tocam o cache;
  - o `404` não é guardado;
  - `RECORDED` e `DUPLICATE` invalidam.
- **Integração (`bootstrap`, Redis em Testcontainers e LocalStack):**
  - a 2ª leitura vem do cache (métrica `hit`, sem nova consulta ao DynamoDB);
  - um lançamento consumido pela fila invalida e a leitura seguinte mostra o lançamento novo;
  - o TTL gravado fica entre 50 e 70 s;
  - com o Redis parado (contêiner pausado ou porta fechada), o extrato responde `200` pelo DynamoDB em menos de 1 s, com a métrica `error` e o WARN;
  - uma página guardada com JSON inválido é tratada como falta.
- **Mutantes a rodar na revisão:**
  - sem o `evict`, o teste da invalidação fica vermelho;
  - com o `get` lançando a exceção para fora, o teste do fallback fica vermelho;
  - guardando também com cursor, o teste "cursor não toca o cache" fica vermelho.
- **e2e:** depois da transferência, o extrato da origem mostra o débito novo mesmo com uma leitura feita antes (prova a invalidação de ponta a ponta).

## 7. Ambiente local e CI
- O compose já tem o Redis. O `statement` ganha as variáveis `STATEMENT_REDIS_*` no `.env.example` e no guia local.
- O apoio de testes ganha um contêiner Redis (`redis:7-alpine`) para o `statement`. Ele pode ficar em `libs/testing-redis`, se a etapa 6 (limite diário com Redis) for reaproveitá-lo; a decisão de onde fica é do plano.
- O e2e sobe o `statement` apontando para um Redis.
- O job `statement` do CI continua cobrindo o serviço.

## 8. Documentação que acompanha
- ADR 0009 atualizado: só Redis, as decisões 2 a 5 e a corrida aceita.
- Item de "Em aberto" em `docs/arquitetura.md` removido; cartão #18 fechado.
- `docs/rodando-localmente.md`: variáveis `STATEMENT_REDIS_*`, como ver as chaves (`redis-cli --scan --pattern 'statement:first-page:v1:*'`) e as métricas do cache.

## Fora de escopo (e onde fica registrado)
- Caffeine local: depois do MVP, se a medição pedir (ADR 0009).
- Filtro por data (#116): etapa 9.
- Circuit breaker: etapa 8, se precisar.
- Cache das páginas seguintes ou de outro `limit`: não previsto.

## Riscos
- **Serialização da página:** uma mudança no formato do lançamento quebra a leitura de páginas já guardadas. Mitigação: um JSON inválido conta como falta, e a versão no prefixo da chave muda junto com o formato.
- **Timeout de 100 ms no runner do CI:** pode ser apertado sob carga. Os testes de integração usam o mesmo valor, e o de fallback mede o limite com folga (1 s).
