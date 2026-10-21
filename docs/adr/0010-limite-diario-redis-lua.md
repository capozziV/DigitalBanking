# 0010 — Limite diário com Redis + Lua

- **Status:** aceito
- **Data:** 2026-10-21

## Contexto
Transferências têm limite diário. A verificação precisa ser atômica sob concorrência e resistente a retentativas da saga.

## Decisão
- Contador do limite no **Redis**, manipulado por **script Lua** com operações de **reservar** e **liberar**.
- Reserva **idempotente por `transferId`**.
- O **PostgreSQL do `transfers` é a fonte da verdade**; o contador no Redis é **reconstruído** a partir dele.

## Consequências
- Positivas: verificação e reserva atômicas; retentativas não consomem limite duas vezes; perda do Redis é recuperável.
- Negativas: dois lugares com o mesmo dado (Redis e PostgreSQL); exige rotina de reconstrução e liberação na compensação da saga.
