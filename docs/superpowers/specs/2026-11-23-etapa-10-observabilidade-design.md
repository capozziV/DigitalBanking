# Etapa 10 — observabilidade: design

- **Data:** 2026-11-23
- **Status:** aprovada pelo usuário em 2026-11-23
- **Roadmap:** etapa 10, proposta pelo usuário em 2026-11-19 (cartão #202): correlation ID, linha do tempo da saga, painéis de logs, traces e métricas.
- **ADR base:** [0006 — Observabilidade só com correlation ID](../../adr/0006-observabilidade-com-correlation-id.md), que esta etapa substitui em parte por um ADR novo (seção 9).

## Objetivo e porquê
Mostrar, no portfólio, que dá para seguir uma transferência de ponta a ponta: do clique no app ao lançamento no extrato, passando pelos passos assíncronos da saga, pelo outbox e pelas filas. Hoje isso só se faz procurando o correlation ID em logs de texto espalhados por seis contêineres.

O usuário quer, no teste na própria máquina:
1. **Um painel com transferências por minuto.**
2. **Pegar o ID de uma transferência e ver a saga inteira, com os horários de cada passo.**

Roda **local agora**, mas nada pode ficar só local: o desenho da AWS sai pronto nesta etapa, com custo estimado, e sobe junto com a subida real (#182).

## Decisões do usuário (2026-11-23)
1. **Tracing distribuído com OpenTelemetry.** Revê o ADR 0006, que o tinha descartado.
2. **Local:** coletor OpenTelemetry + Grafana com Tempo (traces), Prometheus (métricas) e Loki (logs), no docker-compose.
3. **AWS:** X-Ray (traces) e CloudWatch (métricas, logs e painel), recebendo do mesmo coletor. A opção "o mesmo Grafana no EKS" e a opção Datadog foram descartadas (seção Abordagens).
4. **Só local nesta etapa.** A parte AWS é escrita e validada sem aplicar, como na etapa 8; sobe na #182.
5. Prioridade em **custo** e em **funcionar igual** nos dois ambientes.

## Critério de pronto
- Build completo verde (`./gradlew build`, com ktlint, unidade, integração, ArchUnit e e2e) e build, lint e testes do BFF verdes.
- Com a stack local de pé, uma transferência feita no app aparece:
  - no painel "Transferências" do Grafana, na contagem por minuto e na lista das últimas;
  - como **um trace só**, achado pelo ID da transferência, com os spans de cada passo da saga, do outbox/SNS/SQS e dos consumidores `notification` e `statement`, todos com horário e duração;
  - com os logs de cada span abertos a partir do trace (trace ID no log).
- Uma transferência recusada (`FAILED`) e uma compensada (`COMPENSATED`) também viram traces completos, com o passo que falhou marcado como erro.
- Teste automatizado prova a continuidade do trace pelo agendador da saga e pelo outbox até o consumidor (seção 8).
- Coletor ou backend de observabilidade fora **não** derruba nem atrasa transferência (teste).
- Terraform e Helm da parte AWS validados sem aplicar (`terraform validate`, `helm lint`/`template`, scripts de checagem existentes).
- Smoke, coleções Bruno e Playwright seguem verdes.
- Revisão de cada task e revisão final da branch pela Beatriz, com mutantes vermelhos.

## Restrições (de `CLAUDE.md` e ADRs)
- Código, testes, nomes de span, atributos, métricas e logs em inglês, com os termos do `CONTEXT.md` (`Transfer`, `TransferSaga`, `TransferStep`, `TransferStatus`, `LedgerEntry`, `Notification`, `StatementEntry`).
- O `domain` continua sem Spring nem OpenTelemetry (ArchUnit). Spans e métricas ficam em `adapters`/`bootstrap`, ou atrás de uma porta no `application`.
- Bibliotecas compartilhadas só de infraestrutura: a instrumentação comum vai para `libs/observabilidade` e `libs/outbox`/`libs/outbox-relay`.
- Eventos continuam só via Transactional Outbox; consumidores continuam idempotentes. O contexto do trace viaja junto, nunca no lugar do correlation ID.
- Sem dados pessoais em spans, atributos ou métricas: nada de nome, telefone, e-mail, token ou cookie. IDs técnicos (transferência, conta, evento) e valores simulados em centavos podem ir em atributos de span; **nunca** como rótulo de métrica (cardinalidade).
- Healthchecks e chamadas dentro de contêineres usam `127.0.0.1`.

## Abordagens consideradas

**Destino na AWS** (decidido pelo usuário):
- **A) CloudWatch + X-Ray (escolhida).** O coletor no EKS manda traces ao X-Ray, e métricas e logs ao CloudWatch. Custo de centavos por subida de 8 h, sem peso no cluster, nativo da AWS. Contra: o painel é refeito no CloudWatch, e passam a ser dois painéis para manter.
- **B) O mesmo Grafana dentro do EKS.** Painel idêntico ao local, mas ocupa ~1,5 GB dos 2 nós t3.medium, talvez peça um terceiro nó e põe na nossa conta a operação do Grafana.
- **C) Datadog.** O melhor produto, mas só é grátis no teste de 14 dias; depois fica em torno de US$ 50/mês por host com APM. Continua possível no futuro, trocando só o exportador do coletor.

