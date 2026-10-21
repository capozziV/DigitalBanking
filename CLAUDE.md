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
- Etapa nova do roadmap: `superpowers:writing-plans` gera o plano em `docs/superpowers/plans/`; execução com `superpowers:subagent-driven-development`.
- Código de produção: `superpowers:test-driven-development` (teste vermelho antes do código).
- Bug ou teste falhando: `superpowers:systematic-debugging`.
- Termo novo do domínio ou candidato a ADR: `mattpocock-skills:domain-modeling` (atualiza `CONTEXT.md`, aplica o critério de ADR).
- Item de "Em aberto" para decidir: `mattpocock-skills:grilling` com o usuário antes de registrar a decisão.
- Revisão de branch: `mattpocock-skills:code-review` (padrões deste arquivo + plano da etapa).
- Antes de declarar pronto: `superpowers:verification-before-completion` — build, testes e lint rodados, com o resultado real no relato.

## Git
Commits e pushes saem só com o autor do usuário (capozziV <vitorcapozzi4@gmail.com>), sem trailers `Co-Authored-By` ou `Claude-Session`.

## Ao terminar uma tarefa
Atualizar `docs/status.md` (o coordenador faz isso; especialistas reportam o que mudou).
