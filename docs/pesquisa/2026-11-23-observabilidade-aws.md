# Pesquisa: observabilidade na AWS (X-Ray, CloudWatch e coletor ADOT)

Data: 2026-11-23. Etapa 10, Task 6. Só fontes primárias (documentação da AWS, README dos exportadores do
OpenTelemetry Collector, lista de preços da AWS). Região do ambiente: **sa-east-1** (não us-east-1: a variável `region` do
`envs/aws`); os preços abaixo são os de sa-east-1.

## Resumo
| Pergunta | Resposta |
|---|---|
| O X-Ray aceita IDs W3C do OpenTelemetry? | **Sim.** Pelo endpoint OTLP guarda o ID como veio; pelo exportador `awsxray` o coletor converte sozinho. Os serviços e o BFF **não** precisam do gerador de IDs da AWS: as Tasks 2 e 5 seguem como estão. |
| Exportador de traces | **OTLP/HTTP direto no endpoint do X-Ray** (`otlp_http` + `sigv4auth`), não o `awsxray`. |
| Exportador de métricas | `awsemf` (registro EMF no CloudWatch Logs). Limite: sem p50/p95 (ver abaixo). |
| Exportador de logs | `awscloudwatchlogs`, grupo `/capzzpay/{ServiceName}`. |
| Achar um `transfer.id` | Transaction Search (todo atributo de span é pesquisável), sem `indexed_attributes`. |
| ADOT: add-on ou chart? | **Chart próprio** (`deploy/helm/otel-collector`). |

