# Observabilidade na AWS (etapa 10)

Grupos de logs (`/capzzpay/<servico>` e `/capzzpay/metrics`, retenção de 1 dia), Transaction Search do X-Ray e o painel
`capzzpay-transferencias`. O coletor é o chart `deploy/helm/otel-collector`; o papel dele está em `modules/access`.

- **Transaction Search é da conta, não do ambiente.** `aws_xray_trace_segment_destination` e a política de recurso
  do X-Ray no `aws/spans` sobem com o ambiente. O `destroy` do destino **não faz nada na AWS**: no código do provider
  (`internal/service/xray/trace_segment_destination.go`) o recurso herda `framework.WithNoOpDelete`, que só o tira do
  estado, então a conta continua com o destino em CloudWatch Logs. A política de recurso, essa sim, é apagada. O grupo
  `aws/spans` é criado pelo X-Ray, sem a etiqueta `project` e sem retenção (nunca expira): o `leftovers.sh` não o
  enxerga e a retenção dele não é do Terraform. Pendente de decisão do usuário: levar destino, política e `aws/spans`
  (1 dia) para o `bootstrap`, que é o lugar do que sobrevive ao ambiente.
- **Painel e política de recurso sem etiqueta.** `aws_cloudwatch_dashboard` não aceita tags e a política é da conta:
  o `leftovers.sh` não os acusa. Custo zero (dentro dos 3 painéis grátis).
- **Duração da saga em p50/p95.** O painel depende de o `transfers` mandar a duração como histograma exponencial, em
  delta e em segundos (`MANAGEMENT_OTLP_METRICS_EXPORT_*` em `deploy/helm/values/transfers.yaml`). Com buckets explicitos
  o `awsemf` só publica Count/Sum/Min/Max. O `check-config.sh` do coletor prova o formato aceito.
- Nunca aplicado: a prova real (subir, transferir, ver traces e painel) fica no #182.
