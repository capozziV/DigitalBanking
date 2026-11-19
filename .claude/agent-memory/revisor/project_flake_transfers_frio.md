---
name: flake-transfers-chamada-fria-accounts
description: IT do accounts que fecha conta flaka com 503 transfers-unavailable na 1ª chamada fria ao transfers (timeout 500 ms)
metadata:
  type: project
---

Timeout de 500 ms (`accounts.transfers.timeout`) estoura na 1ª chamada ao WireMock `TransfersServiceStub` por contexto Spring (JDK HttpClient e Jetty frios). Quem fechar conta primeiro na JVM leva 503. Visto em `AccountOwnLookupApiIntegrationTest` (etapa 9 task 2) e reproduzido em `AccountClosingConcurrencyIntegrationTest` (rodada 3 do módulo, 2026-11-19). O stub não é compartilhado com estado: respostas são por accountId.

**Why:** flake pré-existente da infra de teste, não do teste novo.
**How to apply:** não bloquear task por isso; correção sugerida em `AccountsIntegrationTest` (timeout maior no TestPropertySource ou aquecer a chamada). Ver [[project_gate_revisao_task]].
