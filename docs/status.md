# Status do projeto

> Resumo do projeto, atualizado pelo agente coordenador ao fim de cada tarefa. Tasks, débito técnico, decisões pendentes e tarefas do usuário ficam no board.

**Última atualização:** 2026-10-27
**Board:** [Digital Banking no GitHub Projects](https://github.com/users/capozziV/projects/2)
**Fase atual:** etapa 2 em execução ([spec](superpowers/specs/2026-10-27-etapa-2-transfers-design.md), [plano](superpowers/plans/2026-10-27-etapa-2-transfers.md), [ADR 0016](adr/0016-saga-orquestrada-por-http.md)). Bloco 1 (preparação para o 2º serviço, mais o domínio do `transfers`) revisado, no PR da branch `etapa-2-transfers`; bloco 2 (`transfers` com a saga) em andamento.
**Próximo passo:** merge do PR do bloco 1 pelo usuário; depois, Tasks 10 a 16 do bloco 2.

## Como usar o board
- Colunas: **Backlog** (tem pendência técnica ou de regra de negócio sem resposta, ou a etapa anterior ainda não está em Done), **Ready** (sem pendência e com a etapa anterior em Done), **In progress**, **In review** (revisão da Beatriz) e **Done** (revisado, build, testes e lint verdes, merge feito).
- Campos: Stage (0–9), Service, Item type (Stage, Task, Tech debt, Decision) e Priority.
- Cada etapa do roadmap é um cartão (`Stage N · …`), bloqueado pela etapa anterior; as tasks e o débito técnico da etapa são sub-issues dele.
- Cada issue tem a seção "Open questions" com a checklist das pendências. A resposta do usuário fica anotada e o item marcado; pendência nova devolve o cartão ao Backlog.
- O board é em inglês. Documentação, ADRs e commits seguem em português.

## Concluído
- **Planejamento:** arquitetura, roadmap, ADRs 0001–0015, glossário `CONTEXT.md`, agentes do projeto e fluxo com os plugins Superpowers e Matt Pocock (mapa de papéis no `CLAUDE.md`).
- **Etapa 0, fundação** ([spec](superpowers/specs/2026-10-21-etapa-0-fundacao-design.md), [plano](superpowers/plans/2026-10-21-etapa-0-fundacao.md); PR #1, 2026-10-22): Gradle 9.8.2 com `build-logic` e ktlint, regras ArchUnit, `accounts` com Spring Boot 4.1.1 e virtual threads, docker-compose (PostgreSQL 17, Redis, nginx), CI com os jobs `build` e `smoke`.
- **Decisões de 2026-10-22** (PR #2): saldo no lançamento com trava `FOR UPDATE` ([ADR 0012](adr/0012-saldo-no-lancamento-e-trava-pessimista.md)), Spring JDBC com `JdbcClient` ([ADR 0013](adr/0013-persistencia-com-spring-jdbc.md)), cheque especial por tipo de conta ([ADR 0014](adr/0014-tipo-de-conta-e-cheque-especial.md)).
- **Etapa 1, `accounts`** ([spec](superpowers/specs/2026-10-22-etapa-1-accounts-design.md), [plano](superpowers/plans/2026-10-22-etapa-1-accounts.md); PRs #3 e #4, 2026-10-23), cada task revisada pela Beatriz:
  - ledger imutável, idempotência por chave, cheque especial por tipo, API com Problem Details, correlation ID e OpenAPI;
  - `libs/outbox` com `OutboxRelay` (líder por trava consultiva, SNS FIFO no LocalStack, conta presa não ocupa o lote das outras) ([ADR 0015](adr/0015-outbox-relay-lider-e-sns-fifo.md));
  - critérios de pronto: 100 débitos paralelos com exatamente 37 aprovados; 20 repetições com a mesma chave geram um lançamento; `./gradlew build --rerun-tasks` verde (231 testes, 0 pulados, com LocalStack); CI do PR #4 verde.
- **Guia local no Mac** (PR #6, 2026-10-23): executado do zero num Mac Intel (build verde, smoke OK, Bruno 7/7) e corrigido para caminhos sem Homebrew.
- **graphify** (2026-10-22): `graphify-out/` versionado, hook do Claude antes de buscas e hook do git que reconstrói o grafo a cada commit.
- **Board no GitHub Projects** (2026-10-26): tasks, débito técnico, decisões de "Em aberto" e tarefas do usuário migrados deste arquivo para o board.
- **Etapa 2, bloco 1** (2026-10-27; Tasks 1–6 e 8 do plano, cada uma revisada pela Beatriz e a branch inteira na revisão final):
  - relay em `libs/outbox-relay` (o `libs/outbox` ficou sem AWS SDK e Micrometer) e asserção de igualdade do esquema do outbox nos `testFixtures`;
  - regras ArchUnit compartilhadas em `libs/testing-architecture` (`ServiceArchitectureRules`), com as lacunas antigas fechadas (isenção só para tipos de dados);
  - `GET /accounts/{id}/entries?idempotencyKey=` para a saga consultar um lançamento sem gravar;
  - Gradle com `FAIL_ON_PROJECT_REPOS`; init do PostgreSQL idempotente, banco `transfers` e volume nomeado;
  - CI seletivo por serviço com o job agregador `ci-ok` (é ele que a proteção da `main` deve exigir);
  - domínio do `transfers` com os passos da saga no padrão State;
  - `./gradlew build --rerun-tasks` verde com o token do LocalStack (319 testes, 0 falhas, 0 pulados).
- Execução da etapa 2 com implementadores em paralelo, cada um num worktree em `.claude/worktrees/` (fora do git), juntados na branch de integração depois da revisão.

## Como rodar localmente
Guia completo, para macOS (zsh) e Linux: [`docs/rodando-localmente.md`](rodando-localmente.md). Resumo: JDK 21 e Docker Desktop; `cp infra/local/.env.example infra/local/.env`; compose `up --wait`; `./gradlew build` (ou `./gradlew test` sem Docker); `bootRun`; `infra/local/smoke.sh`; coleção Bruno em `api/bruno/accounts/`.

## Convenções que já custaram caro
- Agentes em worktree não regeneram nem commitam `graphify-out/`; a coordenadora regenera o grafo depois de juntar as branches.
- Healthchecks e curls dentro de contêineres usam `127.0.0.1`, nunca `localhost`: no runner do GitHub, `localhost` resolve para IPv6 e o nginx escuta só em IPv4 (sem `listen [::]:80`, que derruba o nginx em máquinas sem IPv6).
- Toda tabela nova do `accounts` precisa de `grant` próprio para o `accounts_app` (sem `ALTER DEFAULT PRIVILEGES`).

## Ambiente do Claude
- **Mac do usuário (a partir da etapa 2):** Mac Intel (`x86_64`), sem Homebrew (não suporta Intel). Sessão do Claude Code local com JDK 21 (Temurin), Docker Desktop, plugins `superpowers` e `mattpocock-skills` (marketplace `claude-plugins-official`, escopo do usuário), graphify 0.9.94 via pip no Python 3.15 com `tree-sitter-sql` e a skill em `~/.claude/skills/graphify`; Node v24.22.1 em `~/.local/node` (com a CLI `bru`) e `gh` 2.103.2 em `~/.local/bin`, ambos no PATH pelo `~/.zshrc`. O `infra/local/.env` (fora do git) guarda o `LOCALSTACK_AUTH_TOKEN`. Push, PRs e board pelo `gh` logado como `capozziV` (escopos `repo`, `workflow`, `project`; credencial no keyring do macOS); o `.git/github-token` da VM não é usado no Mac. Os itens abaixo sobre a nuvem valem só se o trabalho voltar para lá.
- **Nuvem:** JDK 21 e Docker. Maven Central e Docker Hub limitam o IP compartilhado (`429`): usar o init script do Gradle com o espelho `maven-central.storage-download.googleapis.com` e `registry-mirrors: ["https://mirror.gcr.io"]` no `dockerd` (configuração do ambiente, fora do repo).
- **Push pela VM do Mac:** token fine-grained do GitHub em `.git/github-token` (fora do controle de versão), lido por um `credential.helper` configurado só no `.git/config` local. Sem esse arquivo, o push pela VM falha; pela nuvem funciona quando o repositório está conectado à sessão.
- **VM do Mac:** sem Docker, sem acesso ao Docker Desktop do Mac e só com Java 11; serve para editar arquivos da pasta do usuário.
- **LocalStack na nuvem:** o proxy da sessão só escuta em 127.0.0.1, então o LocalStack sai com código 55 (licença) sem um encaminhador TCP e as variáveis `LOCALSTACK_HTTPS_PROXY`/`LOCALSTACK_CA_BUNDLE` do helper; o compose usa um override fora do repo. A porta do proxy muda a cada reinício do container: recriar o encaminhador com a porta atual de `$HTTPS_PROXY`. `actionlint` e `shellcheck` não vêm instalados.
- **Nuvem (etapa 1):** o `dockerd` é iniciado à mão (`dockerd` em segundo plano) e para a cada reinício da sessão; o graphify fica em `~/.local/bin` (`uv tool install "graphifyy[sql]"`).
