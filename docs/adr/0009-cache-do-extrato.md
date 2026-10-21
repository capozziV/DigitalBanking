# 0009 — Cache do extrato

- **Status:** aceito
- **Data:** 2026-10-21

## Contexto
A primeira página do extrato é a consulta mais comum. Um cache reduz leituras no DynamoDB, mas não pode servir dados velhos por muito tempo nem derrubar o serviço se falhar.

## Decisão
- **Cache-aside** no Redis só da **primeira página** do extrato.
- **TTL curto com jitter**.
- **Invalidação** ao consumir o evento que altera o extrato.
- **Fallback** para o DynamoDB se o Redis cair.
- Este cache não é usado para autorizar débitos (ver ADR 0007).

## Consequências
- Positivas: menos leituras no DynamoDB na consulta mais comum; jitter evita expiração em massa; indisponibilidade do Redis não derruba o extrato.
- Negativas: janela curta de dado desatualizado entre o evento e a invalidação; lógica extra de invalidação no consumidor.
