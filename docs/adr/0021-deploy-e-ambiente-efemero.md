# 0021 — Deploy e ambiente efêmero na AWS

- **Status:** aceito
- **Data:** 2026-11-18

## Contexto
O [ADR 0019](0019-runtime-na-aws-com-eks.md) pôs os serviços no EKS e proibiu qualquer deploy antes de existirem a derrubada automática e o alarme de orçamento (cartão #146). Ficaram em aberto o tipo de nó do EKS, o tempo máximo ligado e o mecanismo de derrubada. O projeto é de portfólio, sem dados reais: o ambiente precisa subir com um comando, ficar no ar só enquanto alguém testa e sumir sozinho, sem custo esquecido. Detalhes na [spec da etapa 8](../superpowers/specs/2026-11-16-etapa-8-acabamento-design.md).

## Decisão
- **Região `sa-east-1`** (variável `region`): menos latência para quem testa. Trocar por custo é mudar a variável.
- **Dois stacks Terraform.** `infra/terraform/bootstrap`, permanente e aplicado pelo usuário: bucket do estado (lock nativo do S3), provedor OIDC e papel do GitHub, um repositório ECR por serviço (etiqueta imutável) e o AWS Budgets. `infra/terraform/envs/aws`, efêmero: rede (um NAT Gateway), EKS, ALB interno, Cognito, API Gateway, RDS, ElastiCache, DynamoDB, SNS/SQS e IAM dos pods. O estado do `env` fica no bucket do `bootstrap`, para o workflow agendado conseguir destruí-lo.
- **EKS com node group EC2 gerenciado**, 2× `t3.medium` (AL2023) nas sub-redes privadas, no suporte padrão do Kubernetes (sem o estendido, que cobra a mais). Decisão do usuário (cartão #145).
- **Dados nos menores tamanhos:** RDS PostgreSQL `db.t4g.micro` (um banco por serviço, sem Multi-AZ), ElastiCache Redis `cache.t4g.micro`. Cada serviço roda com um pod (`replicas: 1`).
- **ALB interno criado pelo Terraform**, nunca pelo AWS Load Balancer Controller: o controller só registra os IPs dos pods nos target groups (`TargetGroupBinding`). Assim o `env` sobe num só `apply`, o VPC Link da borda aponta para o listener na mesma rodada e o `destroy` leva o ALB e as ENIs.
- **Pod Identity** liga a ServiceAccount de cada serviço (namespace `digitalbanking`) a um papel IAM com o mínimo de SNS, SQS e DynamoDB, sempre por ARN. Sem IRSA: a ServiceAccount não leva a anotação `eks.amazonaws.com/role-arn`.
- **Job `db-init`** (chart `deploy/helm/db-init`) cria no RDS o banco, o dono e o usuário de aplicação de cada serviço, como o `infra/local/postgres/init` no compose. O Terraform não alcança o RDS privado; o Job roda dentro do cluster, antes dos serviços.
- **Empacotamento:** um chart Helm comum (`deploy/helm/service`) com um arquivo de valores por serviço. Imagem por `Dockerfile` em camadas do Spring Boot, etiqueta = SHA do commit, publicada no ECR pelo `aws-up`.
- **Workflows só da `main`**, no grupo de concorrência `aws-environment`:
  - `aws-up` (manual): `apply` com a etiqueta `created-at`, imagens no ECR, `db-init` e os cinco serviços, seed do dono e smoke pela borda. Subir de novo mantém a etiqueta original e não estende o prazo.
  - `aws-down` (manual ou chamado pelo reaper): `helm uninstall`, `terraform destroy` e conferência do que sobrou pela etiqueta `project=digitalbanking`. Sobrou algo, o job falha com a lista; ele não apaga nada fora do Terraform.
  - `aws-reaper` (a cada 30 min): **tempo máximo ligado de 8 h**, contado pela etiqueta `created-at`.
- **Falha segura no reaper:** 8 h ou mais, etiqueta ausente, malformada ou mais de 10 min no futuro → derruba. Erro da AWS ao ler as etiquetas não derruba às cegas: o job fica vermelho e o GitHub avisa.
- **Budget de US$ 20/mês** com alertas em 50%, 80% e 100% para o e-mail do usuário, como segunda rede (o GitHub suspende agendamentos de repositório sem atividade por 60 dias).
- **Papel do GitHub com `AdministratorAccess` e policy de deny**, conforme o [ADR 0019](0019-runtime-na-aws-com-eks.md).
- **Actuator fora da borda:** o ALB não tem regra para o actuator; o health check vai direto ao pod. Diferente do nginx local, que o expõe.
- **Endpoint público do EKS aberto para `0.0.0.0/0`:** o runner do GitHub não tem IP fixo. A autenticação continua por IAM.
- **Redis sem TLS nem AUTH,** aceitando só o security group do cluster, como no compose local.

## Consequências
- Positivas: o ambiente sobe e cai com um comando; nenhum recurso cobrado fica de pé sem prazo; o `destroy` não deixa ALB nem ENI órfãos; pods sem credencial fixa; a decisão do reaper é testada sem AWS (`infra/aws/reaper-decision.test.sh`).
- Negativas: subir leva tempo (a spec estima ~15 min só o EKS e ~25 min o `aws-up`); o prazo real passa de 8 h (até 30 min do cron, mais o atraso do GitHub e a duração do `destroy`); um `aws-up` logo depois de uma derrubada pode ser derrubado por etiquetas velhas da Tagging API; senhas dos serviços e o segredo do app client de máquina ficam no estado do Terraform (bucket privado, criptografado e versionado); na AWS, `/actuator/metrics` não chega pela borda; endpoint do EKS e Redis sem TLS são exposições conscientes de um ambiente efêmero e simulado; sem alta disponibilidade.
- O `bootstrap` fica de pé entre as subidas, com custo pequeno (S3 e ECR).

## Alternativas consideradas
- **Derrubada por Lambda + EventBridge:** duplica o Terraform e a lógica, e um `destroy` de dentro da AWS não tem o estado na mão.
- **Só o alarme de orçamento:** avisa, não derruba.
- **Estado local do Terraform:** o workflow agendado não teria o estado para destruir.
- **ALB criado pelo controller (Ingress):** obrigaria o `env` em duas fases e deixaria recursos fora do `destroy`.
- **Jib ou `bootBuildImage`** no lugar do `Dockerfile`: o usuário preferiu o `Dockerfile` em camadas, visível no portfólio.
