# 0019 — Runtime na AWS com EKS

- **Status:** aceito (substitui parcialmente o [ADR 0001](0001-stack-kotlin-spring-boot-aws.md), na parte do ECS Fargate, e o [ADR 0004](0004-api-gateway-http-api-na-borda.md), na parte do ECS Service Connect)
- **Data:** 2026-11-12

## Contexto
O [ADR 0001](0001-stack-kotlin-spring-boot-aws.md) previa ECS Fargate para rodar os serviços, e o [ADR 0004](0004-api-gateway-http-api-na-borda.md) previa ECS Service Connect nas chamadas entre serviços. O projeto é de portfólio, e Kubernetes pesa mais nele que ECS. O ambiente na AWS é efêmero: sobe por cerca de 1 dia para acesso e é destruído, sendo recriado sob demanda com Terraform.

## Decisão
- Os serviços rodam no **Amazon EKS**, no lugar do ECS.
- A descoberta de serviços usa o **DNS de Service do Kubernetes**; a decisão do ECS Service Connect (cartão #55) deixa de valer.
- Nenhum deploy na AWS antes de existir a **derrubada automática com tempo máximo ligado** e um **alarme de orçamento** (cartão #146).
- Nós EC2 ou Fargate, tempo máximo ligado e mecanismo de derrubada ficam em "Em aberto" em `docs/arquitetura.md`, para o brainstorming da etapa 8.

### Papel do GitHub Actions
O GitHub Actions assume, por OIDC, o papel IAM `digital-banking-github` (criado pelo Terraform em `infra/terraform/bootstrap/`). O papel cria e destrói o ambiente `env` inteiro (VPC, EKS, RDS, ElastiCache, ALB, IAM dos pods), por isso tem **`AdministratorAccess`**. Os controles que compensam:
1. **Trust policy restrita:** só `repo:capozziV/DigitalBanking:ref:refs/heads/main`, com `StringEquals` em `aud` e `sub`. O controle real passa a ser a proteção da `main` (PR + job `ci-ok`).
2. **Policy de deny anexada ao papel:** `budgets:*`; `iam:*` sobre o próprio papel, o provedor OIDC e a própria policy de deny (`digital-banking-github-guardrails`, que assim não pode ganhar versão nova nem ser apagada); e, no bucket do estado do Terraform, `s3:DeleteBucket*`, `s3:PutBucketPolicy`, `s3:PutBucketVersioning` e `s3:PutLifecycleConfiguration`.
3. **Custo contido:** Budget de US$ 20/mês com alertas em 50%, 80% e 100%, e o `aws-reaper`, que derruba o ambiente após 8 h.

## Consequências
- Positivas: Kubernetes no portfólio; descoberta de serviços sem recurso específico da AWS; o papel não precisa de manutenção a cada recurso novo do `env`; o papel não consegue alterar o próprio acesso, o orçamento nem o bucket do estado.
- Negativas: o plano de controle custa cerca de US$ 0,10/h (US$ 2,40/dia) e cobra mesmo sem tráfego; mais peças para configurar que no ECS; sem a derrubada automática, o custo corre solto, por isso ela bloqueia o deploy; quem consegue mergear na `main` consegue executar qualquer ação na conta, fora o que o deny barra. Revisitar se o projeto passar a ter dados reais ou mais de um mantenedor.

## Alternativas consideradas
- **ECS Fargate com Service Connect:** mais simples e sem custo de plano de controle; descartado porque pesa menos no portfólio e o custo extra do EKS é pequeno num ambiente efêmero.
- **Escopo mínimo por serviço AWS no papel do GitHub Actions:** mais seguro; descartado porque exigiria manter a lista de ações a cada recurso novo do `env`, num projeto de portfólio sem dados reais.
- **Permission boundary:** descartada pelo mesmo motivo; a policy de deny cobre o que importa proteger.
