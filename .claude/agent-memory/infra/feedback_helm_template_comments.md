---
name: helm-template-comments
description: Comentario YAML (#) em template Helm vai para o manifesto renderizado; validate.sh que faz grep no render casa com ele
metadata:
  type: feedback
---

Em `templates/*.yaml` de chart Helm, comentario que cita algo que o `validate.sh` procura por grep (ex.: `eks.amazonaws.com/role-arn`) tem de ser `{{- /* ... */}}`, nao `#`.

**Why:** na Task 5 da etapa 8 (2026-11-17), o comentario `# Sem a anotacao IRSA (eks.amazonaws.com/role-arn)` saiu no `helm template` e a checagem "sem IRSA" falhou no proprio chart correto.

**How to apply:** ao escrever checagem por grep no manifesto renderizado, use comentario de template; ao ver falso positivo num grep do render, procure comentario `#` no template primeiro. Ver tambem [[terraform-local-quirks]].
