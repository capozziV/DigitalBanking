# Chart Helm dos serviços

Um chart (`service/`) para os cinco serviços; cada um tem o seu `values/<servico>.yaml`. Validação sem cluster: `./validate.sh` (helm lint, template, kubeconform `-strict` na versão `KUBERNETES_VERSION`, padrão 1.36.0, a mesma do EKS, e regras do projeto).

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
- Instalação: `helm upgrade --install db-init deploy/helm/db-init -n digitalbanking --set dbHost=<db_address> --set masterSecretId=<db_master_secret_arn> --set secretPrefix=<name>`, antes dos serviços.
- `db-init/test-local.sh` (precisa de Docker) roda o script contra um PostgreSQL 17 com o mestre sem superusuário, como no RDS: duas execuções, troca de senha, dono criando tabela e segredo incompleto recusado.

## Propriedades sem placeholder
`TRANSFERS_ACCOUNTS_URL` (e `TRANSFERS_CUSTOMER_BASE_URL`) funciona por relaxed binding de `transfers.accounts.url`: não há `${...}` no YAML, então o `check-env-vars.sh` não a enxerga. Fica no `env` do `values/transfers.yaml` por convenção.
