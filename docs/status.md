# Status do projeto

> Atualizado pelo agente coordenador ao fim de cada tarefa.

**Última atualização:** 2026-10-21
**Fase atual:** etapa 0 (fundação) pronta no PR #1 (`etapa-0-fundacao` → `main`), com CI verde; falta o merge.
**Próxima etapa:** 1 — `accounts` (decidir antes: persistência e concorrência no saldo).

## Concluído
- Planejamento: arquitetura, roadmap, ADRs 0001–0011, glossário `CONTEXT.md`, agentes e fluxo com os plugins Superpowers e Matt Pocock; papéis do fluxo mapeados para os agentes do projeto no `CLAUDE.md`, com nomes: Helena (coordenação), Marina (`dev-kotlin`), Rafael (`infra`), Lucas (`dev-frontend`), Beatriz (`revisor`), Clara (`documentador`).
- Código Kotlin em inglês, com os nomes de código do glossário `CONTEXT.md`; docs e commits em português.
- Decisões de 2026-10-21: Terraform, ArchUnit, Spring MVC com virtual threads, Spring Boot 4.1, ktlint, um módulo Gradle por camada, pacote raiz `io.github.capozziv.digitalbanking`, LocalStack fora da etapa 0.
- Etapa 0 ([spec](superpowers/specs/2026-10-21-etapa-0-fundacao-design.md), [plano](superpowers/plans/2026-10-21-etapa-0-fundacao.md)), cada task revisada:
  - build: Gradle 9.8.2, catálogo de versões, `build-logic` com 3 plugins de convenção, ktlint;
  - regras ArchUnit no `bootstrap`;
  - `accounts` com Spring Boot 4.1.1, virtual threads, Flyway e teste Testcontainers do `/actuator/health` (db `UP`);
  - docker-compose em `infra/local/` (PostgreSQL 17, Redis, nginx);
  - CI no GitHub Actions (jobs `build` e `smoke`).
- Verificação (ambiente de nuvem, 2026-10-21): `./gradlew build --rerun-tasks` verde (5 testes, 0 falhas); compose saudável com e sem `--env-file`; rota `/accounts/actuator/health` pelo nginx `UP`; actionlint sem erros.
- CI no GitHub (PR #1, 2026-10-21): `build` e `smoke` verdes. O `smoke` falhou na primeira execução porque o healthcheck do nginx usava `localhost`, que no runner resolve para IPv6 enquanto o nginx escuta só em IPv4; corrigido para `127.0.0.1` (sem `listen [::]:80`, que derruba o nginx em máquinas sem IPv6).

## Como rodar localmente
```bash
./gradlew build                                                    # compila, ktlint, testes (precisa de Docker)
docker compose -f infra/local/docker-compose.yml up --wait         # postgres, redis, nginx
./gradlew :services:accounts:bootstrap:bootRun                     # app na porta 8081
curl http://localhost/accounts/actuator/health                     # pelo nginx
docker compose -f infra/local/docker-compose.yml down -v           # derruba e apaga os dados
```
- Valores de desenvolvimento em `infra/local/.env.example`; para mudar, copie para `infra/local/.env` e use `--env-file infra/local/.env`.
- Se mudar `POSTGRES_PORT`, ajuste também `ACCOUNTS_DB_URL` para a aplicação.
- O script de init do PostgreSQL só roda com volume novo: ao adicionar um serviço, rode `down -v`.

## Em andamento
- Nada.

## Próximos passos
1. Usuário: fazer o merge do PR #1 na `main`.
2. Decidir os itens "Antes da etapa 1" de `docs/arquitetura.md` (persistência, concorrência no saldo).
3. Etapa 1 pelo fluxo dos skills (brainstorming → spec → plano).

## Pendências e decisões em aberto
Decisões: fonte única na seção "Em aberto" de `docs/arquitetura.md`.

Melhorias técnicas adiadas na revisão da etapa 0 (nenhuma bloqueia):
- **Etapa 1:** tirar `allowEmptyShould` e ver cada regra ArchUnit falhar; separar as duas direções web↔persistence em testes próprios; decidir se haverá source set `integrationTest` (hoje `AccountsApplicationIT` roda no `test`); trocar o cast do teste de health por asserção AssertJ de mapa; teste de virtual threads hoje só lê a propriedade.
- **Etapa 2 (2º serviço):** `repositoriesMode = FAIL_ON_PROJECT_REPOS`; avaliar remover `pluginManagement.repositories` da raiz (só `includeBuild`); extrair o `val libs = ...` repetido nos plugins de convenção se crescer; documentar o fluxo de adicionar banco no init; init com senha via `psql -v` e idempotente; volume nomeado para o PostgreSQL.
- **Etapa 7:** restringir `management.endpoint.health.show-details` (hoje `always`, sem autenticação); headers `X-Forwarded-Proto`/`X-Real-IP` no nginx.
- **Etapa 8:** nome do jar por serviço (`accounts.jar`), smoke reaproveitando o jar do job `build`, step `if: failure()` no smoke com `docker compose logs` e o `State.Health` dos contêineres, detectar morte do processo no laço do smoke, `cancel-in-progress` só em PRs, actions fixadas por SHA, shellcheck nos blocos `run`.
- **Quando incomodar:** ruído de CDS da JVM nos testes (agente do Mockito); `webmvc` explícito no `bootstrap`. Healthchecks e curls dentro de contêineres: usar `127.0.0.1`, nunca `localhost` (IPv6 no runner).

## Com o usuário (fora do código)
- Abrir PRs (a CLI do GitHub da sessão não tem login; o push funciona).
- Proteger a `main` no GitHub exigindo CI verde para merge.

## Ambiente do Claude
- **Nuvem:** JDK 21 e Docker. Maven Central e Docker Hub limitam o IP compartilhado (`429`): usar o init script do Gradle com o espelho `maven-central.storage-download.googleapis.com` e `registry-mirrors: ["https://mirror.gcr.io"]` no `dockerd` (configuração do ambiente, fora do repo).
- **Push:** funciona pela nuvem quando o repositório está conectado à sessão (clone de `github.com/capozziV/digitalbanking`); a VM do Mac não tem credencial do GitHub.
- **VM do Mac:** sem Docker e sem acesso ao Docker Desktop do Mac; serve para editar arquivos da pasta do usuário.
