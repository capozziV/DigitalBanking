# Roadmap

| Etapa | Entrega | Destaques técnicos |
|---|---|---|
| 0 | Fundação | Monorepo, Gradle Kotlin DSL com `build-logic`, ktlint, ArchUnit, CI, docker-compose (PostgreSQL, Redis, nginx), esqueleto do `accounts` ([spec](superpowers/specs/2026-10-21-etapa-0-fundacao-design.md)) |
| 1 | `accounts` | Ledger imutável, concorrência no saldo, cheque especial por tipo de conta, idempotência; Transactional Outbox em `libs/outbox` com relay e SNS FIFO, LocalStack no docker-compose e no CI; tirar a permissão de pacote vazio das regras ArchUnit ([spec](superpowers/specs/2026-10-22-etapa-1-accounts-design.md)) |
| 2 | `transfers` + saga | Saga orquestrada com compensação, `Idempotency-Key`; build seletivo por serviço no CI (chega o 2º serviço) |
| 3 | Mensageria | Filas SQS + DLQ por consumidor, `notification`, consumidores idempotentes (outbox, relay, SNS e LocalStack já vêm da etapa 1) |
| 4 | `statement` | CQRS com DynamoDB, `PutItem` condicional; listagem de lançamentos (extrato) |
| 5 | Cache do extrato | Cache-aside da 1ª página, TTL com jitter, invalidação por evento, fallback |
| 6 | Limite diário | Redis + Lua, reserva idempotente, reconstrução a partir do PostgreSQL |
| 7 | Borda e identidade | `customer`, Cognito, API Gateway HTTP API (autorizador JWT + VPC Link), validação JWT nos serviços, correlation ID ponta a ponta em `libs/observabilidade` (recebe o filtro mínimo criado no `accounts` na etapa 1); proteger `/admin` com grupo de administradores; restringir detalhes do `/actuator/health` |
| 8 | Acabamento | Testes de contrato, Terraform em `terraform/`, imagem Docker por serviço e deploy no CI, README com diagrama, revisão dos ADRs |
| 9 | Frontend | Angular + BFF em NestJS em `frontend/` (sessão, CSRF, endpoints por tela) |

Ao longo de todas as etapas: contrato OpenAPI e erros em Problem Details em cada serviço, coleção Bruno/Insomnia atualizada para testes manuais.