**Instrumentação dos serviços Kotlin** (decisão minha, ajustável):
- **Micrometer Tracing com a ponte OpenTelemetry, pelo suporte nativo do Spring Boot 4 (escolhida).** Combina com as métricas Micrometer que já existem, não muda a imagem Docker e dá controle sobre os spans da saga.
- **Agente Java do OpenTelemetry.** Instrumenta JDBC, Redis e AWS SDK sem código, mas acrescenta um jar à imagem, gera muitos spans de baixo valor e não resolve sozinho a continuidade pelo agendador e pelo outbox, que é o coração da etapa.

**Continuidade da saga** (decisão minha):
- **Guardar o contexto W3C (`traceparent`) na transferência e no outbox, e retomar como filho (escolhida).** Gera um trace só, que é o que o usuário pediu ("a saga inteira").
- **Trace novo por retomada, com link para o original.** É o padrão para filas longas, mas quebra a saga em vários traces, e o X-Ray mostra links mal.

## 1. Sinais e nomes
- **Traces:** W3C Trace Context (`traceparent`) como propagação, nos dois ambientes.
- **Atributos do span** (comuns): `transfer.id`, `account.id` (quando houver), `event.id` e `event.type` (mensageria), `correlation.id`. Nos spans da saga, também `transfer.step` e `transfer.status`.
- **Span por passo da saga**, nomeado pelo `TransferStep` (`ReserveLimit`, `Debit`, `Credit`, consulta, `Compensation`...). O passo que termina em recusa ou erro marca o span com status de erro e o motivo (`daily-limit-exceeded`, saldo insuficiente, conta bloqueada).
- **Métricas novas** no `transfers`:
  - `transfers.finished` (contador), com o rótulo `status` (`COMPLETED`, `FAILED`, `COMPENSATED`) e `reason` só nas recusas (conjunto fechado). Dá o "transferências por minuto";
  - `transfers.saga.duration` (timer, do pedido ao status final), com o rótulo `status`.
- As métricas que já existem (`outbox.*`, `statement.cache.*`, DLQs, circuito do limite diário) passam a ser exportadas e entram no painel.
- **Logs:** JSON estruturado no stdout, com `traceId`, `spanId` e `correlationId`. Corrige a divergência atual: o ADR 0006 diz "logs JSON", mas os serviços gravam texto.

## 2. Serviços Kotlin (`accounts`, `transfers`, `notification`, `statement`, `customer`)
- Tracing e exportação OTLP (traces, métricas e logs) para o coletor, configurados uma vez em `libs/observabilidade` (autoconfiguração), com endpoint por variável de ambiente (`OTEL_EXPORTER_OTLP_ENDPOINT` ou equivalente do Spring).
- Spans automáticos de servidor e cliente HTTP (`RestClient`), já propagando `traceparent` junto do `X-Correlation-Id`.
- O correlation ID continua como está (filtro, MDC, outbox, SNS/SQS) e vira também atributo do span.
- Amostragem de **100%**, local e na AWS: o volume é de demonstração (seção 7).
- **Coletor fora não afeta o serviço:** exportação assíncrona em lote, com fila limitada e descarte. O teste da seção 8 prova isso.
- O `/actuator` continua restrito a admin. A exportação de métricas é *push* (OTLP) e não abre rota nova.

