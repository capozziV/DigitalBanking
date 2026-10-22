# Roadmap

| Etapa | Entrega | Destaques técnicos |
|---|---|---|
| 0 | Fundação | Monorepo, Gradle Kotlin DSL com `build-logic`, ktlint, ArchUnit, CI, docker-compose (PostgreSQL, Redis, nginx), esqueleto do `accounts` ([spec](superpowers/specs/2026-10-21-etapa-0-fundacao-design.md)) |
| 1 | `accounts` | Ledger imutável, concorrência no saldo, idempotência; tirar a permissão de pacote vazio das regras ArchUnit |
| 2 | `transfers` + saga | Saga orquestrada com compensação, `Idempotency-Key`; build seletivo por serviço no CI (chega o 2º serviço) |
| 3 | Mensageria | Transactional Outbox em `libs/outbox`, SNS/SQS + DLQ, `notification`, emulador AWS no docker-compose |
| 4 | `statement` | CQRS com DynamoDB, `PutItem` condicional |
| 5 | Cache do extrato | Cache-aside da 1ª página, TTL com jitter, invalidação por evento, fallback |
| 6 | Limite diário | Redis + Lua, reserva idempotente, reconstrução a partir do PostgreSQL |
| 7 | Borda e identidade | `customer`, Cognito, API Gateway HTTP API (autorizador JWT + VPC Link), validação JWT nos serviços, correlation ID ponta a ponta em `libs/observabilidade`; restringir detalhes do `/actuator/health` |
| 8 | Acabamento | Testes de contrato, Terraform em `terraform/`, imagem Docker por serviço e deploy no CI, README com diagrama, revisão dos ADRs |
| 9 | Frontend | Angular + BFF em NestJS em `frontend/` (sessão, CSRF, endpoints por tela) |

Ao longo de todas as etapas: contrato OpenAPI e erros em Problem Details em cada serviço, coleção Bruno/Insomnia atualizada para testes manuais.
