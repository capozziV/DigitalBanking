# Chart Helm dos serviços

Um chart (`service/`) para os cinco serviços; cada um tem o seu `values/<servico>.yaml`. Validação sem cluster: `./validate.sh` (helm lint, template, kubeconform `-strict` na versão do EKS, lida do `kubernetes_version` do Terraform por `infra/terraform/envs/aws/kubernetes-version.sh` (fonte única; `KUBERNETES_VERSION` sobrepõe), e regras do projeto).

## Valores obrigatórios
- `image.repository`, `image.tag` (SHA do commit) e `targetGroupArn` entram na instalação.
- `requiredEnv` (lista em cada `values/<servico>.yaml`): chaves de `env` que não podem ficar vazias nem ausentes (URL de banco, issuer, ARN de tópico, `REDIS_HOST`). O render falha sem elas, em vez de o pod subir com o padrão local do `application.yml`. Informe com `--set-string env.<CHAVE>=...` ou `-f`.
- `check-env-vars.sh` confere que todo placeholder sem padrão, ou com padrão local (`localhost`, `000000000000`, `local-secret`, `*_app`, `*_owner`), dos `application*.yml` está no `env` ou no `secretEnv` do values.

## ServiceAccount e Pod Identity
`serviceAccountName` do Deployment, `metadata.name` da ServiceAccount e nome do serviço são o mesmo valor (o `validate.sh` afirma). A associação do Pod Identity (Terraform, `modules/access`) usa esse nome e o namespace do release: instale com `-n digitalbanking` (`kubernetes_namespace` do Terraform). A ServiceAccount não leva a anotação IRSA `eks.amazonaws.com/role-arn` (o `validate.sh` recusa): com ela o SDK tentaria web identity, e a trust dos papéis só aceita o Pod Identity. `infra/terraform/envs/aws/check-service-names.py` confere que os `name:` dos values batem com os papéis do Terraform.

## Região
Os values trazem `*_AWS_REGION: sa-east-1`, a região do ambiente (`region` no Terraform); o `validate.sh` recusa outra. O ambiente local continua com `us-east-1` no `.env` e no LocalStack.

## Job `db-init`
O chart `db-init/` cria, no RDS privado, o banco, o dono (`<servico>_owner`, do Flyway) e o usuário de aplicação (`<servico>_app`) de cada serviço com PostgreSQL, como `infra/local/postgres/init` no compose. É um Job em hook `post-install,post-upgrade`, idempotente, com a ServiceAccount `db-init` (papel do Pod Identity que só lê a senha mestre e os segredos `<name>/<servico>-db`). Um init container com o CLI da AWS lê os segredos para um volume em memória; o contêiner do PostgreSQL roda o `psql`.
- Instalação: `helm upgrade --install db-init deploy/helm/db-init -n digitalbanking --set dbHost=<db_address> --set masterSecretId=<db_master_secret_arn> --set secretPrefix=<name>`, antes dos serviços. No `aws-up`, quem instala é `infra/aws/deploy-services.sh`, que depois confere `status.succeeded` do Job antes de subir os serviços.
- **O Job continua no cluster depois do `helm uninstall`.** Ele é hook (`hook-delete-policy: before-hook-creation`), e o Helm não apaga hooks no uninstall: só o próximo install o troca. O `aws-down` o apaga com `kubectl delete job db-init` depois do uninstall; à mão, faça o mesmo.
- `db-init/test-local.sh` (precisa de Docker) roda o script contra um PostgreSQL 17 com o mestre sem superusuário, como no RDS: duas execuções, troca de senha, dono criando tabela e segredo incompleto recusado.

## Instalação na AWS
`infra/aws/deploy-services.sh <saídas do terraform output -json> <registro ECR> <SHA>` instala o `db-init` e os cinco serviços no namespace `digitalbanking`, com `--wait`. Cada chave de `requiredEnv` sai de uma saída do Terraform pelo sufixo (`DB_URL`, `JWT_ISSUER_URI`, `OAUTH_TOKEN_URI`, `OAUTH_CLIENT_ID`, `TOPIC_ARN`, `REDIS_HOST`); uma chave nova sem origem ali falha a instalação. Os Secrets do Kubernetes (`<servico>-db`, `<servico>-oauth`) saem do Secrets Manager por pipe, sem arquivo. Nenhum ARN de papel vai aos charts. `infra/aws/deploy-services.test.sh` renderiza tudo com saídas fictícias (`infra/aws/testdata/`) e passa no kubeconform, sem cluster nem AWS.

## Propriedades sem placeholder
`TRANSFERS_ACCOUNTS_URL` (e `TRANSFERS_CUSTOMER_BASE_URL`) funciona por relaxed binding de `transfers.accounts.url`: não há `${...}` no YAML, então o `check-env-vars.sh` não a enxerga. Fica no `env` do `values/transfers.yaml` por convenção.

## App do cliente (web e bff)
O mesmo chart instala o `web` (Angular em nginx sem root, porta 80, probes em `/`) e o `bff` (NestJS, porta 3000, probes em `/bff/health`). Eles não são Spring: `springProfile: ""` tira o `SPRING_PROFILES_ACTIVE` do ConfigMap, e não têm papel no Pod Identity nem segredo (o `check-service-names.py` os trata à parte). O `web` liga o sysctl `net.ipv4.ip_unprivileged_port_start=0` para escutar na 80 sem root. O `bff` pede `COGNITO_CLIENT_ID` (saída `cognito_bff_client_id`) e `REDIS_URL` (`redis://<redis_endpoint>:6379`) no `requiredEnv`; as URLs dos serviços são os Services do namespace. O `deploy-services.sh` instala o `bff` e depois o `web`, e o `validate.sh` confere as probes contra o `health` do `routes.json`. Os repositórios ECR `web` e `bff` vêm do stack `bootstrap` (`var.services`): reaplique o bootstrap antes do primeiro `aws-up` com o app.

## Coletor ADOT (etapa 10)
`otel-collector/` é um chart à parte (Deployment de uma réplica, Service `otel-collector:4318`, ServiceAccount `otel-collector`) com a configuração do ADOT em `files/config.yaml`: OTLP/HTTP de entrada; traces para o endpoint OTLP do X-Ray, métricas filtradas para o CloudWatch por EMF e logs em `/capzzpay/<servico>`. Os values dos serviços e do bff trazem `OTEL_EXPORTER_OTLP_ENDPOINT` e `OTEL_SERVICE_NAME`. O `deploy-services.sh` o instala antes de tudo e o `helm-teardown.sh` o remove por último. `otel-collector/check-config.sh` (docker) sobe a imagem real com a configuração renderizada contra um servidor da AWS falso e confere trace assinado para o xray, EMF, grupo de logs e filtro; o `validate.sh` o chama (`OTEL_CONFIG_CHECK=skip` pula, só fora do CI). `infra/terraform/envs/aws/check-observability.py` confere os nomes entre chart, Terraform e values. Pesquisa e custo: `docs/pesquisa/2026-11-23-observabilidade-aws.md`.