## 3. Continuidade pelo agendador e pelo outbox
- **Transferência:** coluna nova `trace_parent text` em `transfer` (migration no `transfers`), gravada no pedido a partir do span corrente. Quando o agendador retoma a transferência, o span do passo nasce como filho desse contexto. É assim que o trace fica um só, mesmo com retomadas segundos ou minutos depois.
- **Outbox:** coluna nova `trace_parent text` em `outbox` (no modelo `libs/outbox/.../outbox-table.sql` e numa migration em cada serviço que tem outbox: `accounts`, `transfers` e `customer`), preenchida no `append` pelo span corrente, como o `correlation_id`.
- **Relay:** o span de publicação no SNS é filho do `trace_parent` do evento, e o `traceparent` vai num atributo de mensagem SNS, ao lado do correlation ID (raw message delivery já entrega isso no SQS).
- **Consumidores SQS** (`notification`, `statement` e os demais que consomem): o interceptor de `libs/observabilidade`, que já põe o correlation ID no MDC, abre o span de consumo como filho do `traceparent` da mensagem. Mensagem sem o atributo (eventos antigos, mensagens já nas filas) gera um trace novo, sem erro.
- Linhas antigas sem `trace_parent` continuam funcionando (coluna anulável).

## 4. BFF (NestJS)
- SDK OpenTelemetry para Node, com instrumentação de HTTP de entrada e saída e ioredis, exportando OTLP ao mesmo coletor.
- O BFF é a raiz do trace, como já é a origem do correlation ID. O `traceparent` sai nas chamadas aos serviços.
- O pino passa a incluir `traceId` e `spanId` nos logs. Os logs do Nest fora do pino e a falta de correlation ID neles fazem parte do débito #236 e não entram aqui, salvo se o mesmo ajuste resolver de graça.
- Coletor fora não afeta o BFF.

## 5. Ambiente local (docker-compose)
- Contêineres novos em `infra/local/docker-compose.yml`, num **profile `observability`**, ligado por padrão junto com `apps` no guia e no `up` do usuário:
  - `otel-collector`: recebe OTLP (gRPC 4317, HTTP 4318) e repassa ao Tempo, ao Prometheus e ao Loki;
  - `tempo`, `prometheus` (com o receptor OTLP ou o exportador do coletor; escolha do builder), `loki`;
  - `grafana` em `http://127.0.0.1:3000`, com acesso anônimo de leitura só localmente, e fontes de dados e painéis provisionados como código em `infra/local/grafana/`.
- Volumes próprios para os dados de observabilidade, separados dos volumes do usuário. Apagar esses dados não perde nada do banco.
- **Painel "Transferências"** (provisionado):
  - transferências por minuto, por status;
  - duração da saga (p50 e p95);
  - recusas por motivo;
  - DLQs e outbox pendente;
  - tabela das últimas transferências (das traces no Tempo), com um link "ver saga" que abre o trace.
- **Busca por ID:** no Explore do Tempo, por `{ span.transfer.id = "<id>" }`; o painel traz um campo para colar o ID.
- **Logs a partir do trace:** a fonte do Tempo liga cada span aos logs do Loki pelo `traceId`.
- Os serviços da stack recebem o endpoint do coletor por variável de ambiente no compose.
- **Stack do usuário (`capzzpay`):** a migração segue a regra do status, com backup em `.superpowers/backups/` e recriando só os serviços alterados com `--no-deps`, sem tocar em volumes nem no LocalStack. As duas migrations novas (`trace_parent`) rodam pelo Flyway na subida.
- Guia `docs/rodando-localmente.md` atualizado: como abrir o Grafana, achar uma transferência e ler a linha do tempo.

