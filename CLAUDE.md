# Banco digital em microsserviços

Backend de banco digital **simulado** para portfólio. Sem dinheiro real e sem dados pessoais reais.
O valor do projeto está em resolver bem consistência, idempotência, mensageria confiável, cache e observabilidade.

Responder sempre em português.

## Contexto (leia antes de qualquer tarefa)
- @docs/status.md — onde estamos agora, bloqueios e condições do ambiente
- @docs/arquitetura.md — stack, serviços, decisões e a lista "Em aberto"
- @docs/roadmap.md — etapas do projeto
- @CONTEXT.md — linguagem do domínio; use esses termos em classes, eventos, testes e docs
- ADRs em `docs/adr/` — o porquê de cada decisão

## Regras inegociáveis
- Valores monetários sempre em centavos (`Long`/inteiro), nunca `Double`/float.
- Clean Architecture por serviço: `domain` → `application` → `adapters` → `bootstrap`. O `domain` não depende de Spring nem de infraestrutura (validado com ArchUnit).
- Não compartilhar classes de domínio entre serviços. Bibliotecas compartilhadas só de infraestrutura (outbox, observabilidade).
- Saldo para autorizar débito nunca vem de cache.
- Eventos só via Transactional Outbox; consumidores sempre idempotentes.
- Decisão difícil de reverter, surpreendente sem contexto e fruto de um trade-off real vira um ADR curto em `docs/adr/`. Pendências ficam só na seção "Em aberto" de `docs/arquitetura.md`.
- Se faltar algo necessário (acesso, segredo, ferramenta, decisão), dizer o que falta e parar. Nunca simular nem adivinhar.

## Fluxo de trabalho (skills)
Todo trabalho no projeto passa pelos skills dos plugins **Superpowers** e **Matt Pocock**, sessão principal e agentes. Antes de agir, identifique a situação abaixo e carregue o skill dela com a ferramenta `Skill`; um skill por situação, sem alternar entre equivalentes dos dois plugins.

| Situação | Skill |
|---|---|
| Ideia, feature ou etapa nova (antes de qualquer plano) | `superpowers:brainstorming` |
| Spec aprovada → plano em `docs/superpowers/plans/` | `superpowers:writing-plans` |
| Executar plano com agentes | `superpowers:subagent-driven-development` |
| Executar plano na sessão | `superpowers:executing-plans` |
| Trabalho isolado em branch | `superpowers:using-git-worktrees` |
| Código de produção | `superpowers:test-driven-development` |
| Bug, teste falhando, comportamento estranho | `superpowers:systematic-debugging` |
| Interface de módulo, onde fica a fronteira, testabilidade | `mattpocock-skills:codebase-design` |
| Dúvida de modelagem que só um protótipo responde | `mattpocock-skills:prototype` |
| Termo novo do domínio ou candidato a ADR | `mattpocock-skills:domain-modeling` |
| Item de "Em aberto" a decidir | `mattpocock-skills:grilling` |
| Pesquisa em fontes primárias (docs, APIs, versões) | `mattpocock-skills:research` |
| Tarefas independentes em paralelo | `superpowers:dispatching-parallel-agents` |
| Revisão de branch | `mattpocock-skills:code-review` |
| Receber feedback de revisão | `superpowers:receiving-code-review` |
| Conflito de merge/rebase | `mattpocock-skills:resolving-merge-conflicts` |
| Antes de declarar pronto | `superpowers:verification-before-completion` — build, testes e lint rodados, com o resultado real no relato |
| Fechar a branch (merge, PR) | `superpowers:finishing-a-development-branch` |
| Editar `CLAUDE.md`, agentes ou skills | `mattpocock-skills:writing-for-agents` |

## Agentes do projeto
Quem executa cada papel do fluxo acima são os agentes de `.claude/agents/`, chamados pelo nome no `Agent`:

| Papel | Agente |
|---|---|
| Implementar task de código Kotlin (domínio, casos de uso, adapters, migrations, testes) | `dev-kotlin` |
| Implementar task de build, `build-logic`, docker-compose, nginx, CI, Terraform | `infra` |
| Implementar task de frontend e BFF (etapa 9) | `dev-frontend` |
| Revisar task, re-revisar correção, revisar a branch | `revisor` |
| ADR, `CONTEXT.md`, README, docs de arquitetura | `documentador` |
| Coordenar, delegar, manter `docs/status.md` | sessão principal (`coordenador`) |

- Task que mistura papéis vai para o agente da maior parte do diff; o prompt cita o resto.
- O modelo de cada despacho segue a seleção de modelo do `superpowers:subagent-driven-development`; informe-o sempre.
- Os agentes só existem quando o repositório está conectado à sessão (clone do GitHub). Sem eles, diga isso e use um agente genérico com o arquivo do agente como parte do prompt.

## Git
Commits e pushes saem só com o autor do usuário (capozziV <vitorcapozzi4@gmail.com>), sem trailers `Co-Authored-By` ou `Claude-Session`.

## Ao terminar uma tarefa
Atualizar `docs/status.md` (o coordenador faz isso; especialistas reportam o que mudou).