## 1. IDs W3C no X-Ray
- "X-Ray now supports trace IDs that are created using OpenTelemetry and any other framework that conforms with the W3C Trace
  Context specification." O ID W3C `4efaaf4d1e8720b39541901950019ee5` vira `1-4efaaf4d-1e8720b39541901950019ee5` ao enviar;
  o carimbo de tempo do formato X-Ray "isn't required when sending W3C trace IDs" ([X-Ray, Sending trace data](https://docs.aws.amazon.com/xray/latest/devguide/xray-api-sendingdata.html);
  [anúncio de 27/10/2023](https://aws.amazon.com/about-aws/whats-new/2023/10/aws-x-ray-w3c-format-trace-ids-distributed-tracing): ADOT Collector 0.34.0 ou mais novo).
- Com o endpoint OTLP, os spans são guardados "in the semantic convention format with W3C trace IDs"
  ([Transaction Search](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-Transaction-Search.html)).
- O exportador `awsxray` do coletor converte "fully random trace IDs to X-Ray format" sozinho
  ([README do `awsxrayexporter`](https://github.com/open-telemetry/opentelemetry-collector-contrib/blob/main/exporter/awsxrayexporter/README.md)); a imagem ADOT 0.51.0 liga o gate `exporter.awsxray.skiptimestampvalidation`.
- Consequência: a propagação por `traceparent` (inclusive a nossa pelo SNS/SQS) e a amostragem de 100% valem na AWS como no local.
  A AWS pede "always_on" no SDK para ter 100% dos spans ([coletor OpenTelemetry no CloudWatch](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-OTLPSimplesetup.html)).

## 2. Exportador de traces: OTLP do X-Ray, não `awsxray`
- Endpoint `https://xray.<região>.amazonaws.com/v1/traces`, HTTP (sem gRPC), SigV4 com `service: xray` ou bearer token
  ([endpoints OTLP](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-OTLPEndpoint.html)). A ação autorizada é
  `xray:PutTraceSegments`; o bearer token existe em São Paulo, o que indica o endpoint na região
  ([X-Ray, bearer token](https://docs.aws.amazon.com/xray/latest/devguide/xray-opentelemetry-bearer-token.html)). Não consegui um texto
  que liste sa-east-1 como região do endpoint SigV4 em si: **prova no #182**.
- **Pré-requisito: Transaction Search ligado** ("Make sure Transaction Search is enabled before you use the OTLP Endpoint for traces",
  [Getting started](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-OTLPGettingStarted.html)). É uma configuração da conta
  inteira ("configured for the entire account"), com 1% dos spans indexado de graça
  ([Enable Transaction Search](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/Enable-TransactionSearch.html)). No Terraform:
  `aws_cloudwatch_log_resource_policy` (a política do doc) + `aws_xray_trace_segment_destination` (`destination = "CloudWatchLogs"`).
- Por que OTLP e não `awsxray`: guarda o ID W3C como veio, deixa **todo** atributo de span pesquisável (o `awsxray` só indexa o que estiver em
  `indexed_attributes`, como annotation, e o X-Ray indexa até 50 por trace) e é o caminho que a AWS documenta hoje.
  Contra: Transaction Search muda a conta toda e o destroy não o desfaz (a API não tem "desligar").
  Plano B se isso incomodar: `awsxray` com `indexed_attributes: [transfer.id, ...]`, sem Transaction Search.
- Limites do endpoint: 5 MB e 10.000 spans por requisição, span de até 200 KB, só gzip ou sem compressão (o chart usa gzip e `batch` de 512).

## 3. Métricas: `awsemf` e o limite do p50/p95
- O `awsemf` grava registros EMF no CloudWatch Logs (`/capzzpay/metrics`) e o CloudWatch os converte em métricas do namespace `capzzpay`;
  por isso o papel do coletor **não precisa de `cloudwatch:PutMetricData`** ([README do `awsemfexporter`](https://github.com/open-telemetry/opentelemetry-collector-contrib/blob/main/exporter/awsemfexporter/README.md)).
  `metric_declarations` escolhe os conjuntos de dimensões: só eles viram série (e custo).
- **Verificado com a imagem ADOT 0.51.0** (teste `deploy/helm/otel-collector/check-config.sh`, servidor da AWS falso): contador vira diferença entre
  leituras (a primeira é descartada), as dimensões saem como declaradas e o filtro derruba o que não é do painel.
- **Achado:** um histograma OTLP vira só `{Count, Sum, Min, Max}` no EMF, mesmo com `detailed_metrics: true`. O CloudWatch então mostra **média e máximo**
  da `transfers.saga.duration`, **não p50/p95** (o Grafana local calcula os quantis pelos buckets). Alternativa não avaliada: o endpoint OTLP de métricas do
  CloudWatch (`https://monitoring.<região>.amazonaws.com/v1/metrics`, PromQL no Query Studio), que preserva o histograma, mas não gera widget do painel em Terraform.
  A spec fala em p50/p95: decidir se média e máximo bastam na AWS.
- Atenção a dimensões com ponto: `service.name` aparece como dimensão do EMF (o painel usa `SEARCH` com o nome entre aspas).

## 4. Logs
- `awscloudwatchlogs` aceita placeholders no `log_group_name` (`{ServiceName}` vem do `service.name`) ([README](https://github.com/open-telemetry/opentelemetry-collector-contrib/blob/main/exporter/awscloudwatchlogsexporter/README.md));
  verificado: o log de `service.name=transfers` foi para `/capzzpay/transfers`.
- Os grupos são do Terraform (retenção de 1 dia). O coletor escreve só `CreateLogStream`, `PutLogEvents` e `DescribeLogStreams` nesses grupos.

## 5. Como pesquisar o `transfer.id`
Transaction Search: spans ficam no grupo `aws/spans`; "After these attributes have been added to the span, they become available to search and analyze in the Transaction Search
visual editor" ([atributos customizados](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch-Transaction-Search-add-custom-attributes.html)).
Nos logs, o mesmo ID aparece em `trace_id` (e o JSON do serviço traz `traceId`). O nome exato do campo no Logs Insights (`attributes.transfer.id`) não está
nos textos que li: **confirmar no #182** com uma transferência real. Depois de ligar o Transaction Search, os spans levam cerca de 10 minutos para ficarem pesquisáveis.

## 6. ADOT: add-on do EKS ou chart
O add-on `adot` "requires that the `cert-manager` add-on is deployed on the cluster" e traz o operador e CRDs
([add-ons do EKS](https://docs.aws.amazon.com/eks/latest/userguide/workloads-add-ons-available-eks.html)); o `amazon-cloudwatch-observability` instala o CloudWatch Agent
com Application Signals e Container Insights (métricas de nó e pod que ninguém pediu e custam por métrica). Para um coletor com uma configuração fixa, um chart de
um Deployment (imagem `public.ecr.aws/aws-observability/aws-otel-collector:v0.51.0`, fixa) é menor, validável sem cluster e segue o padrão do repositório. Credenciais pelo Pod Identity
(SDK da AWS na cadeia padrão; o `sigv4auth` as usa), sem anotação IRSA. Os nós saem para o ECR Público pelo NAT Gateway que já existe.

## 7. Custo por subida de 8 h (substitui a seção 7 da spec)
Preços de sa-east-1 da [lista de preços da AWS](https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonCloudWatch/current/sa-east-1/index.json) (publicada em 2026-11-19) e
do [X-Ray](https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AWSXRay/current/sa-east-1/index.json); franquias da [página de preços do CloudWatch](https://aws.amazon.com/cloudwatch/pricing/).
Estimativa para algumas centenas de transferências em 8 h, **sem contar as franquias** (pior caso):

| Item | Preço (sa-east-1) | Volume estimado | Custo |
|---|---|---|---|
| Spans (Transaction Search; item `Application-Signals-Bytes` da lista, que é o que a AWS cobra pelos spans: inferência minha) | US$ 0,35/GB (franquia: 100 GB) | ~40 MB (300 transferências x ~25 spans, mais HTTP) | US$ 0,014 |
| X-Ray, spans indexados | US$ 0,75 por milhão (1% grátis) | 1% de ~10 mil spans | US$ 0 |
| Métricas próprias (EMF) | US$ 0,30/métrica/mês (10 grátis), proporcional às horas | ~40 séries x 8/730 | US$ 0,13 |
| Registros EMF no CloudWatch Logs | US$ 0,90/GB ingerido | ~8 MB | US$ 0,007 |
| Logs dos serviços | US$ 0,90/GB ingerido | ~20 MB | US$ 0,018 |
| Armazenamento de logs (1 dia) | US$ 0,0408/GB-mês | < 0,1 GB | < US$ 0,01 |
| Painel | 3 grátis (50 métricas cada) | 1 | US$ 0 |
| Coletor ADOT | nós que já existem (requests 50m/128Mi) | 1 réplica | US$ 0 |
| NAT Gateway (dados) | já existe | < 0,2 GB | centavos |
| **Total** | | | **~US$ 0,2** (sem franquias; com elas, perto de US$ 0,1) |

Folga: mesmo com 10x o volume, fica abaixo de US$ 1 por subida. O grupo `aws/spans` não é do Terraform e fica na conta depois do destroy; guarda poucos MB.
Se as sondas de saúde (`/actuator/health`) gerarem spans (~17 mil requisições em 8 h), some ~US$ 0,01; vale a Task 2 excluí-las.
