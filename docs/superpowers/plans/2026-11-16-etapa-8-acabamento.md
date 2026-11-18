# Etapa 8 — acabamento: plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** imagens por serviço, chart Helm, Terraform completo (bootstrap e ambiente efêmero em `sa-east-1`), workflows de subir, derrubar e derrubada automática, testes de contrato, README com diagrama e ADRs revistos; tudo validado sem credenciais da AWS.

**Architecture:** dois stacks Terraform: `bootstrap` (permanente: estado, OIDC, ECR, Budget) e `envs/aws` (efêmero: rede, EKS, ALB, dados, mensageria, identidade, borda). Um chart Helm comum liga os pods ao ALB por `TargetGroupBinding`. Três workflows (`aws-up`, `aws-down`, `aws-reaper`) só da `main`. Contratos HTTP e de mensagem com Stubborn Contract (ADR 0022).

**Tech Stack:** Docker (Dockerfile em camadas, `eclipse-temurin:21-jre`), Helm 3, kubeconform, Terraform (AWS provider `~> 6.70`), tflint, GitHub Actions, Stubborn Contract (ADR 0022), Kotlin, Spring Boot 4.1.1.

**Spec:** [`docs/superpowers/specs/2026-11-16-etapa-8-acabamento-design.md`](../specs/2026-11-16-etapa-8-acabamento-design.md)

## Global Constraints
- Região `sa-east-1` (variável `region`, padrão `sa-east-1`). Etiquetas `project=digitalbanking` e `created-at` (RFC 3339) em todo recurso do `env`.
- Nenhum `terraform apply`, `helm install` ou push para a AWS sem a conta entregue e a confirmação explícita do usuário. Pronto = validado sem credenciais.
- Tempo máximo ligado: 8 h. `aws-reaper` agendado a cada 30 min.
- Budget: US$ 20/mês, alertas em 50%, 80% e 100%; e-mail em variável sensível `budget_alert_email` (sem padrão).
- EKS: node group gerenciado, 2× `t3.medium`. RDS `db.t4g.micro`, ElastiCache `cache.t4g.micro`, um NAT Gateway.
- OIDC: trust policy só para `repo:capozziV/DigitalBanking:ref:refs/heads/main`.
- Imagem: etiqueta = SHA do commit; usuário não root; jar `<serviço>.jar` (nome já padronizado pela frente de CI).
- Perfil Spring `aws` ativo nos pods (`SPRING_PROFILES_ACTIVE=aws`).
- Segredos nunca versionados; `*.tfstate`/`*.tfvars` no `.gitignore`.
- Código e testes em inglês; docs e ADRs em português. Agentes em worktree não regeneram nem commitam `graphify-out/`.

## Review Focus
1. `terraform destroy` do `env` não deixa recurso cobrado (ALB, ENIs, NAT, EIP): o ALB é do Terraform, nunca do controller (Task 4, conferido no plano do `terraform`).
2. Reaper com relógio e etiqueta em fusos diferentes, etiqueta ausente ou malformada: ausente ou malformada → derruba (falha segura) (Task 6, testes do script).
3. Pod sem acesso a DynamoDB/SNS/SQS por Pod Identity mal ligada: `helm template` + teste do mapeamento ServiceAccount → papel (Tasks 2 e 5).
4. Serviço que não sobe com o perfil `aws` por variável faltando no values: teste que compara as variáveis exigidas pelo `application.yml` com as do values (Task 2).
5. Contrato quebrado no produtor (campo renomeado no `accounts`) derruba o build do produtor, não só o do consumidor (Task 7, mutante).

