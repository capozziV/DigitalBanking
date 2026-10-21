# 0006 — Observabilidade só com correlation ID

- **Status:** aceito
- **Data:** 2026-10-21

## Contexto
Uma requisição atravessa vários serviços por HTTP e por eventos (SNS/SQS). É preciso conseguir seguir essa requisição nos logs de ponta a ponta.

## Decisão
- Observabilidade baseada **apenas em correlation ID**, sem tracing distribuído.
- O ID vem do header `X-Correlation-Id` ou é gerado na entrada.
- Vai para o MDC e aparece nos logs em JSON.
- É propagado nas chamadas HTTP, numa coluna do outbox, no envelope do evento e nos atributos SNS/SQS (raw message delivery).
- Até existir o BFF (etapa 9), o serviço que recebe a chamada gera o ID; depois, o BFF gera e os serviços reaproveitam.

## Consequências
- Positivas: simples de implementar e de entender; sem infraestrutura extra de tracing; cobre também o caminho assíncrono via outbox e mensageria.
- Negativas: sem visão de tempo por etapa (spans) nem grafo de chamadas; a correlação depende de busca em logs.

## Pontos em aberto
Nenhum.

## Alternativas consideradas
- **Tracing distribuído (OpenTelemetry, Jaeger ou X-Ray):** descartado; o planejamento optou só por correlation ID.
