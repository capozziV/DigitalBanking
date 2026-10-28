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

Relacionado: [[gate-revisao-task]]
