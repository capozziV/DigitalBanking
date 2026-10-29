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

Relacionado: [[gate-revisao-task]]
