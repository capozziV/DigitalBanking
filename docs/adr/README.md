# ADRs

Registros de decisões arquiteturais. Formato: `NNNN-titulo-em-kebab-case.md`.

| Nº | Título | Status | Arquivo |
|---|---|---|---|
| 0001 | Stack: Kotlin + Spring Boot 4 na AWS | aceito | [0001](0001-stack-kotlin-spring-boot-aws.md) |
| 0002 | Clean Architecture por serviço | aceito | [0002](0002-clean-architecture-por-servico.md) |
| 0003 | Transactional Outbox e mensageria SNS/SQS | aceito | [0003](0003-transactional-outbox-e-mensageria.md) |
| 0004 | API Gateway HTTP API na borda | aceito | [0004](0004-api-gateway-http-api-na-borda.md) |
| 0005 | Frontend Angular e BFF em NestJS | aceito | [0005](0005-frontend-angular-e-bff-nestjs.md) |
| 0006 | Observabilidade só com correlation ID | aceito | [0006](0006-observabilidade-com-correlation-id.md) |
| 0007 | Conta e ledger no mesmo serviço (`accounts`) | aceito | [0007](0007-conta-e-ledger-no-accounts.md) |
| 0008 | Extrato (`statement`) com CQRS no DynamoDB | aceito | [0008](0008-statement-cqrs-dynamodb.md) |
| 0009 | Cache do extrato | aceito | [0009](0009-cache-do-extrato.md) |
| 0010 | Limite diário com Redis + Lua | aceito | [0010](0010-limite-diario-redis-lua.md) |
| 0011 | Contratos de API | aceito | [0011](0011-contratos-de-api.md) |
| 0012 | Saldo no lançamento e trava pessimista por conta | aceito | [0012](0012-saldo-no-lancamento-e-trava-pessimista.md) |
| 0013 | Persistência com Spring JDBC (`JdbcClient`) | aceito | [0013](0013-persistencia-com-spring-jdbc.md) |
| 0014 | Tipo de conta e cheque especial | aceito | [0014](0014-tipo-de-conta-e-cheque-especial.md) |
| 0015 | Relay do outbox com líder único e SNS FIFO | aceito | [0015](0015-outbox-relay-lider-e-sns-fifo.md) |
| 0016 | Saga de transferência orquestrada por HTTP síncrono | aceito | [0016](0016-saga-orquestrada-por-http.md) |
| 0017 | `notification` com banco próprio e consumo SQS com Spring Cloud AWS | aceito | [0017](0017-notification-com-banco-e-consumo-sqs.md) |
| 0018 | Extrato ordenado pela sequência do ledger | aceito | [0018](0018-chave-do-extrato-pela-sequencia.md) |
| 0019 | Runtime na AWS com EKS (substitui parcialmente o 0001 e o 0004) | aceito | [0019](0019-runtime-na-aws-com-eks.md) |
