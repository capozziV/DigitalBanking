# Observabilidade na AWS (etapa 10)

Grupos de logs (`/capzzpay/<servico>` e `/capzzpay/metrics`, retenção de 1 dia), Transaction Search do X-Ray e o painel
`capzzpay-transferencias`. O coletor é o chart `deploy/helm/otel-collector`; o papel dele está em `modules/access`.

- **Transaction Search é da conta, não do ambiente.** `aws_xray_trace_segment_destination` e a política de recurso
  do X-Ray no `aws/spans` sobem com o ambiente, mas o destroy não desfaz o destino (a API não tem "desligar"): a conta
  fica com os spans indo para o CloudWatch Logs, com 1% indexado de graça. O grupo `aws/spans` é criado pelo X-Ray, sem
  a etiqueta `project`: o `leftovers.sh` não o enxerga e a retenção dele não é do Terraform.
- Nunca aplicado: a prova real (subir, transferir, ver traces e painel) fica no #182.
