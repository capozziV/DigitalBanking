# Roadmap

| Etapa | Entrega | Destaques técnicos |
|---|---|---|
| 0 | Fundação | Monorepo, Gradle Kotlin DSL, lint, CI, docker-compose (PostgreSQL, Redis, LocalStack, nginx) |
| 1 | `accounts` | Ledger imutável, concorrência no saldo, idempotência |
| 2 | `transfers` + saga | Saga orquestrada com compensação, `Idempotency-Key` |
| 3 | Mensageria | Transactional Outbox, SNS/SQS + DLQ, `notification` |
| 4 | `statement` | CQRS com DynamoDB, `PutItem` condicional |
| 5 | Cache do extrato | Cache-aside da 1ª página, TTL com jitter, invalidação por evento, fallback |
| 6 | Limite diário | Redis + Lua, reserva idempotente, reconstrução a partir do PostgreSQL |
| 7 | Borda e identidade | `customer`, Cognito, API Gateway HTTP API (autorizador JWT + VPC Link), validação JWT nos serviços, correlation ID ponta a ponta |
| 8 | Acabamento | Testes de contrato, Terraform, README com diagrama, revisão dos ADRs |
| 9 | Frontend | Angular + BFF em NestJS (sessão, CSRF, endpoints por tela) |

Ao longo de todas as etapas: contrato OpenAPI e erros em Problem Details em cada serviço, coleção Bruno/Insomnia atualizada para testes manuais.
