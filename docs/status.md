# Status do projeto

> Resumo do projeto, atualizado pela coordenadora ao fim de cada tarefa. Tasks, débito técnico, decisões pendentes e tarefas do usuário ficam no board.

**Última atualização:** 2026-10-28
**Board:** [Digital Banking no GitHub Projects](https://github.com/users/capozziV/projects/2)
**Fase atual:** etapa 2 concluída (PRs #91 e #97 na `main`, CI verde).
**Próximo passo:** etapa 3 (mensageria: filas SQS + DLQ, `notification`, consumidores idempotentes), começando pelo brainstorming (cartão #10).

## Como usar o board
- Colunas: **Backlog** (pendência sem resposta, ou etapa anterior fora de Done), **Ready** (sem pendência e etapa anterior em Done), **In progress**, **In review** (revisão da Beatriz) e **Done** (revisado, build, testes e lint verdes, merge feito).
- Campos: Stage (0–9), Service, Item type (Stage, Task, Tech debt, Decision) e Priority.
- Cada etapa é um cartão (`Stage N · …`), bloqueado pela anterior; tasks e débito técnico da etapa são sub-issues dele.
- Cada issue tem a seção "Open questions" com a checklist das pendências. A resposta do usuário fica anotada e o item marcado; pendência nova devolve o cartão ao Backlog.
- O board é em inglês. Documentação, ADRs e commits seguem em português.

## Concluído
Detalhes de cada etapa na spec, no plano e nos PRs.
- **Planejamento:** arquitetura, roadmap, ADRs 0001–0015, `CONTEXT.md`, agentes do projeto e fluxo com os plugins Superpowers e Matt Pocock.
- **Etapa 0, fundação** ([spec](superpowers/specs/2026-10-21-etapa-0-fundacao-design.md), [plano](superpowers/plans/2026-10-21-etapa-0-fundacao.md); PR #1): Gradle com `build-logic` e ktlint, ArchUnit, esqueleto do `accounts`, docker-compose, CI.
- **Etapa 1, `accounts`** ([spec](superpowers/specs/2026-10-22-etapa-1-accounts-design.md), [plano](superpowers/plans/2026-10-22-etapa-1-accounts.md); PRs #2–#4): ledger imutável com trava pessimista, idempotência por chave, cheque especial por tipo, Problem Details, correlation ID, OpenAPI; outbox com relay líder e SNS FIFO no LocalStack (ADRs 0012–0015).
- **Etapa 2, `transfers`** ([spec](superpowers/specs/2026-10-27-etapa-2-transfers-design.md), [plano](superpowers/plans/2026-10-27-etapa-2-transfers.md), [ADR 0016](adr/0016-saga-orquestrada-por-http.md); PRs #91 e #97): saga orquestrada por HTTP com compensação e passos no padrão State, arrendamento, retomada pelo agendador, eventos no `transfers-events.fifo`; relay em `libs/outbox-relay`, regras ArchUnit em `libs/testing-architecture`, CI seletivo com o job `ci-ok` (exigido pela proteção da `main`), testes ponta a ponta em `tests/e2e`. Último build: 533 testes, 0 falhas, 0 pulados.
- **Board** no GitHub Projects (2026-10-26) e **guia local no Mac** (PR #6).

## Como rodar localmente
Guia completo, para macOS (zsh) e Linux: [`docs/rodando-localmente.md`](rodando-localmente.md). Resumo: JDK 21 e Docker Desktop; `cp infra/local/.env.example infra/local/.env`; compose `up --wait`; `./gradlew build` (ou `./gradlew test` sem Docker); `bootRun`; `infra/local/smoke.sh`; coleção Bruno em `api/bruno/`.

## Convenções que já custaram caro
- Agentes em worktree (`.claude/worktrees/`, fora do git) não regeneram nem commitam `graphify-out/`; a coordenadora regenera o grafo depois de juntar as branches.
- Healthchecks e curls dentro de contêineres usam `127.0.0.1`, nunca `localhost`: no runner do GitHub, `localhost` resolve para IPv6 e o nginx escuta só em IPv4 (sem `listen [::]:80`, que derruba o nginx em máquinas sem IPv6).
- Toda tabela nova do `accounts` precisa de `grant` próprio para o `accounts_app` (sem `ALTER DEFAULT PRIVILEGES`).

## Ambiente do Claude
Mac Intel (`x86_64`) do usuário, sem Homebrew. JDK 21 (Temurin), Docker Desktop, plugins `superpowers` e `mattpocock-skills`, graphify via pip (Python 3.15, com `tree-sitter-sql`); Node em `~/.local/node` (com a CLI `bru`) e `gh` em `~/.local/bin`, no PATH pelo `~/.zshrc`. O `LOCALSTACK_AUTH_TOKEN` está exportado no `~/.zshrc` (e também no `infra/local/.env`, fora do git). Push, PRs e board pelo `gh` logado como `capozziV` (escopos `repo`, `workflow`, `project`).
