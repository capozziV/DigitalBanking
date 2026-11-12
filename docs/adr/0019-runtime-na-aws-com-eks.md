# 0019 — Runtime na AWS com EKS

- **Status:** aceito (substitui parcialmente o [ADR 0001](0001-stack-kotlin-spring-boot-aws.md), na parte do ECS Fargate)
- **Data:** 2026-11-12

## Contexto
O [ADR 0001](0001-stack-kotlin-spring-boot-aws.md) previa ECS Fargate para rodar os serviços, e o [ADR 0004](0004-api-gateway-http-api-na-borda.md) previa ECS Service Connect nas chamadas entre serviços. O projeto é de portfólio, e Kubernetes pesa mais nele que ECS. O ambiente na AWS é efêmero: sobe por cerca de 1 dia para acesso e é destruído, sendo recriado sob demanda com Terraform.

## Decisão
- Os serviços rodam no **Amazon EKS**, no lugar do ECS.
- A descoberta de serviços usa o **DNS de Service do Kubernetes**; a decisão do ECS Service Connect (cartão #55) deixa de valer.
- Nenhum deploy na AWS antes de existir a **derrubada automática com tempo máximo ligado** e um **alarme de orçamento** (cartão #146).
- Nós EC2 ou Fargate, tempo máximo ligado e mecanismo de derrubada ficam em "Em aberto" em `docs/arquitetura.md`, para o brainstorming da etapa 8.

## Consequências
- Positivas: Kubernetes no portfólio; descoberta de serviços sem recurso específico da AWS.
- Negativas: o plano de controle custa cerca de US$ 0,10/h (US$ 2,40/dia) e cobra mesmo sem tráfego; mais peças para configurar que no ECS; sem a derrubada automática, o custo corre solto, por isso ela bloqueia o deploy.

## Alternativas consideradas
- **ECS Fargate com Service Connect:** mais simples e sem custo de plano de controle; descartado porque pesa menos no portfólio e o custo extra do EKS é pequeno num ambiente efêmero.
