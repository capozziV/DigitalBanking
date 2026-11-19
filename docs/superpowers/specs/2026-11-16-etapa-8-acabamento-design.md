# Etapa 8 — acabamento: design

> Spec da etapa 8 do [roadmap](../../roadmap.md): testes de contrato, imagem Docker por serviço, Terraform completo, EKS e deploy no CI com derrubada automática e alarme de orçamento, README com diagrama, revisão dos ADRs.

## Objetivo
Deixar o banco digital pronto para ser mostrado: os cinco serviços sobem na AWS com um comando, ficam no ar por no máximo 8 horas para quem quiser testar e somem sozinhos. Antes disso, o repositório ganha testes de contrato entre os serviços, imagem por serviço e um README que explica o sistema com um diagrama.

## Decisões desta etapa (usuário, 2026-11-16)
- **Região:** `sa-east-1` (menos latência para quem testa no lançamento); a região fica fixa: trocá-la (por exemplo para `us-east-1`, por custo) exige mudar os workflows, os values do Helm e o `validate.sh`.
- **Tempo máximo ligado:** 8 horas.
- **Derrubada:** workflow agendado no GitHub Actions que roda `terraform destroy` depois das 8 h, e workflows manuais (`workflow_dispatch`) para subir e derrubar a qualquer momento.
- **Orçamento:** AWS Budgets de US$ 20/mês, com alertas em 50%, 80% e 100% para o e-mail do usuário.
- **EKS:** node group EC2 gerenciado (2× `t3.medium`) ([ADR 0019](../../adr/0019-runtime-na-aws-com-eks.md), cartão #145).
- **Empacotamento no Kubernetes:** um chart Helm comum aos cinco serviços, com um arquivo de valores por serviço.
- **Imagem:** Dockerfile em camadas por serviço (camadas do Spring Boot: dependências, loader, dependências snapshot, aplicação).
- **Testes de contrato:** Stubborn Contract (ADR 0022), contratos no repositório.
- **Dados na AWS:** serviços gerenciados nos menores tamanhos: RDS PostgreSQL, ElastiCache Redis, DynamoDB, SNS e SQS.
- **Acesso à conta AWS:** o usuário entrega a conta e o papel para o GitHub (OIDC) quando for subir. Até lá, tudo é construído e validado sem credenciais (`terraform validate`, `helm lint`, build das imagens); nenhum `apply` sem a confirmação explícita do usuário.

## Critério de pronto
1. `./gradlew build` verde, com os testes de contrato dos três pares HTTP (`transfers` → `accounts`, `accounts` → `customer`, `transfers` → `customer`) e dos eventos consumidos (`accounts` → `statement`/`notification`, `transfers` → `notification`, `customer` → consumidores, se houver).
2. Cada serviço tem `Dockerfile` em camadas; o CI constrói as cinco imagens e roda o smoke contra elas (compose com as imagens no lugar dos jars).
3. `helm lint` e `helm template` do chart com os cinco arquivos de valores, validados contra o schema do Kubernetes (`kubeconform`) no CI.
4. Terraform `bootstrap` e `env` com `fmt -check`, `validate` e `tflint` verdes no CI.
5. Workflows `up`, `down` e `reaper` escritos e validados (`actionlint`); o `reaper` derruba um ambiente com mais de 8 h, provado por um teste do script de decisão (sem AWS).
6. README com o diagrama da arquitetura (Mermaid), como rodar localmente e como subir na AWS; ADRs revistos.
7. Com a conta da AWS entregue: um `up` real, o seed do dono, o smoke contra a URL pública, e um `down` (ou o `reaper`) que deixa a conta sem recurso cobrado além do `bootstrap`. Este item espera o usuário e não bloqueia o merge do resto.

## Restrições
- Nenhum recurso cobrado fica de pé sem a derrubada automática e o alarme de orçamento já ativos (ADR 0019, #146). O alarme e o papel do GitHub vivem no `bootstrap`, que sobe antes de qualquer `env`.
- Segredos nunca em arquivo versionado. Senhas geradas pelo Terraform, guardadas no Secrets Manager e entregues aos pods; segredo do app client de máquina idem.
- Código e testes em inglês; docs e ADRs em português. Regras do `CLAUDE.md` valem para tudo (Clean Architecture, outbox, idempotência).

## Abordagens consideradas
- **Derrubada:** (escolhida) workflow agendado a cada 30 min que lê a etiqueta `created-at` do ambiente e roda `terraform destroy` passadas 8 h, mais `down` manual; Budgets como segunda rede. Descartadas: Lambda + EventBridge (duplica o Terraform e a lógica, e um `destroy` de dentro da AWS não tem o estado na mão); só o alarme de orçamento (avisa, não derruba).
- **Estado do Terraform:** (escolhida, proposta da coordenadora) dois stacks. O `bootstrap`, permanente e quase sem custo: bucket S3 do estado (com lock nativo do S3), papel OIDC do GitHub, repositórios ECR e o Budget. O `env`, efêmero: rede, EKS, dados, mensageria, identidade e borda. Descartada: estado local (o workflow agendado não teria o estado para destruir).
- **Imagens:** Dockerfile em camadas (escolhido pelo usuário, visível no portfólio). Descartados: Jib, `bootBuildImage`.
- **Contratos:** Stubborn Contract (ADR 0022; sucessor do Spring Cloud Contract, que foi descontinuado). Descartado: Pact (pede broker ou arranjo próprio no monorepo).

## 1. Imagens
- Um `Dockerfile` por serviço (`services/<serviço>/Dockerfile`), multi-stage: a primeira etapa extrai as camadas do jar (`java -Djarmode=tools -jar app.jar extract --layers --launcher`), a segunda copia camada por camada numa imagem `eclipse-temurin:21-jre` com usuário não root, `HEALTHCHECK` não (o Kubernetes usa as probes).
- O jar vem do `bootJar` já construído pelo Gradle; a imagem não compila.
- Etiqueta da imagem: o SHA do commit. O CI constrói as cinco; o `up` publica no ECR.
- O compose local ganha um perfil que usa as imagens no lugar dos processos `java -jar`, e o smoke do CI passa a rodar contra ele.

## 2. Chart Helm
- `deploy/helm/service`: um chart com Deployment, Service (ClusterIP), ServiceAccount (com Pod Identity), ConfigMap de variáveis, `PodDisruptionBudget` e probes no `/actuator/health/liveness` e `/readiness`.
- `deploy/helm/values/<serviço>.yaml`: porta, imagem, réplicas (1), recursos (pedidos baixos, cabe em 2× `t3.medium`), variáveis de ambiente (emissor do Cognito, `cognito:groups`, URLs dos outros serviços pelo DNS de Service, nomes de tópicos, filas e tabelas), segredos referenciados.
- ALB interno criado pelo **Terraform** (módulo `edge-alb`), com um target group por serviço e as regras por caminho iguais às do nginx local; o chart liga os pods a ele por `TargetGroupBinding` (AWS Load Balancer Controller). Assim o `env` sobe num só `apply`, o VPC Link da borda recebe o listener na mesma rodada e o `destroy` apaga o ALB, sem sobra criada pelo controller. *(Escolha da coordenadora: evita o `env` em duas fases.)*
- Perfil Spring `aws` ativo nos pods (desliga Swagger e o endpoint do LocalStack, débito #40/#44).

## 3. Terraform
- `infra/terraform/bootstrap`: bucket do estado, papel OIDC do GitHub (restrito ao repositório e à `main`), ECR (um repositório por serviço, com política de expiração), AWS Budgets (US$ 20/mês, 50/80/100%, e-mail em variável sensível).
- `infra/terraform/envs/aws` (efêmero), compondo módulos novos e os da etapa 7:
  - `network`: VPC, duas zonas, sub-redes públicas e privadas, um NAT Gateway;
  - `eks`: cluster, node group gerenciado 2× `t3.medium`, add-ons (VPC CNI, CoreDNS, kube-proxy, Pod Identity Agent), AWS Load Balancer Controller via Helm;
  - `data`: um RDS PostgreSQL `db.t4g.micro` com os bancos e usuários de cada serviço (como o `infra/local/postgres`), ElastiCache Redis `cache.t4g.micro`, tabela DynamoDB do extrato;
  - `messaging`: os tópicos SNS FIFO e as filas SQS FIFO com DLQ, iguais aos scripts do LocalStack;
  - `identity` e `edge` (etapa 7), agora ligados ao ALB do Ingress;
  - etiqueta `created-at` e `project=digitalbanking` em tudo.
- As migrações continuam pelo Flyway na subida de cada serviço; os grants de usuário `_app` saem de um Job de inicialização ou do provider PostgreSQL (escolha do construtor).

## 4. Workflows
- `aws-up` (`workflow_dispatch`): aplica o `env`, constrói e publica as imagens, instala o chart dos cinco serviços, cria o usuário do dono no Cognito (grupo `admin`) e roda o seed e o smoke contra a URL da borda. Escreve a URL e o horário limite no resumo do job.
- **Tokens na AWS** *(escolha da coordenadora)*: o seed e o smoke criam seus usuários no Cognito com senha aleatória gerada no próprio job (não guardada) e pegam o token por `AdminInitiateAuth` (`ADMIN_USER_PASSWORD_AUTH`), que exige credenciais IAM do papel do GitHub; o fluxo não fica aberto ao público. O app client web continua sem esse fluxo. A senha do dono vem do segredo do repositório `OWNER_PASSWORD`, definido pelo usuário; os usuários de smoke são apagados ao fim do job.
- **Smoke na AWS** *(escolha da coordenadora)*: o mesmo `smoke.sh` com um modo `--remote` que pula a leitura das filas de inspeção e confere os efeitos dos eventos pelas APIs (notificações e extrato); o Terraform não cria filas de inspeção.
- `aws-down` (`workflow_dispatch`): `helm uninstall` e `terraform destroy` do `env`. Depois, confere pela Resource Groups Tagging API e por ENIs na VPC se sobrou algo; se sobrou, o job falha e lista os recursos (não apaga fora do Terraform, para o papel do GitHub não precisar de permissão de delete ampla). *(Escolha da coordenadora.)*
- **Papel OIDC** *(escolha da coordenadora)*: os três workflows rodam só da `main`; a trust policy aceita apenas `repo:capozziV/DigitalBanking:ref:refs/heads/main`. Os workflows só são exercitados depois do merge.
- `aws-reaper` (agendado a cada 30 min): se existe ambiente e a etiqueta `created-at` passou de 8 h, chama o `aws-down`. A decisão fica num script testável sem AWS.
- Nenhum dos três roda sem as variáveis do repositório que só existem depois do `bootstrap` aplicado pelo usuário.

## 5. Testes de contrato
- Stubborn Contract (ADR 0022) no lado de quem responde: contratos em `src/contractTest/resources/contracts` do `accounts` (consultas, débito, crédito, busca de lançamento) e do `customer` (`/internal/customers/{id}`), com token de serviço. O build gera os testes do produtor e o jar de stubs.
- Quem chama (`transfers`, `accounts`) troca os stubs escritos à mão pelos stubs do contrato (Stub Runner, WireMock) nos testes de integração dos gateways HTTP.
- Eventos *(escolha da coordenadora)*: contratos de mensagem do Stubborn Contract (ADR 0022) no nível do envelope do evento, sem SNS/SQS no teste. No produtor, um `MessageVerifier` próprio e pequeno publica pelo mapeador do outbox e lê o envelope gravado; no consumidor, a mensagem do contrato é entregue direto ao tratador do listener. A API do Stubborn Contract 0.2.2 para mensagens é conferida na implementação; se não cobrir o envelope, o construtor para e avisa.

## 6. README e diagrama
- `README.md` na raiz: o que é, arquitetura com diagrama Mermaid (borda, serviços, bancos, mensageria), os pontos fortes (consistência, idempotência, outbox, saga, cache, limite diário), como rodar localmente, como subir na AWS e quanto custa por subida.

## 7. Revisão dos ADRs
- ADR 0001 (ECS Fargate) marcado como substituído onde o 0019 já decidiu.
- ADR novo de deploy e ambiente efêmero (stacks, derrubada, orçamento, Helm, imagens).
- ADR novo de testes de contrato.
- Releitura dos 20 ADRs: status e links coerentes com o código.

## Fora de escopo
- Domínio próprio e HTTPS com certificado próprio (a borda usa a URL do API Gateway).
- Observabilidade na AWS além dos logs no CloudWatch (sem dashboards nem alarmes de aplicação).
- Alta disponibilidade (uma réplica por serviço, RDS sem Multi-AZ).
- Frontend e BFF (etapa 9).
- Encerramento e bloqueio de conta (#27): tarefa própria, fora desta spec.

## Riscos
- **Custo esquecido:** mitigado pelo `reaper`, pelo `down` manual e pelo Budget; o `bootstrap` custa centavos por mês (S3, ECR).
- **Destroy incompleto** (ENIs do ALB, recursos criados pelo controller): o `aws-down` desinstala o chart antes do `destroy` e confere recursos com a etiqueta do projeto ao fim.
- **Tempo de subida:** EKS leva ~15 min; o `up` todo, ~25 min. Aceito.
- **Nada testado na AWS até o usuário entregar a conta:** o critério 7 fica pendente sem bloquear o resto.
