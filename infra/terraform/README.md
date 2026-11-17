# Terraform

Infraestrutura na AWS do banco digital, em `sa-east-1` ([ADR 0019](../../docs/adr/0019-runtime-na-aws-com-eks.md)). O stack `envs/aws` é o ambiente efêmero: rede, EKS, ALB interno, identidade e borda (dados e mensageria entram na Task 5 da etapa 8).

**Nada foi aplicado.** O código só é validado (`fmt`, `init -backend=false`, `validate`), sem credenciais da AWS. Nenhum `apply` antes da derrubada automática e do alarme de orçamento (#146), e sempre com confirmação explícita do usuário.

## Estrutura
- `modules/identity`: User Pool do Cognito (login por e-mail), grupo `admin`, resource server `digitalbanking` com o escopo `internal`, app client web (authorization code + PKCE, sem segredo) e app client de máquina (client credentials, escopo `digitalbanking/internal`).
- `modules/edge`: API Gateway HTTP API, autorizador JWT do Cognito, VPC Link e uma integração `HTTP_PROXY` para o listener do ALB interno. Rotas `ANY /prefixo` e `ANY /prefixo/{proxy+}` para `/accounts`, `/transfers`, `/notifications`, `/customers` e `/admin`; extrato e limite diário ficam sob `/accounts` e `/transfers`. Todas exigem JWT.
- `modules/network`: VPC `10.20.0.0/16` em duas zonas, sub-redes públicas (só o NAT) e privadas, um NAT Gateway com EIP.
- `modules/eks`: cluster (suporte padrão, sem o estendido, que cobra a mais), node group gerenciado 2× `t3.medium` (AL2023) nas sub-redes privadas, add-ons gerenciados (VPC CNI, kube-proxy, CoreDNS, Pod Identity Agent) e o AWS Load Balancer Controller (chart `3.6.2`) por `helm_release`, com papel por Pod Identity.
- `modules/edge-alb`: ALB interno, listener HTTP 80 (404 no que não casa), um target group por serviço (alvo por IP, health check em `/actuator/health/readiness`) e as regras por caminho de `routes.json`, espelho das `location` do nginx local.
- `envs/aws`: compõe tudo; o VPC Link da borda aponta para o listener do ALB. Backend `s3` com configuração parcial e lock nativo (`use_lockfile`); o bucket vem do stack `bootstrap`.

## ALB do Terraform, nunca do controller
O `terraform destroy` precisa levar o ALB, as ENIs dele, o NAT e o EIP. Por isso o ALB e os target groups são do Terraform, e o AWS Load Balancer Controller só registra os IPs dos pods (`TargetGroupBinding` do chart Helm, com `targetGroupArn` da saída `target_group_arns`):
- o papel do controller só lê (`Describe*`) e registra ou tira alvos nos target groups deste ambiente; sem permissão para criar load balancer, target group ou regra de security group;
- sem `IngressClass`, com o controller de Service (NLB) e o Gateway API desligados;
- o `TargetGroupBinding` não usa `networking`: as regras ALB → pods (portas 8081–8085 no security group do cluster) são do Terraform.
O `aws-down` desinstala o chart dos serviços antes do `destroy`, para os `TargetGroupBinding` saírem com o controller ainda de pé.

## Roteamento do ALB
`modules/edge-alb/routes.json` traz os serviços (porta) e as regras. As regex do limite diário (`transfers`) e do extrato (`statement`) têm prioridade menor que o prefixo `/accounts/`, como no nginx. As `location` do actuator ficam só no local: o ALB checa a saúde direto no pod e a borda não expõe o actuator. `envs/aws/check-alb-routes.py` compara os casadores do ALB com as `location` do nginx (exato, prefixo, regex e serviço pela porta) e falha se divergirem.

## Etiquetas e `created_at`
`default_tags` põe `project=digitalbanking` e `created-at` em todo recurso do provider. `created_at` (RFC 3339) não tem padrão: vem do workflow na subida, e o `aws-reaper` lê a etiqueta. Recursos que o EKS cria por conta própria (instâncias e ENIs do node group, security group do cluster) não recebem `default_tags`, mas saem com o node group e o cluster.

## Cognito: domínio e PKCE
- O módulo `identity` cria o `aws_cognito_user_pool_domain`; sem ele não existe `/oauth2/token` e o `client_credentials` não funciona. O prefixo vem da variável `cognito_domain_prefix` (sem padrão, precisa ser único na região). Saídas: `cognito_domain` e `token_endpoint_url`.
- O Cognito não impõe PKCE no servidor. Quem aplica é o frontend (etapa 9), que usa o app client web, público e sem segredo.

## Borda: limites e log de acesso
- O stage `$default` limita taxa e rajada (`throttling_rate_limit`, padrão 10 req/s; `throttling_burst_limit`, padrão 20), baixos por ser ambiente de portfólio.
- O log de acesso vai para um Log Group do CloudWatch (`/aws/apigateway/<nome>-http/access`) com retenção de 7 dias (`access_log_retention_days`).

## Saídas para as outras tasks
`cluster_name`, `target_group_arns` (serviço → ARN), `edge_url`, `private_subnet_ids`, `vpc_id`, `cluster_security_group_id`, além das da identidade.

## Observações
- O autorizador da borda só valida emissor e audiência (os dois app clients). A checagem do grupo `admin` (claim `cognito:groups`) e do escopo `internal` é dos serviços, que também validam o JWT (ADR 0004).
- O segredo do app client de máquina sai como saída sensível (`machine_client_secret`) e fica no estado local; nunca em arquivo versionado. `*.tfstate` e `*.tfvars` estão no `.gitignore`.
- O `.terraform.lock.hcl` fixa os providers `hashicorp/aws` (`~> 6.70`) e `hashicorp/helm` (`~> 3.1`) para linux e macOS.
- O provider `helm` pega o token do cluster pelo `aws eks get-token`: quem aplica precisa do aws CLI e é o administrador do cluster (`bootstrap_cluster_creator_admin_permissions`).

## Validar
```bash
terraform fmt -check -recursive infra/terraform
terraform -chdir=infra/terraform/envs/aws init -backend=false
terraform -chdir=infra/terraform/envs/aws validate
tflint --chdir=infra/terraform/envs/aws --init
tflint --chdir=infra/terraform/envs/aws
for m in network eks edge-alb; do tflint --chdir=infra/terraform/modules/$m --config=$PWD/infra/terraform/envs/aws/.tflint.hcl; done
infra/terraform/envs/aws/check-alb-routes.py
```
