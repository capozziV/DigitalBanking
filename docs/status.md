# Status do projeto

> Resumo do projeto, atualizado pela coordenadora ao fim de cada tarefa. Tasks, débito técnico, decisões pendentes e tarefas do usuário ficam no board.

**Última atualização:** 2026-11-22
**Board:** [Digital Banking no GitHub Projects](https://github.com/users/capozziV/projects/2)
**Fase atual:** etapa 9 (frontend do capzzPay) na branch `etapa-9-frontend`, worktree `.claude/worktrees/etapa-9`, ainda sem PR e sem CI. Juntados e revisados em 2026-11-22: #232, #233, #229, #102 (sininho) e os achados dos commits que estavam sem revisão; `main` trazida. Etapa 8 na `main` (PR #205).
**Próximo passo:** onda de correções da revisão da branch inteira ([#235](https://github.com/capozziV/DigitalBanking/issues/235), bloqueia o PR); depois, re-revisão, CI local e PR.

## Handoff da etapa 9 (2026-11-22)
O checkout principal está na `main` 132 commits atrás e com um `docs/status.md` antigo: vale este arquivo, no worktree da etapa. Há mudanças não commitadas no checkout principal (memória de agentes, grafo) e em cerca de 35 worktrees: não descarte sem olhar.

### Decisões do usuário em 2026-11-22
- Tarefa: #232, #233, #229 e o sininho (#102) neste PR, depois a rodada final até o merge. Tudo em paralelo, sem validar com ele o que não tiver dúvida.
- **#231:** os commits novos seguem com o e-mail atual por enquanto; o cartão fica aberto. Não ligar o bloqueio de push com e-mail no GitHub enquanto isso.
- Stack `capzzpay`: pode atualizar com backup (em `.superpowers/backups/`), recriando só os serviços alterados com `--no-deps`, sem tocar em volumes nem no LocalStack, que não tem `PERSISTENCE`.

### Rodada final
1. Feito: `./gradlew build` completo em `56e8f16` (1617 testes, 0 falhas, 0 pulados; e2e com 21 executados). O sininho, juntado depois, só muda `frontend/`.
2. Feito: #232 e #233 conferidos na stack `capzzpay` pela API (19 notificações, 0 com "outro cliente", 0 com id de conta).
3. Seed, smoke e Bruno rodados numa stack isolada (nunca semear a do usuário): smoke verde; dois bugs (saldo do `seed-check` no CI, Bruno sem telefone) em `etapa-9/fix-verificacao`, que também roda o Playwright.
4. Feito: `npm ci`, lint, testes (web 169, BFF 177) e build de `frontend/`; `customer`, `notification`, `web` e `bff` da stack `capzzpay` atualizados. Atualizar `web` e `bff` de novo depois da onda de correções (recarregar o nginx).
5. Revisão da branch inteira: **reprovada**, 3 Critical e 11 Important (#235; relatório e ledger em `.superpowers/sdd/etapa-9-final/`, fora do git). Primeira leva nas branches `etapa-9/rf-{bff,web,edge,docs,kdoc}`; segunda depois da `fix-verificacao` (guia, I6, I7, I10, Playwright sem UUID no extrato). Cada uma passa pela Beatriz antes de juntar.
5a. Andamento da onda: juntados BFF, borda, docs, guia/CI/seed e bônus. Em revisão: `etapa-9/rf-web` (`a56d6d7`). Depois dele: texto do Início sobre o bônus, docs da `Idempotency-Key` (arquitetura e ADR 0005), `Transferência enviada` também no Início do Playwright, rodada completa na stack isolada e atualização de `accounts`, `web` e `bff` na stack do usuário (recarregar o nginx).
6. PR para a `main` (aberto em rascunho para o CI rodar); merge com a aprovação dela e o `ci-ok` verde; cartões #16, #102, #229, #232 e #233 em Done.
Nunca rodados: `seed.sh --remote`, login pelo Cognito real e os jobs `frontend` e `frontend-e2e` no CI.

### Pendências
- Débito: #222, #223, #225, #230 e #236 (menores da revisão final).
- #237 (um bônus por cliente, ADR 0026): revisado e juntado na branch da etapa.
- #234: dados reparados em 2026-11-22. Prevenção aprovada (DynamoDB Local com volume, ADR 0027) em `debito/dynamodb-local` e `debito/dynamodb-local-adr`, PR próprio depois da etapa 9; depois, migrar a stack do usuário com backup (até lá, sem `compose down`).
- #239 decidido: só a retentativa de 1 hora do `statement` (sem alarme, sem retenção maior, sem backup); PR pequeno depois de `debito/dynamodb-local`, que mexe nos mesmos arquivos, e antes do #182. #117 cancelado pelo usuário.
- Tarefas do usuário: #219 (reaplicar o `bootstrap`) e #224 (segredo `DEMO_PASSWORD`) antes do `aws-up`; #182 (subida real na AWS).
- Decisões do usuário: o banco se chama **capzzPay** (pacotes e repositório seguem `digitalbanking`); rodapé "© 2026 capzz.labs"; "Notificações" no lugar de "Avisos"; nome da pessoa, e não id de conta, nas notificações.

### Ambiente que ficou de pé
- Projeto compose `capzzpay` em `http://127.0.0.1`, imagens `etapa9` do commit `f546bf3`, com dados do usuário nos volumes (não apague sem perguntar). Credenciais do demo no `infra/local/.env` do worktree.
- O `up` usa `MOCK_OAUTH2_HOST=mock-oauth2-server:8080 IMAGE_TAG=etapa9`, `-p capzzpay` e `--profile apps`.
- Nesse `.env`, `LOCALSTACK_AUTH_TOKEN` ficou vazio.
- O projeto `local` antigo foi derrubado com autorização do usuário.
- As capturas em `telas/` estão fora do git.

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
- **Etapa 3, mensageria e `notification`** ([spec](superpowers/specs/2026-10-28-etapa-3-mensageria-design.md), [plano](superpowers/plans/2026-10-28-etapa-3-mensageria.md), [ADR 0017](adr/0017-notification-com-banco-e-consumo-sqs.md); PR #114): serviço `notification` (porta 8083, PostgreSQL próprio) com notificações dentro do aplicativo (boas-vindas, troca de tipo de conta, transferência enviada, recebida, recusada e devolvida); consumo com `@SqsListener` em duas filas FIFO com DLQ (5 tentativas, visibilidade 30 s), idempotência por `processed_events`; API `GET /notifications` e `POST /notifications/read`; `AccountTypeChanged` v2 com o limite de cheque especial; relay em `libs/outbox-relay` com espera crescente com o broker fora; lacunas de teste do `transfers` fechadas (#93–#96). Build final: 699 testes, 0 falhas, 0 pulados; smoke e Bruno das três coleções verdes; e2e com os três serviços. Na `main`, seguida dos débitos técnicos dos PRs #130–#132.
- **Etapa 4, `statement`** ([spec](superpowers/specs/2026-11-10-etapa-4-statement-design.md), [plano](superpowers/plans/2026-11-10-etapa-4-statement.md), [ADR 0018](adr/0018-chave-do-extrato-pela-sequencia.md); PR #139): serviço `statement` (porta 8084, DynamoDB sem PostgreSQL) que projeta os lançamentos do `accounts` com `PutItem` condicional pela chave `accountId` + `sequence` (idempotência sem tabela de controle); consumo da fila `statement-accounts.fifo` com DLQ e métrica `statement.dlq.messages`; `GET /accounts/{accountId}/statement` com paginação por cursor opaco; e2e, smoke e Bruno das quatro coleções. Build da revisão final: 791 testes, 0 falhas, 0 pulados. Débitos técnicos #134–#138 no board.
- **Etapa 5, cache do extrato** ([spec](superpowers/specs/2026-11-12-etapa-5-cache-do-extrato-design.md), [plano](superpowers/plans/2026-11-12-etapa-5-cache-do-extrato.md), [ADR 0009](adr/0009-cache-do-extrato.md)): cache-aside em Redis da 1ª página (`limit` 20, sem cursor), chave `statement:first-page:v1:{accountId}`, TTL de 60 s ± 10 s, invalidação ao consumir o lançamento, timeout de 100 ms com fallback para o DynamoDB, métricas `statement.cache.requests`, `writes` e `evictions`. PR #153. Revisão final: 854 testes, 0 falhas, 0 pulados; correções finais com leitura consistente da 1ª página no DynamoDB.
- **Etapa 6, limite diário** ([spec](superpowers/specs/2026-11-12-etapa-6-limite-diario-design.md), [plano](superpowers/plans/2026-11-12-etapa-6-limite-diario.md), [ADR 0010](adr/0010-limite-diario-redis-lua.md)): passo `ReserveLimit` na saga antes do débito; R$ 5.000 por conta por dia de São Paulo; contador em Redis + Lua com reserva idempotente por transferência; fonte da verdade em `transfer.limit_day`/`limit_reserved`; trava única `pg_advisory_xact_lock` por conta e dia nas duas vias; epoch que força a reconstrução depois de reservas pelo PostgreSQL; fallback no PostgreSQL com o Redis fora; `GET /accounts/{id}/daily-limit`; `libs/testing-redis`. Revisão final: 917 testes, 0 falhas, 0 pulados. PR #161. Débitos técnicos #159 e #160 no board.
- **Etapa 7, borda e identidade** ([spec](superpowers/specs/2026-11-13-etapa-7-borda-e-identidade-design.md), [plano](superpowers/plans/2026-11-13-etapa-7-borda-e-identidade.md), ADR 0020): JWT nos cinco serviços com `libs/security` (`Caller` em `libs/security-caller`, rotas negadas por padrão, `401`/`403` em Problem Details, token de máquina com cache) e `libs/observabilidade` (correlation ID); regra de dono (recurso alheio → `404`) e grupo `admin`; serviço `customer` (porta 8085, eventos no `customer-events.fifo`) consultado na hora pelo `accounts` e pelo `transfers` (bloqueado → `422`, fora → `503`); `call-budget` da saga em 6 s; `mock-oauth2-server` local, `seed.sh` do dono, smoke e cinco coleções Bruno com token; Terraform do Cognito e do API Gateway validado sem aplicar. Revisão final: 1200 testes, 0 falhas, 0 pulados.
- **Bloqueio e encerramento de conta** (#27; [spec](superpowers/specs/2026-11-16-bloqueio-e-encerramento-de-conta-design.md), [plano](superpowers/plans/2026-11-16-bloqueio-e-encerramento-de-conta.md), ADR 0023; PR #197): status da conta (Ativa, Bloqueada, Encerrada) conferido sob a trava; rotas de admin; encerramento com saldo zero e sem pendentes, consultando o `transfers` (`GET /internal/accounts/{id}/pending-transfers`); saga trata conta bloqueada ou encerrada devolvendo o limite; notificações de status. Revisão final: 1441 testes, 0 falhas; smoke, Bruno 15/15 e e2e 21/21.
- **Débitos técnicos de 2026-11-16 a 19:** PRs #188 (#170, #56), #190 (#23), #191 (#33, #40, #44), #193 (#159, #160), #199 (#189), #200 (#196, `TransferCompensated` v2 com `reason`).
- **Etapa 8, acabamento** ([spec](superpowers/specs/2026-11-16-etapa-8-acabamento-design.md), [plano](superpowers/plans/2026-11-16-etapa-8-acabamento.md), ADRs 0021 e 0022): imagens Docker em camadas; chart Helm comum e `db-init`; Terraform `bootstrap` (estado, OIDC, ECR, Budget) e `envs/aws` (VPC, EKS 1.36 com nós EC2, ALB interno, borda, RDS, ElastiCache, DynamoDB, SNS/SQS, Pod Identity), tudo validado sem aplicar; workflows `aws-up`, `aws-down` e `aws-reaper` (8 h, falha segura); contratos HTTP e de mensagem com o Stubborn Contract 0.2.2; README com diagrama. Revisão final: 1504 testes verdes, validações de infra e mutantes vermelhos.
- **Board** no GitHub Projects (2026-10-26) e **guia local no Mac** (PR #6).

## Como rodar localmente
Guia completo, para macOS (zsh) e Linux: [`docs/rodando-localmente.md`](rodando-localmente.md). Resumo: JDK 21 e Docker Desktop; `cp infra/local/.env.example infra/local/.env`; compose `up --wait`; `./gradlew build` (ou `./gradlew test` sem Docker); `bootRun`; `infra/local/smoke.sh`; coleção Bruno em `api/bruno/`.

## Convenções que já custaram caro
- Agentes em worktree (`.claude/worktrees/`, fora do git) não regeneram nem commitam `graphify-out/`; a coordenadora regenera o grafo depois de juntar as branches.
- Teste que "passa de primeira" não prova nada: a revisão roda o mutante (desliga a regra) e o teste tem de ficar vermelho. Na etapa 3, o teste de 20 entregas passava sem a deduplicação.
- LocalStack iniciado logo depois de outros pode passar de 60 s para ficar pronto; o apoio de testes usa 3 min de prazo e imprime os logs do contêiner.
- Healthchecks e curls dentro de contêineres usam `127.0.0.1`, nunca `localhost`: no runner do GitHub, `localhost` resolve para IPv6 e o nginx escuta só em IPv4 (sem `listen [::]:80`, que derruba o nginx em máquinas sem IPv6).
- Script novo em `infra/local/localstack/ready.d` quebra os testes de integração de outros serviços que montam a pasta inteira (na etapa 4, o `04-statement.sh` exigiu DynamoDB no LocalStack do `accounts` e do `transfers`): rode o `./gradlew build` completo ao mexer ali.
- Toda tabela nova do `accounts` precisa de `grant` próprio para o `accounts_app` (sem `ALTER DEFAULT PRIVILEGES`).

## Ambiente do Claude
Mac Intel (`x86_64`) do usuário, sem Homebrew. JDK 21 (Temurin), Docker Desktop, plugins `superpowers` e `mattpocock-skills`, graphify via pip (Python 3.15, com `tree-sitter-sql`); Node em `~/.local/node` (com a CLI `bru`) e `gh` em `~/.local/bin`, no PATH pelo `~/.zshrc`. O `LOCALSTACK_AUTH_TOKEN` está exportado no `~/.zshrc` (e também no `infra/local/.env`, fora do git). Push, PRs e board pelo `gh` logado como `capozziV` (escopos `repo`, `workflow`, `project`).