## 6. AWS (desenhado e validado, sem aplicar)
- **Coletor:** AWS Distro for OpenTelemetry (ADOT) Collector no EKS, um `Deployment` com `Service` interno (chart próprio em `deploy/helm/` ou o add-on gerenciado do EKS; escolha do builder com o custo em vista). Os serviços recebem o mesmo endpoint OTLP por `values`.
- **Exportadores do coletor:**
  - traces para o X-Ray (exportador `awsxray` ou o endpoint OTLP do X-Ray; escolha do builder conforme a documentação atual);
  - métricas para o CloudWatch por EMF (`awsemf`), só as métricas listadas na seção 1 e as que já existem, filtradas no coletor para não pagar por métrica que ninguém olha;
  - logs para o CloudWatch Logs, um grupo por serviço, com **retenção de 1 dia** (o ambiente vive 8 h).
- **Permissões:** Pod Identity do coletor com `xray:PutTraceSegments`/`PutTelemetryRecords`, `cloudwatch:PutMetricData` e `logs:*` restrito aos grupos do projeto (módulo `access`).
- **Painel CloudWatch "Transferências"** em Terraform, espelhando o local: por minuto e por status, duração p50/p95, recusas, DLQs e outbox.
- **Busca por ID no X-Ray:** `transfer.id` como *annotation* indexada (no X-Ray, só annotation é pesquisável) ou pela busca de transações, conforme o exportador escolhido.
- Tudo sobe e desce com `aws-up`/`aws-down` e entra no `aws-reaper`: nada de observabilidade sobrevive ao ambiente, exceto o que o `bootstrap` já mantém.
- Fica no #182 a prova real: subir, fazer transferências e ver traces e painel no console.

## 7. Custo estimado na AWS
Preços de tabela a conferir na página oficial durante o plano (task de pesquisa), em `us-east-1`. Volume de uma demonstração: algumas centenas de transferências em 8 h.

| Item | Grátis por mês | Depois | Estimativa por subida |
|---|---|---|---|
| X-Ray, traces gravados | 100 mil | ~US$ 5 por milhão | US$ 0 |
| CloudWatch Logs, ingestão | 5 GB | ~US$ 0,50/GB | US$ 0 (retenção de 1 dia) |
| Métricas próprias (EMF) | 10 | ~US$ 0,30/métrica/mês, proporcional às horas | centavos |
| Painel | 3 | US$ 3/painel/mês | US$ 0 (um painel) |
| Coletor ADOT | — | roda nos nós que já existem (~200 MB) | US$ 0 |

Orçamento atual: US$ 20/mês, com alertas em 50%, 80% e 100%. A etapa não pode empurrar uma subida comum para perto de US$ 1 a mais. A task de custo confirma isso com os preços reais.

## 8. Testes
- **Unidade/integração (`transfers`):** com um exportador em memória, uma transferência que passa pelo agendador gera um trace só, com um span por passo, e o `trace_parent` gravado. Mutante: sem gravar ou sem retomar o contexto, o teste fica vermelho.
- **Outbox/relay:** o `trace_parent` vai ao `outbox` e ao atributo SNS. O consumidor SQS continua o trace e, sem o atributo, cria um trace novo sem erro.
- **Métricas:** `transfers.finished` e `transfers.saga.duration` registradas com os rótulos certos nos três status.
- **Resiliência:** com o endpoint OTLP apontando para o nada, a transferência conclui no mesmo tempo (margem de teste) e sem erro nos logs a cada requisição.
- **ArchUnit:** o `domain` não depende de OpenTelemetry nem de Micrometer.
- **BFF:** a requisição de transferência sai com `traceparent`; o log tem `traceId`.
- **e2e:** com o compose de observabilidade, uma transferência é achada no Tempo (API de busca) pelo `transfer.id`, com spans de `bff`, `transfers`, `accounts`, `notification` e `statement`.
- **Smoke:** Grafana, Tempo, Prometheus e Loki prontos; a métrica `transfers.finished` aparece no Prometheus depois da transferência do smoke.
- **Infra:** `terraform validate` e os scripts de checagem de `envs/aws`, e `helm lint`/`template` com os valores novos.

## 9. Documentação
- **ADR 0028**, "Tracing distribuído com OpenTelemetry", que substitui o 0006 na parte do tracing (o correlation ID continua). O 0006 passa a "substituído em parte por 0028".
- `docs/arquitetura.md`: seção Observabilidade reescrita (stack local, AWS, continuidade pelo outbox e pela saga).
- `docs/roadmap.md`: linha da etapa 10.
- `libs/observabilidade/README.md` e `docs/rodando-localmente.md` atualizados.
- `CONTEXT.md` não muda: trace e span são termos técnicos, não do domínio.

