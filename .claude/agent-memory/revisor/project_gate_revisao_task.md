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
