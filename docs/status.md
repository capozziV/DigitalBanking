# Status do projeto

> Atualizado pelo agente coordenador ao fim de cada tarefa.

**Última atualização:** 2026-10-23
**Fase atual:** etapa 1 — bloco 1 com merge na `main` (PR #3); bloco 2 (relay e SNS FIFO no LocalStack) concluído na branch `etapa-1-bloco-2`, revisado task a task e na revisão final da branch; PR do bloco 2 para a `main`.
**Próxima etapa:** mudar o trabalho para a máquina do usuário e testar o guia local no Mac; depois, etapa 2.

## Concluído
- Planejamento: arquitetura, roadmap, ADRs 0001–0014, glossário `CONTEXT.md`, agentes e fluxo com os plugins Superpowers e Matt Pocock; papéis do fluxo mapeados para os agentes do projeto no `CLAUDE.md`, com nomes: Helena (coordenação), Marina (`dev-kotlin`), Rafael (`infra`), Lucas (`dev-frontend`), Beatriz (`revisor`), Clara (`documentador`).
- Código Kotlin em inglês, com os nomes de código do glossário `CONTEXT.md`; docs e commits em português.
- Decisões de 2026-10-21: Terraform, ArchUnit, Spring MVC com virtual threads, Spring Boot 4.1, ktlint, um módulo Gradle por camada, pacote raiz `io.github.capozziv.digitalbanking`, LocalStack fora da etapa 0.
- Etapa 0 ([spec](superpowers/specs/2026-10-21-etapa-0-fundacao-design.md), [plano](superpowers/plans/2026-10-21-etapa-0-fundacao.md)), cada task revisada:
  - build: Gradle 9.8.2, catálogo de versões, `build-logic` com 3 plugins de convenção, ktlint;
  - regras ArchUnit no `bootstrap`;
  - `accounts` com Spring Boot 4.1.1, virtual threads, Flyway e teste Testcontainers do `/actuator/health` (db `UP`);
  - docker-compose em `infra/local/` (PostgreSQL 17, Redis, nginx);
  - CI no GitHub Actions (jobs `build` e `smoke`).
- Decisões de 2026-10-22 (antes da etapa 1): saldo no lançamento (`sequence` e `balance_after`) com trava pessimista `FOR UPDATE` na conta ([ADR 0012](adr/0012-saldo-no-lancamento-e-trava-pessimista.md)); Spring JDBC com `JdbcClient` ([ADR 0013](adr/0013-persistencia-com-spring-jdbc.md)); cheque especial por tipo de conta, com limites numa tabela alterável por `/admin/account-types/{type}` ([ADR 0014](adr/0014-tipo-de-conta-e-cheque-especial.md)); glossário com Tipo de conta, Limite de cheque especial e Saldo disponível; limite diário único no MVP.
- PR #1 da etapa 0 com merge na `main` (2026-10-22). PR #2 (ADRs 0012–0014) com merge na `main` (2026-10-22).
- Etapa 1, bloco 1 ([spec](superpowers/specs/2026-10-22-etapa-1-accounts-design.md), [plano](superpowers/plans/2026-10-22-etapa-1-accounts.md)), Tasks 1–13, cada uma revisada pela Beatriz e a branch inteira na revisão final:
  - domínio (`Account`, `LedgerEntry`, valores em centavos, cheque especial por tipo), casos de uso com idempotência por chave no lançamento e na abertura;
  - persistência JDBC com `FOR UPDATE` e `lock_timeout`, dois usuários de banco (`accounts_app` sem `UPDATE`/`DELETE` no ledger), relógio de microssegundos;
  - API HTTP com Problem Details, correlation ID, OpenAPI (springdoc 3.1.1), log e métrica de débito recusado;
  - `libs/outbox` (envelope e writer JDBC; o relay é do bloco 2);
  - source set `integrationTest`, 7 regras ArchUnit vistas falhando, nginx com os caminhos da API, smoke com operações, coleção Bruno e [guia para rodar localmente](rodando-localmente.md);
  - critérios de pronto: 100 débitos paralelos com exatamente 37 aprovados; 20 repetições com a mesma chave geram um lançamento; `./gradlew build --rerun-tasks` verde (174 testes, 2026-10-23);
  - decisões da etapa: ADR 0015 (outbox, relay líder e SNS FIFO) e as tomadas nas revisões, registradas na spec.
- Bloco 1 com merge na `main` (PR #3, 2026-10-23).
- Etapa 1, bloco 2 (Tasks 14–16, cada uma revisada pela Beatriz e a branch inteira na revisão final, 2026-10-23):
  - SNS FIFO validado no LocalStack `2026.10.2`; helper de teste `libs/testing-localstack` (token, imagem fixa, falha no CI sem token);
  - `OutboxRelay` em `libs/outbox`: líder por `pg_try_advisory_xact_lock`, publicador SNS FIFO (`MessageGroupId` = conta, `MessageDeduplicationId` = `eventId`), conta presa não ocupa o lote das outras (índice `outbox_stuck_idx`, `V6`), limites de 2 s por publicação e 10 s por ciclo, métricas `outbox.pending`, idade do mais antigo e `outbox.relay.cycles`;
  - relay ligado no `accounts` (`OutboxRelayScheduler` com desligamento gracioso), LocalStack no docker-compose com tópico e fila de inspeção (`RawMessageDelivery=true`), segredo `LOCALSTACK_AUTH_TOKEN` nos jobs `build` e `smoke`, smoke conferindo os eventos na fila;
  - `./gradlew build --rerun-tasks` verde com o token (230 testes, 0 pulados), compose e smoke OK, actionlint e shellcheck sem erros. O CI no GitHub ainda não rodou com o segredo (primeiro push do PR).
- graphify configurado (2026-10-22): `graphify-out/` versionado, skill, hook do Claude antes de buscas e hook do git que reconstrói o grafo a cada commit.
- Verificação (ambiente de nuvem, 2026-10-21): `./gradlew build --rerun-tasks` verde (5 testes, 0 falhas); compose saudável com e sem `--env-file`; rota `/accounts/actuator/health` pelo nginx `UP`; actionlint sem erros.
- CI no GitHub (PR #1, 2026-10-21): `build` e `smoke` verdes. O `smoke` falhou na primeira execução porque o healthcheck do nginx usava `localhost`, que no runner resolve para IPv6 enquanto o nginx escuta só em IPv4; corrigido para `127.0.0.1` (sem `listen [::]:80`, que derruba o nginx em máquinas sem IPv6).

## Como rodar localmente
Guia completo, para macOS (zsh) e Linux: [`docs/rodando-localmente.md`](rodando-localmente.md). Resumo: JDK 21 e Docker Desktop; `cp infra/local/.env.example infra/local/.env`; compose `up --wait`; `./gradlew build` (ou `./gradlew test` sem Docker); `bootRun`; `infra/local/smoke.sh`; coleção Bruno em `api/bruno/accounts/`. O guia ainda não foi executado num Mac de verdade.

## Em andamento
- PR do bloco 2 (`etapa-1-bloco-2` → `main`), aguardando o CI.

## Próximos passos
1. Usuário: conferir o CI verde, revisar e fazer o merge do PR do bloco 2.
2. Usuário: rotacionar o token do LocalStack (ficou registrado no chat) e atualizar o segredo no GitHub e o `infra/local/.env`.
3. Pedido do usuário (2026-10-23), no fim do bloco 2: mudar o modo de trabalho para que o coordenador e os agentes alterem o código na máquina do usuário, que acompanha o código no próprio computador.
4. Pedido do usuário (2026-10-22): depois da etapa 1, montar uma forma de rodar e testar o projeto na máquina dele (sessão do Claude Code no Mac ou uso do computador) e executar o guia local do zero lá.

## Pendências e decisões em aberto
Decisões: fonte única na seção "Em aberto" de `docs/arquitetura.md`.

Débito técnico do bloco 1 da etapa 1 (das revisões; nenhum bloqueia; detalhes nos relatórios das tasks):
- Cache no Redis com TTL das respostas de idempotência (mesma resposta para recusa repetida), se algum consumidor precisar.
- Desbloqueio manual de conta presa no relay.
- Relay sem backoff: com o broker fora do ar, o limiar de 10 tentativas chega em ~5 s e cada ciclo loga um `ERROR` com stack trace por conta presa; trocar por backoff por evento ou `ERROR` só na transição.
- `libs/outbox` expõe AWS SDK e Micrometer como `api`; separar o relay num módulo próprio quando chegar o 2º serviço (etapa 2).
- Testes do relay: `get()` no `finally` do `OutboxRelayIntegrationTest` pode encobrir a exceção original; KDoc do `OutboxRelayUnreachableBrokerIntegrationTest` não diz que ele também cobre a ligação com o agendador.
- Encerrar ou bloquear conta: sem regra definida.
- Resposta de crédito/débito/troca de tipo montada com um `getAccount` fora da transação da escrita; `GetAccountUseCase` lê fora de transação.
- `V5__app_grants.sql`: limitar `UPDATE` em `account` a `(type, updated_at)`; toda tabela nova precisa de `grant` próprio (sem `ALTER DEFAULT PRIVILEGES`).
- Teste de igualdade entre o esquema das migrations `V4` + `V6` e o DDL modelo de `libs/outbox`.
- Duplicata de lançamento logada como `ERROR` duas vezes (repositório e handler).
- ArchUnit: regra de sufixo `UseCase` cobre só o pacote `usecase`; isenções por sufixo; `domain`/`application` sem regra contra infraestrutura (o Gradle já impede); `@Controller` + `@ResponseBody` fora da regra.
- OpenAPI: `ProblemDetail` repetido nas anotações (customizer global); `X-Correlation-Id` fora dos parâmetros.
- Testes: comparar entidade inteira no débito do domínio; teste de limite exato no caso de uso; testes de `OverdraftLimit`/`AvailableBalance` em arquivos próprios; `Thread.sleep` no teste de integração de corrida; literais repetidos.
- nginx: `proxy_set_header` repetido nos `location`; Bruno: teste do `02-consultar-conta.bru` só confere presença; `actionlint` fora do CI (etapa 8).
- Glossário: "valor" no sentido de enum conflita com o termo Valor (`Money`).

Débito técnico de produto, fora do MVP (decidido em 2026-10-22):
- Juros e tarifa sobre saldo negativo.
- Histórico de troca de tipo de conta.
- Auditoria com autor das alterações de limite por tipo (junto da etapa 7).
- Limite diário variando por tipo de conta.

Melhorias técnicas adiadas na revisão da etapa 0 (nenhuma bloqueia):
- **Etapa 2 (2º serviço):** `repositoriesMode = FAIL_ON_PROJECT_REPOS`; avaliar remover `pluginManagement.repositories` da raiz (só `includeBuild`); extrair o `val libs = ...` repetido nos plugins de convenção se crescer; documentar o fluxo de adicionar banco no init; init com senha via `psql -v` e idempotente; volume nomeado para o PostgreSQL.
- **Etapa 7:** restringir `management.endpoint.health.show-details` (hoje `always`, sem autenticação); headers `X-Forwarded-Proto`/`X-Real-IP` no nginx; Swagger UI e `/v3/api-docs` abertos (proteger ou desligar por perfil); exceções de segurança e desconexão do cliente fora do handler genérico de 500; mover o filtro de correlation ID e a constante do MDC para `libs/observabilidade` (hoje o adapter web depende de `libs/outbox` só por ela); `/actuator/metrics` aberto sem autenticação e pelo nginx (bloco 2).
- **Etapa 8:** endpoint AWS padrão `http://localhost:4566` no `application.yml` (na AWS, variável vazia ou perfil `local`); nome do jar por serviço (`accounts.jar`), smoke reaproveitando o jar do job `build`, step `if: failure()` no smoke com `docker compose logs` e o `State.Health` dos contêineres, detectar morte do processo no laço do smoke, `cancel-in-progress` só em PRs, actions fixadas por SHA, shellcheck nos blocos `run`.
- **Quando incomodar:** ruído de CDS da JVM nos testes (agente do Mockito); `webmvc` explícito no `bootstrap`. Healthchecks e curls dentro de contêineres: usar `127.0.0.1`, nunca `localhost` (IPv6 no runner).

## Com o usuário (fora do código)
- Abrir PRs: pela API do GitHub com o token do usuário (PR #2) ou pelo link de criação; a CLI `gh` da sessão não tem login.
- Proteger a `main` no GitHub exigindo CI verde para merge.

## Ambiente do Claude
- **Nuvem:** JDK 21 e Docker. Maven Central e Docker Hub limitam o IP compartilhado (`429`): usar o init script do Gradle com o espelho `maven-central.storage-download.googleapis.com` e `registry-mirrors: ["https://mirror.gcr.io"]` no `dockerd` (configuração do ambiente, fora do repo).
- **Push:** pela VM do Mac, com um token fine-grained do GitHub em `.git/github-token` (fora do controle de versão), lido por um `credential.helper` configurado só no `.git/config` local. Sem esse arquivo, o push pela VM falha; pela nuvem funciona quando o repositório está conectado à sessão.
- **VM do Mac:** sem Docker, sem acesso ao Docker Desktop do Mac e só com Java 11; serve para editar arquivos da pasta do usuário. Por isso o build e os testes rodam no clone da nuvem.
- **LocalStack na nuvem:** o proxy da sessão só escuta em 127.0.0.1, então o LocalStack sai com código 55 (licença) sem um encaminhador TCP e as variáveis `LOCALSTACK_HTTPS_PROXY`/`LOCALSTACK_CA_BUNDLE` do helper; o compose usa um override fora do repo. A porta do proxy muda a cada reinício do container: recriar o encaminhador com a porta atual de `$HTTPS_PROXY`. `actionlint` e `shellcheck` não vêm instalados.
- **Nuvem (etapa 1):** o `dockerd` é iniciado à mão (`dockerd` em segundo plano) e para a cada reinício da sessão; o graphify fica em `~/.local/bin` (`uv tool install "graphifyy[sql]"`).
- **Painel lateral do Claude (mod `equipe-etapa-1`):** não aparece no app usado pelo usuário (sem superfície de desenho); o acompanhamento é pela página "Equipe da Etapa 1" e pela lista de tarefas.
