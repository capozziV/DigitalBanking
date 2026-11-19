# 0011 — Contratos de API

- **Status:** aceito
- **Data:** 2026-10-21

## Contexto
Os serviços são consumidos pela borda, entre si e, mais tarde, pelo BFF. É preciso contratos explícitos, erros padronizados e proteção contra transferências duplicadas por retentativa do cliente.

## Decisão
- Cada serviço expõe contrato **OpenAPI**.
- Erros no formato **Problem Details (RFC 9457)**.
- Transferências exigem o header **`Idempotency-Key`**.
- Uma coleção Bruno versionada no repositório (`api/bruno/`) é mantida atualizada para testes manuais.

## Consequências
- Positivas: contratos documentados e verificáveis (testes de contrato com Stubborn Contract, [ADR 0022](0022-testes-de-contrato.md)); tratamento de erro uniforme entre serviços; retentativas de transferência sem efeito duplicado.
- Negativas: contratos e coleção precisam ser mantidos junto com o código; clientes precisam gerar e guardar a `Idempotency-Key`.
