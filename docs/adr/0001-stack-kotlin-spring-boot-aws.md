# 0001 — Stack: Kotlin + Spring Boot 4 na AWS

- **Status:** aceito
- **Data:** 2026-10-21

## Contexto
Backend de banco digital simulado, para portfólio, organizado em microsserviços (customer, accounts, transfers, statement, notification). É preciso uma stack única para todos os serviços, um provedor de nuvem e um ambiente local que reproduza a infraestrutura.

## Decisão
- **Linguagem e framework:** Kotlin + Spring Boot 4.1, JDK 21. (Revisado em 2026-10-21: a linha 3 saiu do suporte open source em 2026-06-30.)
- **Modelo de execução:** Spring MVC com virtual threads (JDK 21).
- **Build:** Gradle Kotlin DSL multi-módulo, em **monorepo**.
- **AWS:** RDS PostgreSQL, DynamoDB, SNS, SQS + DLQ, ElastiCache (Redis), Cognito, API Gateway (HTTP API), ECS Fargate.
- **Infraestrutura como código:** Terraform.
- **Ambiente local:** docker-compose com PostgreSQL, Redis, LocalStack e nginx (no papel do API Gateway). LocalStack escolhido em 2026-10-22, com conta gratuita e `LOCALSTACK_AUTH_TOKEN` (local e segredo no CI), a partir da etapa 1 ([ADR 0015](0015-outbox-relay-lider-e-sns-fifo.md)).
- **Testes de integração:** Testcontainers.

## Consequências
- Positivas: uma única stack e um único build para todos os serviços; ambiente local próximo da AWS; testes de integração contra infraestrutura real em contêiner.
- Negativas: o LocalStack não cobre tudo de graça (Cognito é recurso pago, ver ADR 0004); o monorepo exige disciplina para não acoplar serviços (ver ADR 0002).

## Alternativas consideradas
- **CDK** (no lugar de Terraform): descartado por decisão do usuário em 2026-10-21.
- **Spring Boot 3.5:** descartado em 2026-10-21 por ter saído do suporte open source.
- **WebFlux com coroutines** (no lugar de Spring MVC com virtual threads): descartado por decisão do usuário em 2026-10-21.
