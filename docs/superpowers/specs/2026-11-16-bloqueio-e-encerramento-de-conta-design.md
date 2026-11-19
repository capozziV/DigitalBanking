# Bloqueio e encerramento de conta: design

> Cartão #27. Desenho aprovado pelo usuário em 2026-11-16.

## Objetivo
O cliente pode encerrar a própria conta, e o admin pode bloquear uma conta. O ledger nunca é apagado. O frontend (etapa 9) decide depois como isso aparece.

## Decisões (usuário, 2026-11-16)
- Bloqueio e encerramento entram agora.
- Depois de encerrar, o cliente pode abrir uma conta nova: a regra passa a ser **uma conta não encerrada por cliente**.
- Conta bloqueada **não envia** (débitos e transferências de saída), mas **recebe** créditos.

## Abordagens consideradas
- **Status na conta** (escolhida): campo `status` (`ACTIVE`, `BLOCKED`, `CLOSED`) na conta do `accounts`, conferido na mesma transação do débito e do crédito, que já trava a conta. O `transfers` não muda: recusa de débito já vira `FAILED` e recusa de crédito já vira compensação.
- Descartada: status só no `customer` (o bloqueio do cliente já existe na etapa 7, mas é por pessoa, não por conta, e não cobre encerramento).

## Desenho
- **Status da conta** (termo novo no `CONTEXT.md`, código `AccountStatus`): `ACTIVE`, `BLOCKED`, `CLOSED`.
- **Bloqueio** (só admin): `PUT /admin/accounts/{id}/status` com `{"status":"BLOCKED"}` ou `{"status":"ACTIVE"}`; cliente e serviço → `403`. `CLOSED` não entra por essa rota; conta encerrada → `422 account-closed`.
  - Débito em conta bloqueada → `422 account-blocked`; a transferência termina `FAILED` sem tirar valor.
  - Crédito em conta bloqueada é aceito.
  - Eventos `AccountBlocked` e `AccountUnblocked` pelo outbox; notificações "Conta bloqueada" e "Conta desbloqueada".
- **Encerramento** (cliente na própria conta, ou admin): `POST /accounts/{id}/close`.
  - Exige saldo exatamente zero, senão `422 balance-not-zero`.
  - Transferência `PENDING` saindo da conta impede o encerramento (`422 pending-transfers`). Como o `accounts` não conhece as transferências, ele consulta o `transfers` (`GET /internal/accounts/{id}/pending-transfers`, token de serviço) antes de encerrar; `transfers` fora → `503 transfers-unavailable`. *(Escolha da coordenadora.)*
  - Sem volta. Ledger e extrato continuam consultáveis.
  - Conta encerrada recusa débito (`422 account-closed`) e crédito (`422 account-closed`); um crédito recusado na saga vira compensação, e a transferência termina `COMPENSATED`.
  - Conta bloqueada pode ser encerrada se o saldo for zero.
  - Evento `AccountClosed`; notificação "Conta encerrada".
- **Conta nova:** a unicidade de `customer_id` passa a valer só para contas não encerradas (índice único parcial `WHERE status <> 'CLOSED'`). Conta bloqueada conta como não encerrada.
- **Leitura:** `GET /accounts/{id}` devolve o `status`. Saldo, extrato e notificações continuam disponíveis em qualquer status.

## Onde mexe
- `accounts`: domínio (`AccountStatus`, regras no débito, crédito e encerramento), migração (coluna, índice parcial, grants), rotas, gateway para o `transfers`, eventos, OpenAPI, testes.
- `transfers`: rota interna `GET /internal/accounts/{id}/pending-transfers` (só `SERVICE`), e testes de saga com débito recusado por bloqueio e crédito recusado por encerramento.
- `notification`: três tipos novos (`ACCOUNT_BLOCKED`, `ACCOUNT_UNBLOCKED`, `ACCOUNT_CLOSED`) e o consumo dos eventos.
- `statement`: nada.
- Bruno e smoke: requisições de bloqueio e encerramento (depois da frente de infra mergeada).

## Fora de escopo
- Reabrir conta encerrada.
- Motivo do bloqueio e trilha de auditoria (cartão #49, pós-MVP).
- Frontend.
