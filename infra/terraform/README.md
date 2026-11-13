# Terraform

Infraestrutura na AWS do banco digital. Nesta etapa (7) só a identidade e a borda; rede, EKS, ALB, dados e mensageria vêm nas etapas seguintes ([ADR 0019](../../docs/adr/0019-runtime-na-aws-com-eks.md)).

**Nada foi aplicado.** O código só é validado (`fmt`, `init -backend=false`, `validate`), sem credenciais da AWS. Nenhum `apply` antes da derrubada automática e do alarme de orçamento (#146), e sempre com confirmação explícita do usuário.

## Estrutura
- `modules/identity`: User Pool do Cognito (login por e-mail), grupo `admin`, resource server `digitalbanking` com o escopo `internal`, app client web (authorization code + PKCE, sem segredo) e app client de máquina (client credentials, escopo `digitalbanking/internal`).
- `modules/edge`: API Gateway HTTP API, autorizador JWT do Cognito, VPC Link e uma integração `HTTP_PROXY` para o listener do ALB interno. Rotas `ANY /prefixo` e `ANY /prefixo/{proxy+}` para `/accounts`, `/transfers`, `/notifications`, `/customers` e `/admin`; extrato e limite diário ficam sob `/accounts` e `/transfers`. Todas exigem JWT.
- `envs/aws`: compõe os dois módulos. Estado local, sem backend remoto (ambiente efêmero).

## Entradas da etapa 8
`alb_listener_arn`, `subnet_ids` e `security_group_ids` não têm valor padrão: vêm da rede, do EKS e do ALB da etapa 8.

## Observações
- O autorizador da borda só valida emissor e audiência (os dois app clients). A checagem do grupo `admin` (claim `cognito:groups`) e do escopo `internal` é dos serviços, que também validam o JWT (ADR 0004).
- O segredo do app client de máquina sai como saída sensível (`machine_client_secret`) e fica no estado local; nunca em arquivo versionado. `*.tfstate` e `*.tfvars` estão no `.gitignore`.
- O `.terraform.lock.hcl` fixa o provider `hashicorp/aws` (`~> 6.70`) para linux e macOS.

## Validar
```bash
terraform fmt -check -recursive infra/terraform
terraform -chdir=infra/terraform/envs/aws init -backend=false
terraform -chdir=infra/terraform/envs/aws validate
```
