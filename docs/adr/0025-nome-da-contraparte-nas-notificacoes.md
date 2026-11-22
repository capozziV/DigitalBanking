# 0025 — Nome da contraparte nas notificações

- **Status:** aceito
- **Data:** 2026-11-20
- **Cartão:** #228

## Contexto
As notificações de transferência devem mostrar o nome completo da outra parte. O `notification` ([ADR 0017](0017-notification-com-banco-e-consumo-sqs.md)) tem banco próprio e só recebe a conta da outra parte; o nome vive no `customer` e a ligação conta → cliente, no `accounts`.

## Decisão
- O `notification` mantém no seu PostgreSQL uma **projeção própria**, alimentada por eventos: conta → cliente (`AccountOpened`, de `accounts-events.fifo`) e cliente → nome (`CustomerRegistered`, de `customer-events.fifo`, pela fila `notification-customer.fifo`). Os consumidores são idempotentes e gravam por upsert.
- O texto é **montado na leitura** (`GET /notifications`): junta a conta da outra parte, já guardada nos dados da notificação, com a projeção. Enquanto o nome não chegou, mostra "outro cliente".
- A ordem entre as filas não importa e nenhuma mensagem vai para a DLQ por falta de nome. As notificações antigas passam a mostrar o nome sem migração de dados (corrigido pela emenda de 2026-11-22).

## Consequências
- Positivas: a disponibilidade das notificações não depende de outros serviços; sem latência extra por item; sem token de máquina nem regra de acesso novos; sem migração das notificações (a projeção de nomes precisa de backfill, veja a emenda de 2026-11-22).
- Negativas: o nome é cópia eventual. O cliente não troca o nome hoje; se um dia trocar, será preciso um evento de troca. O nome do cliente passa a existir também no banco do `notification` (dado fictício, banco simulado). Uma fila, um consumidor e uma tabela nova (`customer_names`) para manter; a `known_accounts` já existia e foi reaproveitada.

## Alternativas consideradas
- **Consultar o `customer` ou o `accounts` de forma síncrona, ao consumir ou ao listar:** acoplaria a disponibilidade das notificações à de outros dois serviços, somaria latência por item e exigiria um token de máquina e regras de acesso novas.
- **Endpoint de admin para republicar os clientes existentes (descartada em 2026-11-22):** manual, fácil de esquecer num deploy.

## Emenda de 2026-11-22
- **Erro na decisão:** "as notificações antigas passam a mostrar o nome sem migração de dados" era falso para o cliente cadastrado antes de a fila `notification-customer.fifo` existir. O `CustomerRegistered` dele não é reentregue, a projeção `customer_names` fica vazia e a notificação mostra "outro cliente" (#232).
- **Correção:** a migration V5 do `customer` grava no outbox um `CustomerRegistered` v2 por cliente existente; o relay o publica e o consumidor faz upsert. As notificações em si seguem sem migração.
- **Pré-condição de deploy:** a fila e a inscrição existem antes de o `customer` novo subir; senão o evento do backfill se perde.
- **Consumidor novo:** quem passar a consumir `customer-events.fifo` no futuro precisa do próprio backfill.
- **Texto na leitura (#233):** boas-vindas, bloqueio, desbloqueio e encerramento também têm o texto montado na leitura, sem id de conta, o que corrige as já gravadas.
