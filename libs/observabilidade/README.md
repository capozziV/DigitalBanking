# libs/observabilidade

Correlation ID nas requisições HTTP. A propagação por eventos continua no `libs/outbox`.

- `CorrelationIdFilter`, registrado por autoconfiguração antes de todos os filtros (inclusive o do Spring Security):
  - reaproveita o `X-Correlation-Id` recebido quando é um token seguro: letras, dígitos e `. _ - = + /`, até 128 caracteres;
  - fora disso, gera um UUID e registra a troca em DEBUG, sem o valor descartado;
  - põe o id no MDC (chave `correlationId`, a mesma de `OutboxEventEnvelope.CORRELATION_ID_MDC_KEY`) e o devolve no cabeçalho da resposta.
- `CorrelationIdRestClientInterceptor`: copia o id do MDC para o `X-Correlation-Id` da chamada de saída. Adicione-o em cada `RestClient` que chama outro serviço: `RestClient.builder().requestInterceptor(CorrelationIdRestClientInterceptor())`.

Para usar, o serviço depende de `:libs:observabilidade` no `adapters` e apaga o próprio `CorrelationIdFilter`.
