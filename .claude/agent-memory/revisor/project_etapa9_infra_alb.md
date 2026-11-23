---
name: etapa9-infra-alb-health
description: Servico novo no ALB (web/bff) herda o health_check_path unico do Spring; conferir ao revisar routes.json
metadata:
  type: project
---

`modules/edge-alb/main.tf` usa `var.health_check_path` (`/actuator/health/readiness`) para TODO target group de `routes.json`. Servico nao-Spring (web nginx, bff Node `/bff/health`) fica unhealthy.

**Why:** achado na Task 4 da etapa 9; o check-alb-routes.py nao olha health check.
**How to apply:** ao revisar rota/servico novo no ALB, ver se o health check e por servico. Audiencia: os servicos Spring validam so issuer (sem aud); a lista `audiences` fica em `envs/aws/main.tf` (so autorizador do API Gateway).

**Recorrentes das re-revisões da etapa 9:**
- Re-revisao Task 10 (e2b625e): ambos bloqueantes resolvidos. Recorrente: httpFetch (node:http) sem timeout (fetch tinha); derivacao nova em script de deploy sem asserção no .test.sh (mutante us-east-1 sobreviveu); IT local do Host nao discrimina (default ja e localhost:8090), so o unit.
- Task 7 BFF (72fd64c): recorrente: mudanca em values/*.yaml (secretEnv) sem rodar deploy/helm/validate.sh (guard "secretKeyRef" quebra) e sem passo que crie o Secret; token "opaco" so assinado (base64 legivel) nao esconde o dado.
- Task 9 web (79e9a2f): recorrente: spec do web mocka resposta que o BFF real nunca devolve ({account:null} em /home; BFF da 404) e fixa contrato errado (upTo=createdAt, BFF/servico exigem UUID). Sempre cruzar cada chamada do web com o controller do BFF e o DTO do servico; e2e so skeleton ate a Task 11. Rodar ng test com `--watch=false --include=...` para mutantes; sed -i no Mac precisa `''`.
