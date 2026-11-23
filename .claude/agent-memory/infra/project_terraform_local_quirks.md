---
name: terraform-local-quirks
description: No Mac do usuario, o 1o terraform validate/providers schema apos init falha por timeout do plugin aws; repetir resolve. tflint e terraform em ~/.local/bin.
metadata:
  type: project
---

No Mac Intel do usuario, o primeiro `terraform validate` (ou `providers schema`) logo depois de um `init` que baixou o provider aws costuma falhar com "timeout while waiting for plugin to start" (o macOS inspeciona o binario novo). Repetir o comando passa (~1,5 min).

**Why:** visto na etapa 8 (Task 4, 2026-11-16); nao e erro de configuracao.
**How to apply:** ao validar Terraform localmente, rode em segundo plano e, se der esse timeout, repita antes de investigar. `terraform` 1.17.2 (igual ao CI), `tflint` 0.66 e `helm` ficam em `~/.local/bin`; o plugin aws do tflint (0.50.0) baixa do GitHub com `tflint --init`.
