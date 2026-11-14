---
name: revisao-final-bloco
description: O que conferir na revisão final de branch/bloco da etapa 2 (escopo que muda de PR, CI sem job para o código novo, docs atrasados em relação aos rulings)
metadata:
  type: project
---

Na revisão final do bloco 1 da etapa 2 (2026-10-27), as tasks estavam limpas uma a uma, mas o conjunto tinha lacunas que só aparecem olhando a integração.

**Why:** a coordenadora puxou a Task 8 (domínio do `transfers`) para o PR 1. O ruling da Task 6 ("job `transfers` só no bloco 2") partia do PR 1 sem código do `transfers`, e ninguém o revisou depois da mudança: o `ci.yml` não compilava nem testava `:services:transfers:*`, e o `ci-ok` passava mesmo assim. Os rulings do ledger também foram para o código e para o cabeçalho do plano, mas não para a spec (`RefusalReason` × `String`, `debit-not-confirmed`), e a edição do plano ficou sem commit.

**How to apply:**
- Quando o escopo de um PR muda (task adiantada ou adiada), confira de novo os rulings das tasks que pressupunham o escopo antigo, principalmente CI e filtro de caminhos: cada módulo do `settings.gradle.kts` precisa de um job que o compile.
- Compare o ledger (`progress.md`) com a spec e com o plano: ruling aceito tem que chegar aos dois, e o arquivo do plano tem que estar commitado (`git status`).
- Confira o `.gitignore` para `.claude/worktrees/` e decida sobre `.claude/agent-memory/` (memória versionada).
- Commits de merge dos worktrees saem com a mensagem padrão do git, em inglês: aponte isso como Menor, sem pedir para reescrever o histórico.
- Bloco 2 (2026-10-27): o job novo do CI rodava só `:tests:e2e:integrationTest`, então o ktlint do módulo novo não rodava em lugar nenhum do CI. Para cada módulo, confira se algum job roda `build` (ou `check`), não só uma task de teste.
- Bloco 2: invariante de segurança que cruza duas famílias de propriedades (`transfers.saga.call-budget` ≥ conexão + leitura de `transfers.accounts.*`) só existia em KDoc; o e2e ajustou as duas à mão. Procure invariantes entre propriedades de módulos diferentes e peça validação na partida no `bootstrap`.
- Drift de docs se repetiu no bloco 2: rulings de execução (reentrância do arrendamento, owner por execução, 408/429 passageiros, `perform`, pacote do `RetryBackoff`, prazo do e2e) chegaram ao código, mas não à spec §4 nem às Tasks do plano. Faça sempre a tabela ruling → spec/plano.

- Etapa 3 (revisão final, 2026-10-29): de novo o drift veio de correções feitas *depois* da task de docs. A A10 (Clara) escreveu o guia com "espera 10 s"; a correção da A8 mudou smoke/Bruno para 45 s e ninguém voltou ao `rodando-localmente.md` nem ao plano. Renome por ruling (`consume/list/markRead` → `execute`) também ficou só no código. Para cada ruling ou rodada de correção do ledger, faça grep do valor antigo em `docs/` e no plano.
- Menor adiado de docs ("uma fila + DLQ por consumidor") estava errado em dois lugares (arquitetura.md e ADR 0003); ADR novo que refina um antigo precisa citá-lo. Confira os ADRs base, não só o novo.
- Metade dos menores adiados já estava resolvida no HEAD: confira cada um no código antes de mandar para o board.
- Etapa 4 (revisão final, 2026-11-12): sem ferramenta de subagente na sessão, os dois eixos do `code-review` foram feitos em linha. De novo: texto do `ci.yml` ("três jars", comentário do job e2e) e `docs/status.md` atrás do código; spec com campo que o produtor não publica (`openedAt` no item da conta, ausente do payload `AccountOpened`). Confira cada campo da tabela/modelo da spec contra o payload real do produtor (`OutboxEventAppender`). Decisão de spec que depende de dado de outro serviço ("a transferência aparece pela `description`") costuma ficar sem asserção no e2e.
- Etapa 6 (revisão final, 2026-11-12): pronta para PR. Build completo deu 917 testes em 12 min. Script de 10 mutantes num `git archive`, rodando só as classes-alvo: cerca de 8 min, todos vermelhos. Três problemas recorrentes:
  - Drift de docs de novo. O ruling de nome (`daily-limit-unavailable`) não chegou ao ADR nem à spec, que ainda dizem `LIMIT_UNAVAILABLE`.
  - A frase "o TTL corrige" sobreviveu ao ruling do epoch, mas é falsa com TTL renovado a cada uso.
  - ADR base com regra afetada (0016, prazo só em débito e crédito) não foi tocado.
- Em contador de cache com versão (epoch), procure **perda parcial** de escritas (failover com réplica assíncrona, snapshot antigo). A chave fica com o mesmo epoch e abaixo do real, e o ADR só cobre perda total.
- Coluna gravada pelo adapter sob trava e de novo pelo save do agregado, a partir da memória, é um segundo escritor fora da trava. O mutante "domínio não marca" mostra a sobrescrita no IT.
- Mutante que apaga uma chamada a função privada quebra o build (`allWarningsAsErrors`, função não usada). Use uma guarda falsa, como `if (id == UUID(0, 0))`, ou esvazie o lambda.
- Menores adiados que são "plano reservava outro arquivo"/"fora da lista do brief" e já seguem a spec: triagem "sem ação", não issue.

Relacionado: [[gate-revisao-task]]
- Etapa 7 (revisão final, 2026-11-14): aprovada, sem bloqueantes. Build `--rerun-tasks` deu 1200 testes em 15 min. Os 5 mutantes rodaram num script com `git archive`, um por cópia e só nas classes-alvo: cerca de 3 min, todos vermelhos.
  - Drift de docs de novo. Uma task manteve um parâmetro que a spec mandava tirar (`GET /notifications?customerId=` ficou só para o admin). O código tem teste, mas a spec §1 e o plano não foram corrigidos. Para cada "X some"/"sem Y" da spec, faça `git grep` no código.
  - O `docs/status.md` estava parado no meio da etapa.
  - Integração de libs comuns: compare as regras de rota de cada serviço (`SecurityRules`) com os `@*Mapping`. Exceção aberta num só serviço (`/v3/api-docs.yaml`) é sinal de regra que devia estar na lib.
  - Os `TEST-*.xml` não servem com `-newer build.log`, porque o log é gravado até o fim. Com `--rerun-tasks`, conte todos e confira o intervalo dos mtimes.
