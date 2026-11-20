# 0025 — Nome da contraparte nas notificações

- **Status:** aceito
- **Data:** 2026-11-20
- **Cartão:** #228

## Contexto
As notificações de transferência devem mostrar o nome completo da outra parte. O `notification` ([ADR 0017](0017-notification-com-banco-e-consumo-sqs.md)) tem banco próprio e só recebe a conta da outra parte; o nome vive no `customer` e a ligação conta → cliente, no `accounts`.

## Decisão
- O `notification` mantém no seu PostgreSQL uma **projeção própria**, alimentada por eventos: conta → cliente (`AccountOpened`, de `accounts-events.fifo`) e cliente → nome (`CustomerRegistered`, de `customer-events.fifo`, pela fila `notification-customer.fifo`). Os consumidores são idempotentes e gravam por upsert.
- O texto é **montado na leitura** (`GET /notifications`): junta a conta da outra parte, já guardada nos dados da notificação, com a projeção. Enquanto o nome não chegou, mostra "outro cliente".
- A ordem entre as filas não importa e nenhuma mensagem vai para a DLQ por falta de nome. As notificações antigas passam a mostrar o nome sem migração de dados.

## Consequências
- Positivas: a disponibilidade das notificações não depende de outros serviços; sem latência extra por item; sem token de máquina nem regra de acesso novos; sem migração de dados.
- Negativas: o nome é cópia eventual. O cliente não troca o nome hoje; se um dia trocar, será preciso um evento de troca. O nome do cliente passa a existir também no banco do `notification` (dado fictício, banco simulado). Uma fila, um consumidor e duas tabelas novos para manter.

## Alternativas consideradas
- **Consultar o `customer` ou o `accounts` de forma síncrona, ao consumir ou ao listar:** acoplaria a disponibilidade das notificações à de outros dois serviços, somaria latência por item e exigiria um token de máquina e regras de acesso novas.
