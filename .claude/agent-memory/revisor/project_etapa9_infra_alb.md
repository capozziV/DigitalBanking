---
name: etapa9-infra-alb-health
description: Servico novo no ALB (web/bff) herda o health_check_path unico do Spring; conferir ao revisar routes.json
metadata:
  type: project
---

`modules/edge-alb/main.tf` usa `var.health_check_path` (`/actuator/health/readiness`) para TODO target group de `routes.json`. Servico nao-Spring (web nginx, bff Node `/bff/health`) fica unhealthy.

**Why:** achado na Task 4 da etapa 9; o check-alb-routes.py nao olha health check.
**How to apply:** ao revisar rota/servico novo no ALB, ver se o health check e por servico. Audiencia: os servicos Spring validam so issuer (sem aud); a lista `audiences` fica em `envs/aws/main.tf` (so autorizador do API Gateway).
