---
name: gate-revisao-task
description: Como funcionam os gates de revisão por task da etapa 2 (arquivos em .superpowers/sdd/, relato sem saída literal, achados recorrentes de cobertura e de contrato de porta)
metadata:
  type: project
---

Na etapa 2 (transfers), cada task chega com brief, global-constraints, diff e relato (`task-N-report.md`) em `.superpowers/sdd/<data>-etapa-2-transfers/`.

**Why:** na Task 8 (2026-10-27) o relato não existia; na Task 9 existia, mas curto e sem saída literal do build. A contagem de testes só pôde ser conferida contando os `@Test` do diff.

**How to apply:**
- Confira se o relato existe e se traz saída real; sem ela, registre como Menor (checklist "build, testes e lint com resultado real") e conte os `@Test` do diff.
- Cobertura no domínio: `status` PENDING e `terminalEvent() == null` testados só em parte dos passos não terminais (Task 8).
- Fakes de porta (InMemory*) costumam definir semântica que o contrato da porta não diz (ex.: Task 9, `findDue` do fake trata `next_attempt_at` nulo como vencido, mas a SQL da spec usa `<= now()`). Compare a KDoc da porta com o fake e com a SQL da spec.
- Regras de exceção da tabela de passos da spec (ex.: Compensate recusado usa a espera máxima) tendem a ser trocadas pelo caminho genérico; confira cada célula da tabela contra o código e os testes.
- Ao mudar a semântica de um campo de agendamento (ex.: `nextAttemptAt` nulo deixa de ser vencido, Task 9 correção 1), confira todos os pontos de escrita da porta, não só o caso de uso: `insert` sem o campo deixou transferência recém-criada fora de `findDue`.
- Porta x spec: a KDoc da porta (Task 9) às vezes acrescenta semântica que a SQL da spec não tem (Task 11: `tryAcquireLease` reentrante para o mesmo dono). A implementadora segue a porta; confira se a diferença é segura com os `owner` reais do plano (agendador `scheduler-<host>-<pid>` é fixo por processo).
- Adapter que tira "agora" de um campo do domínio (Task 11: `saveIfOwner` usa `transfer.updatedAt`) é contrato implícito; peça `now` explícito na porta.
- Teste de "terminal" que passa por outro motivo (findDue vazio por `next_attempt_at` nulo, não pela coluna `status`): confira se a asserção isola o que o nome do teste promete.
- Desligamento gracioso de laço em lote (Task 14, agendador da saga): copiar o padrão do `OutboxRelayScheduler` (trava + espera) não basta quando o trabalho não tem duração máxima como o ciclo do relay. Confira se o lote checa "parando" entre itens e se `runCatching` (pega `Throwable`) engole `InterruptedException` e segue o lote depois do `shutdownNow`. Na correção (fix1) a interrupção foi detectada só na cadeia de causas da exceção, mas o gateway HTTP converte `IOException`/`RestClientException` em `TransientFailure` sem lançar: o caminho real não chega ao `catch`. Peça também `Thread.currentThread().isInterrupted` no laço.
- "Reinício" em IT (Task 14): ITs costumam simular a queda montando o estado no banco em vez de fechar e subir um contexto. Compare com a lista de testes da spec §8 e diga se o e2e (Task 16) cobre o resto.
- Cliente HTTP (Task 12): `HttpRequest.timeout` do JDK/RestClient cobre só até os cabeçalhos; corpo que trava depois do cabeçalho pode não respeitar o read-timeout. Em cliente de fronteira, peça teste com corpo lento (`withChunkedDribbleDelay`) e o risco "404 account-not-found no FindEntry" testado no gateway, não só no classificador.
- E2e com tempo (Task 16): asserção que compara com um instante (ex.: "passos depois da queda só após `locked_until`") fica vazia quando o ambiente é lento (reinício maior que o arrendamento). Peça a pré-condição explícita logo depois do reinício (`now < lockedUntil`, `now < deadline_at`), falhando com mensagem de ambiente lento, nunca `assume` (CI exige 0 pulados). Confira também: proxy de teste que troca "conexão recusada" por `502` deixa o caminho real local (transfers direto no accounts) sem e2e; `stop()` que não confere a morte do processo; ruling de prazo do e2e chegando à spec §8.
