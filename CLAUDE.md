# Banco digital em microsserviços

Backend de banco digital **simulado** para portfólio. Sem dinheiro real e sem dados pessoais reais.
O valor do projeto está em resolver bem consistência, idempotência, mensageria confiável, cache e observabilidade.

Responder sempre em português.

## Contexto (leia antes de qualquer tarefa)
- @docs/status.md — onde estamos agora
- @docs/arquitetura.md — stack, serviços e decisões
- @docs/roadmap.md — etapas do projeto
- ADRs em `docs/adr/`

## Regras inegociáveis
- Valores monetários sempre em centavos (`Long`/inteiro), nunca `Double`/float.
- Clean Architecture por serviço: `domain` → `application` → `adapters` → `bootstrap`. O `domain` não depende de Spring nem de infraestrutura (validado com ArchUnit).
- Não compartilhar classes de domínio entre serviços. Bibliotecas compartilhadas só de infraestrutura (outbox, observabilidade).
- Saldo para autorizar débito nunca vem de cache.
- Eventos só via Transactional Outbox; consumidores sempre idempotentes.
- Toda decisão arquitetural relevante vira um ADR curto em `docs/adr/`.
- Antes de considerar uma tarefa pronta: rodar build, testes e lint e informar o resultado.
- Se faltar algo necessário (acesso, segredo, ferramenta, decisão), dizer o que falta e parar. Nunca simular nem adivinhar.

## Ao terminar uma tarefa
Atualizar `docs/status.md` (o coordenador faz isso; especialistas reportam o que mudou).