## Ordem e paralelismo
- **Onda 1 (agora):** Task 2 (Rafael), Task 3 (Rafael), Task 4 (Rafael) em worktrees separados a partir de `etapa-8-acabamento`. Só a Task 4 edita `infra/terraform/envs/aws/*`.
- **Onda 2:** Task 1 (Rafael) depois da frente de CI (#45) mergeada na `main` e trazida para a branch; Task 5 (Rafael) depois da Task 4.
- **Onda 3:** Tasks 7 e 8 (Marina) depois das frentes de débito técnico do `transfers`, `accounts` e perfis mergeadas.
- **Onda 4:** Task 6 (Rafael) depois das Tasks 1–5; Task 9 (Clara) por último.
- **Pendente do usuário:** Task 10 (subida real).
- `ci.yml`: só as Tasks 1 e 6 editam.

---

### Task 1: Dockerfile em camadas e smoke contra as imagens
**Files:** `services/<serviço>/Dockerfile` (5; multi-stage com `java -Djarmode=tools -jar <serviço>.jar extract --layers --launcher`); `infra/local/docker-compose.yml` perfil `apps` com os cinco serviços pelas imagens; `ci.yml`: job `images` (constrói as cinco a partir dos jars do build) e smoke contra o perfil `apps`.
- [ ] **Testes:** `docker build` das cinco; contêiner roda como não root (`docker run --entrypoint id` ≠ 0); compose `apps` de pé com os cinco healthy; `seed-check.sh` e `smoke.sh` verdes contra ele.
- [ ] Commit `build: imagem Docker em camadas por serviço e smoke contra as imagens`.

### Task 2: chart Helm
**Files:** `deploy/helm/service/` (Chart, templates: Deployment, Service, ServiceAccount, ConfigMap, PodDisruptionBudget, `TargetGroupBinding`); `deploy/helm/values/{accounts,transfers,statement,notification,customer}.yaml`; `deploy/helm/validate.sh` (`helm lint`, `helm template` de cada values, `kubeconform -strict` com o schema do CRD do `TargetGroupBinding`).
**Interfaces (produz):** values `image.repository`, `image.tag`, `targetGroupArn`, `serviceAccount.roleArn`, `env` (mapa), `secretEnv` (nome do Secret → chaves).
- [ ] **Testes:** `validate.sh` verde; teste (script) que lista as variáveis `${…}` sem padrão no `application.yml` + `application-aws.yml` de cada serviço e confere que o values de cada um as define (Review Focus 4); probes em `/actuator/health/liveness` e `/readiness`.
- [ ] Commit `deploy: chart Helm comum aos serviços`.

### Task 3: Terraform `bootstrap`
**Files:** `infra/terraform/bootstrap/` (`versions.tf`, `main.tf`, `variables.tf`, `outputs.tf`): bucket S3 do estado (versionado, criptografado, bloqueio público, lock nativo `use_lockfile`), provedor OIDC do GitHub e papel com a trust policy da `main`, ECR por serviço (expira imagens sem etiqueta em 1 dia, mantém as 10 últimas), `aws_budgets_budget` US$ 20 com três notificações. Saídas: `state_bucket`, `github_role_arn`, `ecr_repository_urls`.
- [ ] **Testes:** `terraform fmt -check`, `init -backend=false`, `validate`, `tflint` verdes; a trust policy contém só `ref:refs/heads/main` (teste com `terraform show`/`jq` sobre um `plan` simulado não é possível sem credenciais: conferir por `grep` no HCL renderizado em `validate.sh`).
- [ ] Commit `infra: Terraform do bootstrap (estado, OIDC, ECR e orçamento)`.

### Task 4: Terraform `env`: rede, EKS, ALB e borda
**Files:** `infra/terraform/modules/network`, `modules/eks` (cluster, node group, add-ons, Pod Identity Agent, AWS Load Balancer Controller via `helm_release`), `modules/edge-alb` (ALB interno, listener, target group por serviço, regras por caminho iguais às do nginx); `envs/aws/main.tf` compõe tudo e liga `edge` ao listener do `edge-alb`; backend `s3` com configuração parcial; `default_tags` com as etiquetas.
**Interfaces (produz):** saídas `cluster_name`, `target_group_arns` (mapa serviço → ARN), `edge_url`, `private_subnet_ids`, `vpc_id`, `cluster_security_group_id`.
- [ ] **Testes:** `fmt`, `init -backend=false`, `validate`, `tflint` verdes; regras do ALB batem com as `location` do nginx (script compara os prefixos).
- [ ] Commit `infra: rede, EKS, ALB interno e borda no ambiente efêmero`.

### Task 5: Terraform `env`: dados, mensageria e acessos
**Files:** `modules/data` (RDS PostgreSQL com bancos e usuários por serviço via provider `postgresql` ou Job, ElastiCache, DynamoDB do extrato), `modules/messaging` (tópicos SNS FIFO e filas SQS FIFO com DLQ iguais aos scripts de `infra/local/localstack/ready.d`), Pod Identity: um papel por serviço com o mínimo de DynamoDB/SNS/SQS; Secrets Manager com as senhas geradas e o segredo do app client de máquina; `envs/aws/main.tf` liga os módulos.
**Interfaces (produz):** saídas `service_role_arns`, `secret_names`, `queue_urls`, `topic_arns`, `db_endpoint`, `redis_endpoint`.
- [ ] **Testes:** `fmt`, `validate`, `tflint` verdes; script que compara tópicos, filas e DLQs com os do LocalStack (nomes e atributos FIFO).
- [ ] Commit `infra: dados, mensageria e acessos por serviço no ambiente efêmero`.

### Task 6: workflows `aws-up`, `aws-down` e `aws-reaper`
**Files:** `.github/workflows/aws-up.yml`, `aws-down.yml`, `aws-reaper.yml`; `infra/aws/reaper-decision.sh` (entrada: `created-at`, agora; saída: `destroy`/`keep`); `infra/aws/leftovers.sh`; `ci.yml`: jobs `terraform` (os dois stacks + `tflint`), `helm` (`validate.sh`), testes do `reaper-decision.sh` (bats ou shell puro); seed e smoke com `--remote` e tokens por `AdminInitiateAuth`.
- [ ] **Testes:** `reaper-decision.sh`: 7 h 59 → `keep`; 8 h 01 → `destroy`; etiqueta ausente → `destroy`; malformada → `destroy`; fuso diferente → certo (Review Focus 2). `actionlint` limpo.
- [ ] Commit `ci: subir, derrubar e derrubada automática do ambiente na AWS`.

### Task 7: contratos HTTP
**Files:** Stubborn Contract 0.2.2 (ADR 0022) no `build-logic` (configuração no produtor e nos stubs do consumidor, conforme a documentação da ferramenta); contratos em `services/accounts/bootstrap/src/contractTest/resources/contracts` (consulta da conta, débito, crédito, busca de lançamento) e `services/customer/...` (`/internal/customers/{id}`); `transfers` e `accounts` trocam os stubs escritos à mão dos testes dos gateways HTTP pelos do contrato.
- [ ] **Testes:** produtor gera e passa os testes do contrato; consumidor passa com os stubs; mutante: campo renomeado no `AccountDTO` → build do `accounts` vermelho (Review Focus 5).
- [ ] Commit `test(contract): contratos HTTP entre transfers, accounts e customer`.

### Task 8: contratos de mensagem
**Files:** contratos dos eventos `accounts` → `statement`/`notification` e `transfers` → `notification` (envelope do outbox); `MessageVerifier` próprio no produtor (publica pelo mapeador do outbox e lê o envelope gravado); no consumidor, a mensagem do contrato vai direto ao tratador do listener.
- [ ] **Testes:** produtor e consumidor verdes; mutante: campo do envelope renomeado → produtor vermelho.
- [ ] Commit `test(contract): contratos dos eventos`.

### Task 9: README, diagrama e ADRs
**Files:** `README.md` (o que é, diagrama Mermaid, pontos fortes, rodar local, subir na AWS, custo por subida); ADR 0001 marcado como substituído na parte do runtime; ADR 0021 (deploy e ambiente efêmero); ADR 0022 (testes de contrato); releitura dos ADRs; `docs/arquitetura.md` sem os itens decididos de "Em aberto".
- [ ] Commit `docs: README com diagrama e revisão dos ADRs`.

### Task 10: subida real (pendente do usuário)
`bootstrap` aplicado pelo usuário; variáveis do repositório; `aws-up`, seed, smoke remoto, `aws-down`; `leftovers.sh` vazio. Não bloqueia o merge da etapa.

## Revisão final da branch (Beatriz)
Mutantes: reaper mantendo ambiente com 9 h → vermelho; contrato do `accounts` com campo renomeado → vermelho; values sem variável exigida → teste da Task 2 vermelho.
