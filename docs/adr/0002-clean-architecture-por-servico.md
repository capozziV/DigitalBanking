# 0002 — Clean Architecture por serviço

- **Status:** aceito
- **Data:** 2026-10-21

## Contexto
Os serviços vivem no mesmo monorepo e tratam regras sensíveis (ledger, saldo, limite, saga). É preciso isolar regra de negócio de framework e infraestrutura e evitar acoplamento entre serviços.

## Decisão
- Cada serviço é organizado em camadas: `domain` → `application` → `adapters` → `bootstrap`.
- O `domain` não depende de Spring nem de infraestrutura.
- As regras de dependência entre camadas são validadas por teste automatizado com **ArchUnit**.
- Classes de domínio **não** são compartilhadas entre serviços.
- Bibliotecas compartilhadas só de infraestrutura (ex.: outbox, observabilidade).

## Consequências
- Positivas: domínio testável sem Spring; troca de adaptadores sem tocar em regra de negócio; violações de camada quebram o build em vez de passarem em revisão.
- Negativas: mais módulos e mapeamentos entre camadas; conceitos parecidos (ex.: identificadores, valores) são duplicados entre serviços em vez de reaproveitados.

## Alternativas consideradas
- **Konsist** (no lugar de ArchUnit): descartado por decisão do usuário em 2026-10-21.
