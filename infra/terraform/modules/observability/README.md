# Observabilidade na AWS (etapa 10)

Grupos de logs (`/capzzpay/<servico>` e `/capzzpay/metrics`, retenção de 1 dia), Transaction Search do X-Ray e o painel
`capzzpay-transferencias`. O coletor é o chart `deploy/helm/otel-collector`; o papel dele está em `modules/access`.

- **Transaction Search é da conta e mora no `bootstrap`.** `aws_xray_trace_segment_destination`, a política de recurso
  do X-Ray e o grupo `aws/spans` (retenção de 1 dia, criado antes do destino) ficam em `infra/terraform/bootstrap`, não
  aqui. Motivo: o `destroy` do destino **não faz nada na AWS** (no provider, `internal/service/xray/trace_segment_destination.go`
  herda `framework.WithNoOpDelete`), então no ambiente ele ficaria na conta sem a política (essa, sim, apagada), e o
  `aws/spans` criado pelo X-Ray nasce sem retenção. O ambiente só depende de o `bootstrap` já ter sido aplicado.
- **Painel e política de recurso sem etiqueta.** `aws_cloudwatch_dashboard` não aceita tags e a política é da conta:
  o `leftovers.sh` não os acusa. Custo zero (dentro dos 3 painéis grátis).
- **Duração da saga em p50/p95.** O painel depende de o `transfers` mandar a duração como histograma exponencial, em
  delta e em segundos (`MANAGEMENT_OTLP_METRICS_EXPORT_*` em `deploy/helm/values/transfers.yaml`). Com buckets explicitos
  o `awsemf` só publica Count/Sum/Min/Max. O `check-config.sh` do coletor prova o formato aceito.
- Nunca aplicado: a prova real (subir, transferir, ver traces e painel) fica no #182.
