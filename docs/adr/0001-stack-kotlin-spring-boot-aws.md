# 0001 — Stack: Kotlin + Spring Boot 3 na AWS

- **Status:** aceito
- **Data:** 2026-10-21

## Contexto
Backend de banco digital simulado, para portfólio, organizado em microsserviços (customer, accounts, transfers, statement, notification). É preciso uma stack única para todos os serviços, um provedor de nuvem e um ambiente local que reproduza a infraestrutura.

## Decisão
- **Linguagem e framework:** Kotlin + Spring Boot 3, JDK 21.
- **Modelo de execução:** Spring MVC com virtual threads (JDK 21).
- **Build:** Gradle Kotlin DSL multi-módulo, em **monorepo**.
- **AWS:** RDS PostgreSQL, DynamoDB, SNS, SQS + DLQ, ElastiCache (Redis), Cognito, API Gateway (HTTP API), ECS Fargate.
- **Infraestrutura como código:** Terraform.
- **Ambiente local:** docker-compose com PostgreSQL, Redis, LocalStack e nginx (no papel do API Gateway).
- **Testes de integração:** Testcontainers.

## Consequências
- Positivas: uma única stack e um único build para todos os serviços; ambiente local próximo da AWS; testes de integração contra infraestrutura real em contêiner.
- Negativas: o LocalStack não cobre tudo de graça (Cognito é recurso pago, ver ADR 0004); o monorepo exige disciplina para não acoplar serviços (ver ADR 0002).

## Pontos em aberto
Nenhum.

## Alternativas consideradas
- **CDK** (no lugar de Terraform): descartado por decisão do usuário em 2026-10-21.
- **WebFlux com coroutines** (no lugar de Spring MVC com virtual threads): descartado por decisão do usuário em 2026-10-21.
