# Terraform

Infraestrutura na AWS do banco digital, em `sa-east-1` ([ADR 0019](../../docs/adr/0019-runtime-na-aws-com-eks.md)). O stack `envs/aws` é o ambiente efêmero: rede, EKS, ALB interno, identidade, borda, dados, mensageria e acessos por serviço.

**Nada foi aplicado.** O código só é validado (`fmt`, `init -backend=false`, `validate`, `tflint`), sem credenciais da AWS. Os dois stacks rodam no job `terraform` do CI pelos próprios `validate.sh`. Nenhum `apply` antes da derrubada automática e do alarme de orçamento (#146), e sempre com confirmação explícita do usuário.

## Estrutura
- `modules/identity`: User Pool do Cognito (login por e-mail, autocadastro com código por e-mail padrão do Cognito, senha de mínimo 6 sem classes; o limite diário do e-mail padrão é baixo, suficiente para a demonstração), grupo `admin`, resource server `digitalbanking` com o escopo `internal`, app client web (authorization code + PKCE, sem segredo), app client `bff` (`USER_PASSWORD_AUTH` e refresh, sem segredo; output `cognito_bff_client_id`), app client de máquina (client credentials, escopo `digitalbanking/internal`) e app client de operação `ops` (só `ADMIN_USER_PASSWORD_AUTH`, sem segredo, token de 1 h), que o seed e o smoke do `aws-up` usam por `AdminInitiateAuth`. Esse fluxo exige credenciais IAM; o app client web continua sem ele.
- `modules/edge`: API Gateway HTTP API, autorizador JWT do Cognito (audiências: os app clients web, de máquina, `ops` e `bff`), VPC Link e uma integração `HTTP_PROXY` para o listener do ALB interno. Rotas `ANY /prefixo` e `ANY /prefixo/{proxy+}` para `/accounts`, `/transfers`, `/notifications`, `/customers` e `/admin`; extrato e limite diário ficam sob `/accounts` e `/transfers`. Todas exigem JWT, exceto `/bff` e a rota padrão `$default` (o web em `/`), públicos: o BFF autentica pelo cookie de sessão. O ALB ganha os serviços `web` (80) e `bff` (3000), com `/bff` na prioridade 400 e o catch-all `/*` do web na 900.
- `modules/network`: VPC `10.20.0.0/16` em duas zonas, sub-redes públicas (só o NAT) e privadas, um NAT Gateway com EIP.
- `modules/eks`: cluster (suporte padrão, sem o estendido, que cobra a mais), node group gerenciado 2× `t3.medium` (AL2023) nas sub-redes privadas, add-ons gerenciados (VPC CNI, kube-proxy, CoreDNS, Pod Identity Agent) e o AWS Load Balancer Controller (chart `3.6.2`) por `helm_release`, com papel por Pod Identity.
- `modules/edge-alb`: ALB interno, listener HTTP 80 (o que nenhuma regra casa cai no web, pelo catch-all `/*` da prioridade 900; o 404 fixo do listener só sobra como padrão), um target group por serviço (alvo por IP; health check em `/actuator/health/readiness` nos cinco serviços Spring, em `/` no web e em `/bff/health` no BFF, conforme `health` do `routes.json`) e as regras por caminho de `routes.json`, espelho das `location` do nginx local.
- `modules/data`: RDS PostgreSQL 17 `db.t4g.micro` (senha mestre gerada e guardada pelo próprio RDS no Secrets Manager), ElastiCache Redis 7.1 `cache.t4g.micro` e os segredos por serviço (`<name>/<servico>-db` com `*_DB_PASSWORD` e `*_DB_OWNER_PASSWORD`; `<name>/accounts-oauth` e `<name>/transfers-oauth` com o segredo do app client de máquina). RDS e Redis só aceitam o security group do cluster do EKS.
- `modules/messaging`: tópicos SNS FIFO, filas SQS FIFO com DLQ (visibilidade 30 s, 5 recebimentos; a `statement-accounts.fifo` tem 120, cerca de 1 hora de retentativa), assinaturas com entrega crua e a tabela DynamoDB `statement`, tudo a partir de `resources.json`, espelho dos recursos locais: os scripts do LocalStack (SNS e SQS) e a tabela do DynamoDB Local (`infra/local/dynamodb/init/statement-table.json`).
- `modules/access`: um papel IAM por serviço (Pod Identity), ligado à ServiceAccount de mesmo nome no namespace do release (`kubernetes_namespace`, padrão `digitalbanking`), e o papel do Job `db-init`, que só lê (`secretsmanager:GetSecretValue`) a senha mestre do RDS e os segredos `<name>/<servico>-db`. `accounts`, `transfers` e `customer` só publicam no próprio tópico; `notification` e `statement` consomem as próprias filas e leem os atributos das DLQs; `statement` também usa `PutItem`, `GetItem` e `Query` na própria tabela. Sempre com ARN específico.
- `envs/aws`: compõe tudo; o VPC Link da borda aponta para o listener do ALB. Backend `s3` com configuração parcial e lock nativo (`use_lockfile`); o bucket vem do stack `bootstrap`.

## ALB do Terraform, nunca do controller
O `terraform destroy` precisa levar o ALB, as ENIs dele, o NAT e o EIP. Por isso o ALB e os target groups são do Terraform, e o AWS Load Balancer Controller só registra os IPs dos pods (`TargetGroupBinding` do chart Helm, com `targetGroupArn` da saída `target_group_arns`):
- o papel do controller só lê (`Describe*`) e registra ou tira alvos nos target groups deste ambiente; sem permissão para criar load balancer, target group ou regra de security group;
- sem `IngressClass`, com o controller de Service (NLB) e o Gateway API desligados;
- o `TargetGroupBinding` não usa `networking`: as regras ALB → pods (portas 8081–8085 no security group do cluster) são do Terraform.
O `aws-down` desinstala o chart dos serviços antes do `destroy`, para os `TargetGroupBinding` saírem com o controller ainda de pé.

## Roteamento do ALB
`modules/edge-alb/routes.json` traz os serviços (porta) e as regras. As regex do limite diário (`transfers`) e do extrato (`statement`) têm prioridade menor que o prefixo `/accounts/`, como no nginx. As `location` do actuator ficam só no local: o ALB checa a saúde direto no pod e a borda não expõe o actuator. `envs/aws/check-alb-routes.py` compara os casadores do ALB com as `location` do nginx (exato, prefixo, regex e serviço pela porta) e falha se divergirem.

## Mensageria igual ao LocalStack
`envs/aws/check-messaging.py` interpreta os scripts de `infra/local/localstack/init/ready.d` (atribuições, `$(...)`, a função `create_consumer_queue`; comentários ignorados) e confere cada `create-topic`, `create-queue` e `subscribe` sozinho, sem deixar o último script esconder os anteriores. Falha se diferirem de `modules/messaging/resources.json` e do `main.tf` em tópicos, filas, DLQs, assinaturas, `FifoTopic`/`FifoQueue`, `ContentBasedDeduplication=false`, `VisibilityTimeout` e `RedrivePolicy` (DLQ e `maxReceiveCount`) nos atributos da fila, `RawMessageDelivery=true` ou na tabela do extrato. As filas `*-inspection.fifo` são só locais (sem consumidor) e ficam de fora da comparação de nomes.

## Bancos e usuários por serviço
O Terraform não cria os bancos e usuários dentro do RDS: ele fica em sub-rede privada e o runner não o alcança. Quem cria é o Job do chart [`deploy/helm/db-init`](../../deploy/helm/db-init), o mesmo papel de `infra/local/postgres/init` no compose: para `accounts`, `transfers`, `notification` e `customer` (o `statement` não usa PostgreSQL), o banco `<servico>`, o dono `<servico>_owner` (o Flyway migra com ele, e as migrations dão os `grant` ao `<servico>_app`) e o usuário `<servico>_app`. Idempotente; a senha dos roles é sempre a do segredo.
- O Job lê a senha mestre (`db_master_secret_arn`) e os segredos `<name>/<servico>-db` pelo Pod Identity da ServiceAccount `db-init` (papel `db_init_role_arn`), e os guarda só num volume em memória.
- No RDS o usuário mestre não é superusuário: o Job se dá o role `<servico>_owner` (`WITH SET TRUE`) antes do `CREATE DATABASE ... OWNER`, exigência do PostgreSQL 16+. `deploy/helm/db-init/test-local.sh` prova isso com um mestre sem superusuário em Docker.
- Instalação (Task 6), antes dos serviços e no mesmo namespace: `helm upgrade --install db-init deploy/helm/db-init -n digitalbanking --set dbHost=<db_address> --set masterSecretId=<db_master_secret_arn> --set secretPrefix=<name>`. O Job é hook `post-install,post-upgrade`: o `helm` espera o fim dele e falha junto.
- Os Secrets do Kubernetes dos serviços saem dos mesmos segredos, pelo runner que instala o chart.

## Namespace `digitalbanking`
As associações do Pod Identity valem só no namespace `kubernetes_namespace` (padrão `digitalbanking`). Os charts não fixam namespace: a Task 6 instala `db-init` e os cinco serviços com `-n digitalbanking` (ou passa outro `kubernetes_namespace` ao Terraform). Em outro namespace os pods sobem sem papel e sem acesso à AWS.

## X-Ray Transaction Search no `bootstrap`
O `bootstrap` cria o grupo `aws/spans` (retenção de 1 dia), a política de recurso `capzzpay-xray-spans` e o `aws_xray_trace_segment_destination` (CloudWatch Logs): é da conta, sobrevive ao ambiente e o `destroy` do destino é um no-op no provider. O papel do GitHub já é `AdministratorAccess`, que cobre `logs:PutRetentionPolicy`, `logs:PutResourcePolicy` e `xray:UpdateTraceSegmentDestination`; o deny de guardrails só barra `budgets:*`, `iam:*` do próprio papel e o bucket do estado, então nada novo foi liberado. Quem aplica o `bootstrap` (o usuário, local) precisa dessas mesmas permissões. Se o X-Ray já criou o `aws/spans` (Transaction Search ligado antes, no console), importe antes do `apply`: `terraform import aws_cloudwatch_log_group.xray_spans aws/spans`. O `envs/aws/check-observability.py` confere que esses recursos moram no `bootstrap` e não voltaram ao módulo.

## Nomes iguais no Terraform e nos charts
`envs/aws/check-service-names.py` falha se as chaves de `local.services` do `modules/access` diferirem dos `name:` de `deploy/helm/values/*.yaml` (a ServiceAccount do chart leva o nome do serviço), se `database_services` do `modules/data` diferir dos `services` do `db-init` ou dos values com `<SERVICO>_DB_URL`, ou se a ServiceAccount do `db-init` diferir de `db_init_service_account`.

## Etiquetas e `created_at`
`default_tags` põe `project=digitalbanking` e `created-at` em todo recurso do provider. `created_at` (RFC 3339) não tem padrão: vem do workflow na subida, e o `aws-reaper` lê a etiqueta. Recursos que o EKS cria por conta própria (instâncias e ENIs do node group, security group do cluster) não recebem `default_tags`, mas saem com o node group e o cluster.

## Cognito: domínio e PKCE
- O módulo `identity` cria o `aws_cognito_user_pool_domain`; sem ele não existe `/oauth2/token` e o `client_credentials` não funciona. O prefixo vem da variável `cognito_domain_prefix` (sem padrão, precisa ser único na região). Saídas: `cognito_domain` e `token_endpoint_url`.
- O Cognito não impõe PKCE no servidor. Quem aplica é o frontend (etapa 9), que usa o app client web, público e sem segredo.

## Borda: limites e log de acesso
- O stage `$default` limita taxa e rajada em dois níveis. As rotas da API (com JWT) usam `default_route_settings`: `throttling_rate_limit`, padrão 10 req/s, e `throttling_burst_limit`, padrão 20, baixos por ser ambiente de portfólio. As rotas do app (`$default`, que serve os estáticos do web, e `/bff`) têm `route_settings` próprios: `app_throttling_rate_limit`, padrão 50 req/s, e `app_throttling_burst_limit`, padrão 150. A conta está nas variáveis: uma carga do app faz cerca de 12 requisições, então 10 visitantes juntos somam ~120 de rajada, mais ~15 req/s sustentados de polling. `envs/aws/check-edge-throttling.py` (no `validate.sh`) falha se o app voltar a herdar o limite da API.
- O log de acesso vai para um Log Group do CloudWatch (`/aws/apigateway/<nome>-http/access`) com retenção de 7 dias (`access_log_retention_days`).

## Saídas para as outras tasks
`cluster_name`, `user_pool_id`, `ops_client_id`, `target_group_arns` (serviço → ARN), `edge_url`, `private_subnet_ids`, `vpc_id`, `cluster_security_group_id`, `service_role_arns`, `db_init_role_arn`, `secret_names`, `queue_urls`, `topic_arns`, `db_endpoint`, `db_address`, `db_master_secret_arn` e `redis_endpoint`, além das da identidade. Os papéis chegam aos pods pelo Pod Identity: os charts não recebem ARN de papel (sem anotação IRSA).

## Observações
- O autorizador da borda só valida emissor e audiência (os quatro app clients: web, de máquina, `ops` e `bff`). A checagem do grupo `admin` (claim `cognito:groups`) e do escopo `internal` é dos serviços, que também validam o JWT (ADR 0004).
- O segredo do app client de máquina sai como saída sensível (`machine_client_secret`) e fica no estado; nunca em arquivo versionado. `*.tfstate` e `*.tfvars` estão no `.gitignore`.
- **Senhas no estado.** As senhas dos serviços (`random_password`, dono e aplicação) e o segredo do app client de máquina ficam no estado do Terraform, no bucket do stack `bootstrap` (privado, criptografado e versionado; as versões antigas do estado também as guardam). Aceitável no ambiente efêmero e simulado, sem dado real. A senha mestre do RDS não passa pelo estado (`manage_master_user_password`). `ephemeral` + `secret_string_wo` tiraria as senhas do estado, mas exige Terraform 1.11+ e o repositório trava `>= 1.9`.
- O `.terraform.lock.hcl` fixa os providers `hashicorp/aws` (`~> 6.70`) e `hashicorp/helm` (`~> 3.1`) para linux e macOS.
- O provider `helm` pega o token do cluster pelo `aws eks get-token`: quem aplica precisa do aws CLI e é o administrador do cluster (`bootstrap_cluster_creator_admin_permissions`).

## Workflows na AWS
`aws-up`, `aws-down` e `aws-reaper` (`.github/workflows/`), só da `main` (a trust do papel aceita só `repo:capozziV/DigitalBanking:ref:refs/heads/main`), no grupo de concorrência `aws-environment`. Configuração do repositório, nunca versionada: `vars.AWS_ROLE_ARN` (`github_role_arn`), `vars.TF_STATE_BUCKET` (`state_bucket`), `vars.COGNITO_DOMAIN_PREFIX`, `vars.OWNER_EMAIL` (opcional) e `secrets.OWNER_PASSWORD`. Estado em `envs/aws/terraform.tfstate` no bucket do `bootstrap`.
- **`aws-up`** (manual): constrói as imagens, `apply` com `created_at` em RFC 3339 UTC (num ambiente que já existe, mantém a etiqueta original: subir de novo não estende o prazo), publica no ECR (pula a tag que já existe, os repositórios são imutáveis), roda `infra/aws/deploy-services.sh` (db-init, Secrets e serviços), o seed (`seed-check.sh --remote`) e o smoke (`smoke.sh --remote`) pela borda, com tokens do Cognito por `infra/aws/cognito-user.sh`. O resumo do job traz a URL da borda e o horário a partir do qual o `aws-reaper` derruba. A sessão do papel dura 1 h: o job pede credenciais de novo antes de cada fase longa.
- **`aws-down`** (manual ou chamado pelo `aws-reaper`): `helm uninstall` dos serviços e do db-init (e `kubectl delete job db-init`), espera os `TargetGroupBinding` saírem, `terraform destroy` e `infra/aws/leftovers.sh`, que lista pela Tagging API o que tem `project=digitalbanking` fora de `stack=bootstrap` e as ENIs da VPC antiga; sobrou algo, o job falha com a lista (não apaga nada fora do Terraform).
- **`aws-reaper`** (a cada 30 min): `infra/aws/reaper-check.sh` lê o `created-at` de cada recurso do env e passa cada valor por `infra/aws/reaper-decision.sh`; 8 h ou mais, etiqueta ausente, malformada ou mais de 10 min no futuro → chama o `aws-down`. Sem `vars.AWS_ROLE_ARN` não faz nada. O GitHub suspende agendamentos de repositório sem atividade por 60 dias; o Budget é a segunda rede.
- Testes sem AWS, no job `aws-scripts` do CI: `infra/aws/reaper-decision.test.sh`, `reaper-check.test.sh`, `leftovers.test.sh` e `helm-teardown.test.sh` (aws, helm e kubectl falsos no `PATH`).
- **Falha no `helm uninstall` do `aws-down`**: o passo tem `continue-on-error`; o `terraform destroy` e o `leftovers.sh` rodam sempre e o job termina vermelho. O ambiente não fica cobrando por causa do Helm.
- **Prazo real**: o reaper derruba a partir de 8 h, mas o cron do GitHub atrasa ou pula execuções em horário de carga. O máximo é 8 h + até 30 min + o atraso + a duração do destroy.
- **Reaper vermelho = ambiente possivelmente ligado**: erro da AWS ao ler as etiquetas não derruba nada (não decide às cegas); o job falha e o GitHub notifica. Confira e rode o `aws-down`.
- **Fila de concorrência**: o grupo `aws-environment` guarda só um job pendente; um `aws-up` manual pode cancelar o `aws-down` do reaper que espera. Por isso o `aws-up` recusa subir sobre um ambiente já vencido (rode o `aws-down`); o próximo ciclo do reaper também repete a derrubada.
- **kubectl**: a ação `aws-tools` usa o kubectl da imagem `ubuntu-latest` se existir; ele pode sair da janela de ±1 versão menor do EKS. Só se faltar, instala a versão do `kubernetes-version.sh`.
- **Etiquetas antigas**: a Tagging API mostra por um tempo recursos já apagados. Um `aws-up` logo depois de uma derrubada pode ser derrubado pelo reaper por etiquetas velhas (falha segura); espere alguns minutos entre o `aws-down` e o `aws-up`.

## Validar
```bash
infra/terraform/bootstrap/validate.sh
infra/terraform/envs/aws/validate.sh
```
O script roda `terraform fmt -check` em toda a árvore, `init -backend=false -lockfile=readonly`, `validate`, o tflint na raiz e em cada módulo (`network`, `eks`, `edge-alb`, `edge`, `identity`, `messaging`, `data`, `access`), o `check-messaging.py`, o `check-service-names.py` e o `check-alb-routes.py`, que compara as regras do ALB com as `location` do nginx local (recusa `^~` e `~*`, e prefixo de um serviço sombreado por outro). Não precisa de credenciais.

## Decisões do ambiente efêmero
- **Actuator fora da borda.** O ALB não tem regras para `/<servico>/actuator/`: o health check vai direto ao pod e a borda não expõe o actuator. Divergência deliberada do nginx local, que o expõe; na AWS, `/actuator/metrics` (admin) não chega pela borda. O `check-alb-routes.py` falha se um actuator aparecer no ALB. Se virar ADR, é da Clara (ADR 0021).
- **Redis sem TLS nem AUTH.** O ElastiCache aceita só o security group do cluster; sem TLS em trânsito nem senha, como no compose local. Exposição consciente do ambiente efêmero.
- **Endpoint público do EKS aberto.** `public_access_cidrs` fica em `0.0.0.0/0` (padrão): o runner do GitHub não tem IP fixo. A autenticação é por IAM e o ambiente é efêmero e de portfólio; é uma exposição consciente, não restringida.
