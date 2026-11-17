# Chart Helm dos serviços

Um chart (`service/`) para os cinco serviços; cada um tem o seu `values/<servico>.yaml`. Validação sem cluster: `./validate.sh` (helm lint, template, kubeconform `-strict` na versão `KUBERNETES_VERSION`, padrão 1.36.0, a mesma do EKS, e regras do projeto).

## Valores obrigatórios
- `image.repository`, `image.tag` (SHA do commit) e `targetGroupArn` entram na instalação.
- `requiredEnv` (lista em cada `values/<servico>.yaml`): chaves de `env` que não podem ficar vazias nem ausentes (URL de banco, issuer, ARN de tópico, `REDIS_HOST`). O render falha sem elas, em vez de o pod subir com o padrão local do `application.yml`. Informe com `--set-string env.<CHAVE>=...` ou `-f`.
- `check-env-vars.sh` confere que todo placeholder sem padrão, ou com padrão local (`localhost`, `000000000000`, `local-secret`, `*_app`, `*_owner`), dos `application*.yml` está no `env` ou no `secretEnv` do values.

## ServiceAccount
`serviceAccountName` do Deployment, `metadata.name` da ServiceAccount e nome do serviço são o mesmo valor (o `validate.sh` afirma). A associação do Pod Identity (Terraform) usa esse nome e o namespace do release.

## Propriedades sem placeholder
`TRANSFERS_ACCOUNTS_URL` (e `TRANSFERS_CUSTOMER_BASE_URL`) funciona por relaxed binding de `transfers.accounts.url`: não há `${...}` no YAML, então o `check-env-vars.sh` não a enxerga. Fica no `env` do `values/transfers.yaml` por convenção.
