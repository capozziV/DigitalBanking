# 0013 — Persistência com Spring JDBC (`JdbcClient`)

- **Status:** aceito
- **Data:** 2026-10-22

## Contexto
O `accounts` tem um ledger só de inserções e depende de trava explícita de linha ([ADR 0012](0012-saldo-no-lancamento-e-trava-pessimista.md)). Era preciso escolher a biblioteca de persistência antes da etapa 1 (datasource, driver e Flyway já estão prontos desde a etapa 0).

## Decisão
- O adapter de persistência usa **Spring JDBC com `JdbcClient`**.
- SQL explícito: o `FOR UPDATE` e o `INSERT` da `sequence` ficam visíveis no código.
- Mapeamento manual entre linhas e domínio.
- Erros de SQL são pegos pelos testes de integração com Testcontainers.

## Consequências
- Positivas: o SQL que trava e grava é lido diretamente, sem camada que o esconda; nenhum comportamento implícito de sessão ou cache; o domínio continua sem depender da biblioteca.
- Negativas: mapeamento escrito à mão; sem checagem de tipos no SQL em tempo de compilação, o que torna os testes com banco real indispensáveis.

## Alternativas consideradas
- **JPA/Hibernate:** pensado para entidades mutáveis (dirty checking, cascata); briga com um ledger só de inserções e com a trava explícita, e o `@Version` não seria usado.
- **jOOQ:** SQL com tipos checados, mas a geração de código a partir das migrations exige banco rodando no build, pesando no CI e em cada serviço. Migrar para jOOQ depois não toca no domínio.