## 10. Pontos fechados na revisão do builder (decisões da coordenadora, ajustáveis pelo usuário)
1. **Onde fica o `transfer.id`:** no span de servidor do `POST /transfers` do `transfers`, em todos os spans da saga e nos spans de consumo de eventos da transferência. O BFF põe o atributo no próprio span quando recebe a resposta, antes de fechá-lo. O Tempo acha o trace por atributo de qualquer span, sem índice extra. O trace ID **não** é derivado do ID da transferência: os eventos do `accounts` e do `customer` também precisam de contexto, e o `trace_parent` resolve os dois casos do mesmo jeito.
2. **Duração da saga:** o prazo é de 2 min (`transfers.saga.deadline`), com retentativas de até 15 s. Um trace de alguns minutos cabe nas janelas do Tempo e do X-Ray, então o "filho do original" basta e não há links.
3. **Um trace só também na AWS:** a propagação pelo SNS/SQS é nossa (atributo de mensagem lido pelo interceptor), sem depender do `AWSTraceHeader`. O que precisa valer é o X-Ray aceitar os IDs W3C do OpenTelemetry. A task de pesquisa confirma isso na documentação atual (endpoint OTLP do X-Ray ou busca de transações). Se não aceitar, os serviços e o BFF passam a gerar IDs no formato X-Ray (gerador de IDs da AWS), que também valem no Tempo. Critério: o mesmo trace inteiro nos dois ambientes.
4. **Migrations do `trace_parent`:** a coluna é anulável e cada serviço roda uma instância só, localmente e no EKS. O relay lê a coluna só se ela existir com valor. Não há janela de compatibilidade a testar além de "linha sem `trace_parent` funciona", que já está na seção 3.
5. **CI:** o e2e existente sobe só o `otel-collector` e o `tempo` (leves) e confere o trace pela API do Tempo. O Grafana, o Prometheus e o Loki entram no smoke local. Não cria job novo no `ci-ok`. Aceita-se até ~2 min a mais no job de e2e.
6. **Memória no Mac:** o profile `observability` é separado do `apps`. O guia e o `up` do usuário o incluem, mas dá para subir sem ele.
7. **Nomes das métricas:** no código, os nomes Micrometer (`transfers.finished`, `transfers.saga.duration`). No Prometheus e no painel local, os nomes convertidos (`transfers_finished_total`; o timer vira histograma, com p50/p95 por `histogram_quantile`). No CloudWatch, o nome que o `awsemf` gerar, registrado no Terraform do painel. O smoke usa o nome do Prometheus.
8. **Logs JSON:** todos os serviços mudam juntos, na mesma task. Antes, a task procura quem lê o formato atual (smoke, e2e, guia, scripts de `infra/local`) e ajusta o que depender dele.

## Fora do escopo
- Alarmes, SLOs, *profiling* e alertas por e-mail.
- Aplicar a parte AWS (fica na #182).
- Datadog. A troca futura é só de exportador no coletor.
- Painéis de negócio além de "Transferências" (por exemplo, contas abertas por dia).
- Débitos do #236 sobre logs do BFF, além do que esta etapa resolver de passagem.

## Anti-objetivos
- Nada que só funcione localmente: todo sinal local tem destino definido na AWS.
- Observabilidade derrubando ou atrasando transferência.
- Dado pessoal em trace, log ou métrica.
- Saga quebrada em vários traces, que é exatamente o que o usuário quer ver inteiro.
- Custo que ameace o orçamento de US$ 20/mês.

## Fica para o builder
- O mecanismo exato do Spring Boot 4.1 para exportar traces, métricas e logs por OTLP (starter e propriedades), conferido na documentação oficial.
- Prometheus recebendo OTLP direto ou pelo exportador do coletor.
- Exportador do X-Ray (`awsxray` ou OTLP) e o tipo de busca por `transfer.id`.
- Coletor ADOT por chart próprio ou pelo add-on do EKS.
- Nomes dos spans de mensageria, seguindo as convenções semânticas do OpenTelemetry.
