# Status do projeto

> Atualizado pelo agente coordenador ao fim de cada tarefa.

**Última atualização:** 2026-10-21
**Fase atual:** planejamento concluído; ADRs 0001–0011 aceitos; nenhum código implementado.
**Próxima etapa:** 0 — Fundação.

## Concluído
- Arquitetura e decisões documentadas em `docs/arquitetura.md`, com links para os ADRs.
- Roadmap definido em `docs/roadmap.md`.
- Estrutura de agentes criada em `.claude/agents/`.
- ADRs 0001–0011 (stack, Clean Architecture, outbox, borda, frontend, observabilidade, ledger, statement, cache do extrato, limite diário, contratos), com índice em `docs/adr/README.md`.
- Decisões de 2026-10-21: Terraform (CDK descartado), ArchUnit (Konsist descartado), Spring MVC com virtual threads (WebFlux descartado).

## Em andamento
- Nada.

## Próximos passos
1. Iniciar etapa 0: estrutura do monorepo, Gradle, lint, CI e docker-compose.

## Pendências e decisões em aberto
Ver seção "Em aberto" em `docs/arquitetura.md`, mais:
- Mecanismo de concorrência no saldo (ADR 0007) — decidir antes da etapa 1.
- Avaliar ADRs próprios para a saga orquestrada com compensação (`transfers`) e para ECS Service Connect.
- "Angular Material ou PrimeNG" aparece em duas seções de `arquitetura.md`; consolidar na etapa 9.

## Bloqueios
- O ambiente de execução dos agentes tem JDK 11 e não tem Docker. A etapa 0 exige JDK 21 (instalar) e Docker para docker-compose/Testcontainers (rodar no Mac do usuário).
