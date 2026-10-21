# Status do projeto

> Atualizado pelo agente coordenador ao fim de cada tarefa.

**Última atualização:** 2026-10-21
**Fase atual:** planejamento concluído; ADRs 0001–0011 aceitos; nenhum código implementado. Decisões da etapa 0 tomadas; falta o plano.
**Próxima etapa:** 0 — Fundação.

## Concluído
- Arquitetura e decisões documentadas em `docs/arquitetura.md`, com links para os ADRs.
- Roadmap definido em `docs/roadmap.md`.
- Estrutura de agentes criada em `.claude/agents/`.
- ADRs 0001–0011 (stack, Clean Architecture, outbox, borda, frontend, observabilidade, ledger, statement, cache do extrato, limite diário, contratos), com índice em `docs/adr/README.md`.
- Revisão com os plugins Superpowers e Matt Pocock: glossário `CONTEXT.md`; ADRs sem seções vazias; pendências consolidadas em "Em aberto" de `docs/arquitetura.md`; `CLAUDE.md` e agentes apontam para os skills de plano, TDD, debugging, verificação, domain modeling e code review.
- Decisões de 2026-10-21: Terraform (CDK descartado), ArchUnit (Konsist descartado), Spring MVC com virtual threads (WebFlux descartado), Spring Boot 4.1 (3.5 fora de suporte, ADR 0001 revisado), lint com ktlint (detekt descartado por ainda estar em alpha para Kotlin 2.x).

## Em andamento
- Nada.

## Próximos passos
1. Gerar o plano da etapa 0 com `superpowers:writing-plans` e revisar com o usuário.
2. Executar a etapa 0: estrutura do monorepo, Gradle, lint, CI e docker-compose.

## Pendências e decisões em aberto
Fonte única: seção "Em aberto" de `docs/arquitetura.md`.

## Bloqueios
- Ambiente de nuvem (testado em 2026-10-21 19:25): JDK 21 instalado, `dockerd` sobe como root, e `repo1.maven.org`, `services.gradle.org` e `plugins.gradle.org` estão liberados. Gradle destravado.
- Docker na nuvem: pull do Docker Hub funcionou às 19:29 depois de um `429 Too Many Requests` intermitente (limite anônimo por IP compartilhado). `mirror.gcr.io` está acessível; falta configurá-lo como `registry-mirrors` no `dockerd` e validar as imagens da etapa 0 (postgres, redis, localstack, nginx).
- VM local (Mac): sem Docker e sem acesso ao Docker Desktop do Mac. Não usar para build.
