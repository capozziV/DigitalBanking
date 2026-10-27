# 0016 — Saga de transferência orquestrada por HTTP síncrono

- **Status:** aceito
- **Data:** 2026-10-27

## Contexto
A transferência mexe em duas contas do `accounts` sem transação distribuída: débito na origem, crédito no destino e, se o crédito não sai, devolução à origem. É preciso garantir que o dinheiro nunca some nem duplica diante de falha no meio, retentativa ou queda de processo, e que o cliente saiba o resultado já na primeira resposta quando possível. O `accounts` já expõe crédito e débito idempotentes por chave. Detalhes na [spec da etapa 2](../superpowers/specs/2026-10-27-etapa-2-transfers-design.md).

## Decisão
- **Orquestração no `transfers`:** o serviço conduz a saga (`TransferSaga`) e guarda o passo atual; os passos seguem o padrão State (`TransferStep`).
- **HTTP síncrono ao `accounts`**, com os endpoints de crédito e débito existentes. `POST /transfers` roda a saga na requisição; em falha passageira devolve `202` com a transferência `PENDING`, e um agendador termina depois.
- **Idempotência por passo:** a chave de cada chamada é derivada do `transferId` (`transfer-<id>-debit`, `-credit`, `-compensation`), então repetir uma chamada é seguro.
- **Prazo de 2 minutos** (configurável) desde a criação, só para falha passageira em débito e crédito. Vencido o prazo, o `transfers` não repete a chamada: **consulta o lançamento pela chave** (`GET /accounts/{id}/entries?idempotencyKey=…`, endpoint novo, sem gravar nada) para resolver a dúvida. Débito encontrado leva à compensação; crédito encontrado leva à conclusão; lançamento ausente leva à falha (débito) ou à compensação (crédito).
- **Compensação** é um crédito de volta na origem, tentado até conseguir.
- **Arrendamento** (`locked_until`, `lock_owner`) em vez de trava de banco durante a chamada HTTP: nenhuma transação fica aberta esperando o `accounts`, e a saga roda em um lugar só. Perder o arrendamento é seguro por causa das chaves idempotentes.
- Eventos `TransferCompleted`, `TransferFailed` e `TransferCompensated` pelo outbox ([ADR 0015](0015-outbox-relay-lider-e-sns-fifo.md)).

## Consequências
- Positivas: o cliente costuma saber o resultado na primeira resposta; sem componente novo de mensageria nesta etapa; a saga sobrevive a queda do `transfers` (o agendador retoma) e a repetição de chamadas; a consulta pela chave evita pagar em dobro; cada passo é testável isolado.
- Negativas: `transfers` e `accounts` ficam acoplados em tempo de execução (o `accounts` fora do ar deixa transferências `PENDING`); o endpoint de consulta é um contrato novo no `accounts`; há um prazo e um arrendamento a ajustar.
- **Risco residual aceito:** uma chamada ainda em voo no `accounts` quando a consulta roda poderia gravar depois. Com leitura de 3 s, `lock_timeout` de 2 s e prazo de 2 min, não acontece na prática; se o crédito tardio aparecer na consulta, a transferência conclui.
- **Compensação recusada em laço:** o crédito na origem não tem regra de recusa, então uma recusa indica bug. O passo continua tentando com a espera máxima e registra `ERROR`; a transferência não termina sozinha. Desbloqueio manual fica no cartão [#56](https://github.com/capozziV/DigitalBanking/issues/56).

## Alternativas consideradas
- **Comandos por mensageria (SNS/SQS):** antecipa a etapa 3 e obriga o `accounts` a ter um consumidor novo.
- **Híbrido (HTTP e mensageria):** carrega a complexidade dos dois.
- **Sempre assíncrono:** o cliente nunca sabe o resultado na primeira resposta.
- **Compensar sem consultar após o prazo:** se o crédito tiver acontecido com timeout, o destino recebe e a origem é reembolsada (dinheiro em dobro).
- **Repetir o débito após o prazo:** débito tardio e estorno aparecem no extrato do cliente.
