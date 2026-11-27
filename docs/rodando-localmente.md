# Rodando o capzzPay localmente (macOS)

Guia para subir a infraestrutura local, os serviços `customer`, `accounts`, `transfers`, `notification` e `statement` e conferir as APIs pelo nginx.
Todos os comandos rodam na raiz do repositório e valem tanto para o zsh (o shell padrão do macOS) quanto para o bash. Os blocos de comando não têm comentários: as explicações ficam no texto antes e depois de cada bloco, porque o zsh do macOS, por padrão, não aceita `# comentário` na linha de comando.

## Pré-requisitos
Os comandos `brew` abaixo valem só para Mac com chip Apple (M1 em diante): o Homebrew não instala em Mac Intel. Confira o processador com `uname -m` (`arm64` é Apple, `x86_64` é Intel). Para cada ferramenta há também o caminho sem Homebrew.

- **JDK 21 instalado.** O Gradle não baixa JDK sozinho (`org.gradle.java.installations.auto-download=false` em `gradle.properties`). Com Homebrew:
  ```bash
  brew install --cask temurin@21
  ```
  Sem Homebrew: baixe o instalador `.pkg` do Temurin 21 para macOS (x64 no Intel, aarch64 no Apple) em [adoptium.net](https://adoptium.net/temurin/releases/?version=21&os=mac) e dê dois cliques. Depois, nos dois casos, confira:
  ```bash
  export JAVA_HOME=$(/usr/libexec/java_home -v 21)
  java -version
  ```
  O `java -version` deve mostrar 21. O `export` vale só para o terminal em que foi digitado. Para não repetir em cada terminal novo, acrescente a linha ao `~/.zshrc`:
  ```bash
  echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc
  ```
  Ou repita o `export` em cada terminal novo antes de rodar o Gradle, como no passo 4.
- **Docker Desktop em execução.** O compose e os testes de integração com Testcontainers precisam dele. Confira com `docker info`.
- **Porta 80 no Docker Desktop.** O nginx publica a porta 80. Se o `up` falhar ao publicar essa porta, ative "Allow privileged port mapping" em Settings → Advanced do Docker Desktop, ou troque `NGINX_PORT` no `infra/local/.env` (passo 1).
- **jq** para o smoke. A partir do macOS 15 costuma vir em `/usr/bin/jq`; confira com `jq --version`. Se faltar: `brew install jq`, ou as outras formas de instalação em [jqlang.org/download](https://jqlang.org/download/).
- **Python 3** só para migrar o extrato de uma stack antiga ([procedimento](#migrar-o-extrato-de-uma-stack-anterior-ao-dynamodb-local)). Confira com `python3 --version` (3.9 ou mais novo; vem com as ferramentas de linha de comando do Xcode).
- **Token do LocalStack.** O LocalStack emula o SNS e o SQS e exige um token, mesmo no plano gratuito. Crie uma conta Hobby em [app.localstack.cloud](https://app.localstack.cloud), copie o seu Auth Token no painel da conta e preencha `LOCALSTACK_AUTH_TOKEN` no `infra/local/.env` (passo 1). Sem o token, o contêiner do LocalStack encerra na partida e os testes com LocalStack são pulados. Nunca coloque o token no `.env.example` nem em outro arquivo versionado; o `infra/local/.env` está no `.gitignore`.
- **Opcional:** Node (para a CLI do Bruno) e uv (para o graphify). Com Homebrew:
  ```bash
  brew install node uv
  npm install -g @usebruno/cli
  ```
  Sem Homebrew, tudo na pasta pessoal e sem senha de administrador. Primeiro, uma única vez, ponha `~/.local/bin` e `~/.local/node/bin` no `PATH` (rodar de novo duplica a linha no `~/.zshrc`):
  ```bash
  echo 'export PATH="$HOME/.local/bin:$HOME/.local/node/bin:$PATH"' >> ~/.zshrc
  source ~/.zshrc
  ```
  Node: baixe o `.tar.gz` do Node LTS para macOS (`darwin-x64` no Intel, `darwin-arm64` no Apple) em [nodejs.org/dist](https://nodejs.org/dist/) e extraia em `~/.local/node`. O exemplo usa o Node v24.22.1 num Mac Intel; troque a versão e a arquitetura se precisar. Os dois hashes mostrados pelo `shasum` e pelo `grep` precisam ser iguais:
  ```bash
  mkdir -p ~/.local/node
  curl -fsSL -o /tmp/node.tar.gz https://nodejs.org/dist/v24.22.1/node-v24.22.1-darwin-x64.tar.gz
  shasum -a 256 /tmp/node.tar.gz
  curl -fsSL https://nodejs.org/dist/v24.22.1/SHASUMS256.txt | grep node-v24.22.1-darwin-x64.tar.gz
  tar -xzf /tmp/node.tar.gz -C ~/.local/node --strip-components=1
  npm install -g @usebruno/cli
  ```
  uv: o instalador oficial grava em `~/.local/bin`; abra um terminal novo depois dele:
  ```bash
  curl -LsSf https://astral.sh/uv/install.sh | sh
  ```

## 1. Configurar o ambiente
```bash
cp infra/local/.env.example infra/local/.env
```
**Já tem uma stack com dados, criada antes do DynamoDB Local?** Não rode `up`, `down`, `stop` nem `restart` nela antes de [migrar o extrato](#migrar-o-extrato-de-uma-stack-anterior-ao-dynamodb-local): o extrato dela mora no LocalStack e some quando ele para ou é recriado.

Preencha `LOCALSTACK_AUTH_TOKEN` no `infra/local/.env`. Os outros valores do `.env.example` servem para desenvolvimento. Se a porta 80 ou 5432 já estiver em uso, troque `NGINX_PORT` ou `POSTGRES_PORT` no `infra/local/.env`. Ao trocar `POSTGRES_PORT`, ajuste a porta em `ACCOUNTS_DB_URL`, `TRANSFERS_DB_URL`, `NOTIFICATION_DB_URL` e `CUSTOMER_DB_URL` no mesmo arquivo. Ao trocar `DYNAMODB_PORT` (padrão 8000, publicada só em `127.0.0.1`), ajuste a porta em `STATEMENT_DYNAMODB_ENDPOINT`. Um `.env` criado antes do DynamoDB Local precisa ganhar essas duas linhas: sem `STATEMENT_DYNAMODB_ENDPOINT`, o `statement` em `bootRun` procura o DynamoDB real da AWS. Ao trocar `NGINX_PORT`, use a porta nova nas URLs abaixo (`http://localhost:<porta>`) e no `baseUrl` de `local.bru` de cada coleção em `api/bruno/*/environments/`.

## 2. Subir a infraestrutura (PostgreSQL, Redis, nginx, LocalStack, DynamoDB Local, mock-oauth2-server, web, BFF)
```bash
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml up --wait
```
O comando constrói as imagens `web` e `bff`, que não têm perfil, e volta quando os oito contêineres estão saudáveis (um nono, o `dynamodb-volume`, só entrega o volume ao usuário do DynamoDB Local e sai com código 0: ficar `Exited (0)` é o esperado). Sem o token, o `up --wait` volta com erro porque o LocalStack encerra, mas PostgreSQL, Redis, DynamoDB Local e nginx ficam de pé. O LocalStack só fica saudável depois que os scripts de `infra/local/localstack/init/ready.d/` criam os tópicos `accounts-events.fifo` e `transfers-events.fifo`, cada um com sua fila de inspeção assinada (`accounts-events-inspection.fifo` e `transfers-events-inspection.fifo`), a fila `statement-accounts.fifo` (assinada em `accounts-events.fifo`, com a DLQ `statement-accounts-dlq.fifo`) e as filas do `notification`: `notification-accounts.fifo` (assinada em `accounts-events.fifo`), `notification-transfers.fifo` (assinada em `transfers-events.fifo`) e `notification-customer.fifo` (assinada em `customer-events.fifo`, [ADR 0025](adr/0025-nome-da-contraparte-nas-notificacoes.md)), cada uma com sua DLQ FIFO (`notification-accounts-dlq.fifo`, `notification-transfers-dlq.fifo` e `notification-customer-dlq.fifo`). A fila de origem move a mensagem para a DLQ na 5ª recepção (`NOTIFICATION_MAX_RECEIVE_COUNT`) e esconde a mensagem em leitura por 30 s (`NOTIFICATION_VISIBILITY_TIMEOUT_SECONDS`); os dois valores podem ser trocados no `infra/local/.env`. O script de init do PostgreSQL (`infra/local/postgres/init/01-create-service-databases.sh`) cria um banco por serviço, cada um com dois roles: `<serviço>_owner` (dono, usado pelo Flyway) e `<serviço>_app` (só `LOGIN`; as permissões vêm das migrations). Hoje são `accounts`, `transfers`, `notification` e `customer`. Os dados ficam no volume nomeado `postgres-data`, que sobrevive a `down` e `up`; o script só roda com volume novo, então, se mudar senhas ou o script, recrie com `down -v` (passo 8). O script é idempotente: para rodá-lo de novo sem apagar nada, execute `docker compose exec postgres /docker-entrypoint-initdb.d/01-create-service-databases.sh`. Ele cria só o que falta e não troca a senha de um role que já existe.

O DynamoDB Local (serviço `dynamodb`) guarda a tabela `statement` no volume nomeado `dynamodb-data`, que também sobrevive a `down` e `up`; só o `down -v` a apaga. Ele fica saudável depois de criar a tabela, se ela não existir (`infra/local/dynamodb/init/run.sh`, com a definição em `statement-table.json` na mesma pasta), e o contêiner do `statement` espera por isso. O LocalStack segue sem persistência, só com SNS e SQS: ao parar ou recriar, perde as mensagens em trânsito e as das DLQs. Um evento perdido assim não volta ao extrato sozinho; antes de derrubar, espere as filas esvaziarem.

Para adicionar o banco de um serviço novo: acrescente ao fim do script a linha `create_service_db <serviço> <SERVICO>_DB_OWNER_PASSWORD <SERVICO>_DB_PASSWORD` (os dois últimos argumentos são os nomes das variáveis com as senhas), declare essas duas variáveis no `environment` do `postgres` em `docker-compose.yml` e em `infra/local/.env.example`, e recrie o volume com `down -v`.

## 3. Build e testes
O build completo compila, roda o ktlint, os testes unitários, o ArchUnit e os testes de integração, que precisam do Docker. Os testes com LocalStack leem o token do ambiente, então carregue o `.env` antes:
```bash
set -a; . infra/local/.env; set +a
./gradlew build
```
Para rodar só os testes unitários, sem Docker:
```bash
./gradlew test
```

## 4. Subir os serviços `customer`, `accounts`, `transfers`, `notification` e `statement`
Todos validam o JWT do `mock-oauth2-server` (`*_JWT_ISSUER_URI`; veja [Identidade local](#identidade-local-mock-oauth2-server)). O `customer` sobe primeiro: o `accounts` o consulta a cada abertura de conta e o `transfers` a cada pedido de transferência, direto e com token de máquina (`ACCOUNTS_CUSTOMER_BASE_URL` e `TRANSFERS_CUSTOMER_BASE_URL`, padrão `http://localhost:8085`; tempo máximo `ACCOUNTS_CUSTOMER_TIMEOUT` e `TRANSFERS_CUSTOMER_TIMEOUT`, padrão `500ms`). Com o `customer` fora, essas duas operações respondem `503 customers-unavailable`.

O `accounts` também consulta o `transfers` ao encerrar uma conta (`POST /accounts/{id}/close`), direto e com token de máquina, para saber se há transferências pendentes: `ACCOUNTS_TRANSFERS_BASE_URL` (padrão `http://localhost:8082`) e `ACCOUNTS_TRANSFERS_TIMEOUT` (padrão `500ms`). Com o `transfers` fora ou lento, só o encerramento responde `503 transfers-unavailable`; o resto do `accounts` segue normal. Bloquear e desbloquear (`PUT /admin/accounts/{id}/status`, só admin) não dependem do `transfers`.

Localmente os serviços sobem com o perfil padrão (LocalStack, swagger-ui ligado). O perfil `aws` é só do deploy na AWS; veja [arquitetura](arquitetura.md#borda-e-segurança).

O `customer` lê `CUSTOMER_DB_*`, `CUSTOMER_AWS_ENDPOINT` (LocalStack) e `CUSTOMER_JWT_ISSUER_URI`, e publica `CustomerRegistered` (v2, com o telefone), `CustomerPhoneChanged`, `CustomerBlocked` e `CustomerUnblocked` no tópico `customer-events.fifo` pelo outbox. Num terminal novo:
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
set -a; . infra/local/.env; set +a
./gradlew :services:customer:bootstrap:bootRun
```
O `customer` sobe na porta 8085 e aplica as próprias migrations. O nginx expõe `/customers` (`POST /customers` e `GET /customers/me`), `/admin/customers/**` e `/customers/actuator/`; o `/internal/customers/{id}` fica fora da borda, só para os serviços.

O `accounts` lê as variáveis `ACCOUNTS_DB_*`, `ACCOUNTS_AWS_ENDPOINT`, `ACCOUNTS_JWT_ISSUER_URI`, `ACCOUNTS_OAUTH_*` (token de máquina) e `ACCOUNTS_CUSTOMER_*` do ambiente. Num segundo terminal, carregue o `.env` no shell (e o `JAVA_HOME`, se não estiver no `~/.zshrc`) e rode:
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
set -a; . infra/local/.env; set +a
./gradlew :services:accounts:bootstrap:bootRun
```
O serviço sobe na porta 8081 e aplica as migrations (Flyway) ao iniciar. A cada 500 ms o relay publica os eventos pendentes do outbox no tópico SNS do LocalStack. Deixe esse terminal aberto.

O `transfers` precisa do `accounts` e do `customer` no ar e roda num terceiro terminal, com o mesmo `.env` carregado. Ele lê `TRANSFERS_DB_*`, `TRANSFERS_ACCOUNTS_URL` (padrão `http://localhost:8081`, o `accounts` chamado direto, sem o nginx), `TRANSFERS_JWT_ISSUER_URI`, `TRANSFERS_OAUTH_*` (token de máquina para o `accounts` e o `customer`), `TRANSFERS_CUSTOMER_*`, `TRANSFERS_AWS_*` (LocalStack) e, para o limite diário, `TRANSFERS_REDIS_HOST` (padrão `localhost`), `TRANSFERS_REDIS_PORT` (padrão `6379`), `TRANSFERS_REDIS_TIMEOUT` (padrão `100ms`) e `TRANSFERS_DAILY_LIMIT` (padrão `500000` centavos). As métricas `transfers.pending` e `transfers.oldest.pending.age` ficam em `/transfers/actuator/metrics`:
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
set -a; . infra/local/.env; set +a
./gradlew :services:transfers:bootstrap:bootRun
```
O `transfers` sobe na porta 8082 e aplica as próprias migrations. O nginx expõe `GET /accounts/{id}/daily-limit` (limite de 500000 centavos por conta e por dia de São Paulo, o usado e o disponível; conta desconhecida volta com uso zero) pelo `transfers`, com uma regra mais específica que a do prefixo `/accounts/`, que é do `accounts`. A transferência que passaria do limite termina `FAILED` com o motivo `daily-limit-exceeded` (`422 transfer-failed`); sem resposta do Redis nem do PostgreSQL, com `daily-limit-unavailable`. As métricas do limite saem em `/transfers/actuator/metrics`: `transfers.daily_limit.reservations` (tag `result` = `reserved`, `exceeded`, `fallback` ou `error`; `fallback` é a reserva decidida no PostgreSQL com o Redis fora) e `transfers.daily_limit.rebuilds` (vezes em que o contador do Redis foi reconstruído a partir do PostgreSQL). O gauge `transfers.daily_limit.circuit.state` mostra o circuit breaker do Redis: 0 = CLOSED, 1 = OPEN (reservas direto no PostgreSQL), 2 = HALF_OPEN (uma sonda em curso). Propriedades `transfers.daily-limit.*`: `lock-timeout` (espera máxima pela trava da conta e do dia; padrão `2s`; variável `TRANSFERS_DAILY_LIMIT_LOCK_TIMEOUT`), `circuit-breaker.failure-threshold` (falhas seguidas que abrem o circuito; padrão `5`; `TRANSFERS_DAILY_LIMIT_CIRCUIT_BREAKER_FAILURE_THRESHOLD`) e `circuit-breaker.open-duration` (tempo aberto antes da sonda; padrão `10s`; `TRANSFERS_DAILY_LIMIT_CIRCUIT_BREAKER_OPEN_DURATION`).

O `notification` consome os eventos dos dois serviços e roda num quarto terminal, com o mesmo `.env` carregado. Ele lê as variáveis `NOTIFICATION_*` (banco e LocalStack) e consome as filas `notification-accounts.fifo`, `notification-transfers.fifo` e `notification-customer.fifo` ([ADR 0025](adr/0025-nome-da-contraparte-nas-notificacoes.md)):
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
set -a; . infra/local/.env; set +a
./gradlew :services:notification:bootstrap:bootRun
```
O `notification` sobe na porta 8083 e aplica as próprias migrations; o nginx o expõe em `/notifications`.

O `statement` consome os eventos do `accounts` e roda num quinto terminal, com o mesmo `.env` carregado. Não usa PostgreSQL: consome do SQS do LocalStack e grava no DynamoDB Local. Ele lê `STATEMENT_AWS_ENDPOINT` (o do SQS; no `.env.example`, `http://localhost:4566`), `STATEMENT_DYNAMODB_ENDPOINT` (o do DynamoDB; no `.env.example`, `http://127.0.0.1:8000`), cada um usando o endpoint real da AWS daquele serviço quando vazio, e, se definidas, `STATEMENT_AWS_REGION` (padrão `us-east-1`), `STATEMENT_TABLE` (padrão `statement`), `STATEMENT_ACCOUNTS_QUEUE` (padrão `statement-accounts.fifo`), `STATEMENT_ACCOUNTS_DLQ` (padrão `statement-accounts-dlq.fifo`), `STATEMENT_DLQ_METRICS_REFRESH` (padrão `30s`), `STATEMENT_REDIS_HOST` (padrão `localhost`), `STATEMENT_REDIS_PORT` (padrão `6379`), `STATEMENT_CACHE_TIMEOUT` (padrão `100ms`, tempo de conexão e de comando ao Redis) e `STATEMENT_CACHE_ENABLED` (padrão `true`; `false` desliga o cache e o extrato sempre lê do DynamoDB):
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
set -a; . infra/local/.env; set +a
./gradlew :services:statement:bootstrap:bootRun
```
O `statement` sobe na porta 8084. O `statement` guarda no Redis do compose a primeira página do extrato (20 lançamentos, sem cursor) por cerca de 60 s; com o Redis fora, ele lê do DynamoDB. A tabela DynamoDB `statement` (chave `accountId` texto + `sequence` numérica, cobrança `PAY_PER_REQUEST`) é criada pelo serviço `dynamodb` do compose e as filas pelo `infra/local/localstack/init/ready.d/04-statement.sh`, não pelo `statement`. O nginx expõe `GET /accounts/{id}/statement` e `/statement/actuator/`. Use um sexto terminal para os passos seguintes.

### Alternativa: os cinco serviços pelas imagens Docker (perfil `apps`)
Em vez dos cinco `bootRun`, o perfil `apps` do compose sobe os serviços como contêineres, pelas imagens de `services/<serviço>/Dockerfile` (as mesmas do CI). A imagem não compila: ela usa o `bootJar` já gerado pelo Gradle, separado em camadas (dependências, carregador do Spring Boot, dependências snapshot e classes do serviço) e roda com usuário não root (uid 10001). Gere os jars e suba tudo (infra e serviços) de uma vez:
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./gradlew :services:customer:bootstrap:bootJar :services:accounts:bootstrap:bootJar \
  :services:transfers:bootstrap:bootJar :services:notification:bootstrap:bootJar :services:statement:bootstrap:bootJar
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml --profile apps build
export MOCK_OAUTH2_HOST=mock-oauth2-server:8080
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml --profile apps up --wait
```
As imagens se chamam `digitalbanking/<serviço>:<etiqueta>`, com a etiqueta de `IMAGE_TAG` (padrão `local`; no CI, o SHA do commit). Depois de mudar o código, gere o jar de novo e rode o `build` outra vez. Cada contêiner fica `healthy` quando o `/actuator/health` responde em `127.0.0.1` dentro dele; o `up --wait` espera os cinco. As portas 8081–8085 continuam publicadas no host, então o nginx não muda; não rode os `bootRun` ao mesmo tempo.

Dentro do compose, os serviços falam entre si e com a infra pelo nome do serviço (`postgres`, `redis`, `localstack`, `dynamodb:8000`, `customer:8085`, `accounts:8081`), com as mesmas variáveis do `.env.example` apontando para esses nomes (veja o perfil no `docker-compose.yml`). O emissor dos tokens muda: os contêineres validam o `iss` `http://mock-oauth2-server:8080/default`, e o mock monta o `iss` pelo Host pedido (veja [Identidade local](#identidade-local-mock-oauth2-server)). Por isso, com o perfil `apps`, peça os tokens do host com `MOCK_OAUTH2_HOST=mock-oauth2-server:8080`, que o `token.sh` manda como cabeçalho Host; o seed e o smoke usam o `token.sh` e herdam a variável:
```bash
export MOCK_OAUTH2_HOST=mock-oauth2-server:8080
bash infra/local/seed-check.sh http://127.0.0.1 1010000
bash infra/local/smoke.sh http://127.0.0.1
```
É assim que o CI roda o smoke: o job `images` constrói as cinco imagens a partir dos jars dos jobs de build, e o job `smoke` sobe o perfil `apps` com elas. Um token pedido sem `MOCK_OAUTH2_HOST` (inclusive pelas coleções Bruno) sai com o `iss` `http://localhost:8090/default` e leva `401` nos contêineres.

O mesmo vale para o BFF: o `up` do perfil `apps` precisa de `MOCK_OAUTH2_HOST=mock-oauth2-server:8080` no ambiente, porque o `bff` pede os tokens ao mock com esse Host (padrão `localhost:8090`, certo só para serviços em `bootRun`); senão os serviços devolvem `401` ao BFF. Esse `up` também constrói as imagens `web` e `bff` (`digitalbanking/web` e `digitalbanking/bff`, de `frontend/`), que não têm perfil e sobem em qualquer `up`:
```bash
export MOCK_OAUTH2_HOST=mock-oauth2-server:8080
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml --profile apps up --wait
```

**Atualizar um serviço numa stack com dados.** O LocalStack roda sem `PERSISTENCE`: recriar o contêiner dele perde filas e mensagens, mesmo sem apagar volumes; o ledger (PostgreSQL) e o extrato (DynamoDB Local) ficam nos volumes, mas um evento em trânsito não chega ao extrato. Numa stack anterior ao DynamoDB Local, perde também o extrato inteiro: [migre](#migrar-o-extrato-de-uma-stack-anterior-ao-dynamodb-local) antes. Por isso, para atualizar só um serviço, não recrie o resto:
```bash
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml --profile apps up -d --no-deps <serviço>
```
Ao atualizar uma stack anterior à etapa 9 (e recriar `customer` e `notification`), crie antes a fila do `notification` para o `CustomerRegistered`: o `ready.d` só roda na partida do LocalStack, que aqui não é recriado, e a migration V5 do `customer` (que republica os cadastros para o `notification` aprender os nomes, [ADR 0025](adr/0025-nome-da-contraparte-nas-notificacoes.md)) roda uma vez só: sem a fila e a inscrição, os eventos republicados se perdem e os nomes ficam "outro cliente". O script é idempotente; rode-o no contêiner e confira a inscrição, antes de recriar os dois serviços:
```bash
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml exec localstack bash /etc/localstack/init/ready.d/03-notification-queues.sh
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml exec -e AWS_ACCESS_KEY_ID=test -e AWS_SECRET_ACCESS_KEY=test -e AWS_DEFAULT_REGION=us-east-1 localstack \
  aws --endpoint-url http://127.0.0.1:4566 sns list-subscriptions-by-topic --topic-arn arn:aws:sns:us-east-1:000000000000:customer-events.fifo --query 'Subscriptions[].Endpoint' --output text
```
A última saída tem de terminar em `:notification-customer.fifo`. Só então recrie `customer` e `notification` (comando acima).

Se o serviço for `web` ou `bff`, recarregue em seguida o nginx, que resolve esses dois nomes só na partida e responderia `502` com o IP antigo:
```bash
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml exec nginx nginx -s reload
```

### Web em desenvolvimento (React)
Para mexer no web com recarga automática, em vez de reconstruir a imagem, rode o Vite na pasta `frontend/` (Node 24, `npm ci` na primeira vez):
```bash
npm run dev -w web   # http://localhost:5173; /bff vai por proxy para http://127.0.0.1:3000
npm test             # unidade do web e do bff
npm run build        # tsc + vite build (web/dist) e nest build (bff)
```
O proxy espera o BFF em `127.0.0.1:3000` (`npm run start:dev -w bff`, com as variáveis de [`frontend/README.md`](../frontend/README.md)); o contêiner `bff` do compose não publica essa porta. Para ver o web atrás do nginx, reconstrua a imagem como acima.

### Migrar o extrato de uma stack anterior ao DynamoDB Local
Vale para a stack cujo extrato ainda está no DynamoDB do LocalStack. **A origem só existe enquanto aquele contêiner do LocalStack não parar nem for recriado**, e o compose novo o recria. Por isso a ordem importa: copie antes de qualquer `up` geral. O script só lê a origem, só cria o que falta no destino e falha se a contagem ou o conteúdo divergirem; pode ser repetido enquanto a origem existir e o `statement` estiver parado.

1. Descubra o projeto e a etiqueta das imagens da stack (`docker compose ls`; `docker ps --format '{{.Names}} {{.Image}}'`) e use os dois em todos os comandos. No exemplo, projeto `capzzpay`, etiqueta `local` e portas padrão:
```bash
export COMPOSE_PROJECT_NAME=capzzpay
export IMAGE_TAG=local
export MOCK_OAUTH2_HOST=mock-oauth2-server:8080
```
2. Acrescente `DYNAMODB_PORT=8000` e `STATEMENT_DYNAMODB_ENDPOINT=http://127.0.0.1:8000` ao `infra/local/.env` e construa a imagem nova do `statement`. A imagem antiga ignora o endpoint novo, e construir não mexe no contêiner que está rodando:
```bash
./gradlew :services:statement:bootstrap:bootJar
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml --profile apps build statement
```
3. Pare quem publica eventos (com `bootRun`, Ctrl+C nos três):
```bash
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml --profile apps stop accounts transfers customer
```
4. Espere as filas esvaziarem: repita até os dois números de cada uma serem `0`. O que estiver nelas e nas DLQs se perde no passo 8; devolva antes as mensagens de DLQ que importarem ([DLQ do statement](#dlq-do-statement), [DLQ do notification](#dlq-do-notification)).
```bash
for queue in statement-accounts.fifo notification-accounts.fifo notification-transfers.fifo notification-customer.fifo statement-accounts-dlq.fifo; do
  docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml exec -T \
    -e AWS_ACCESS_KEY_ID=test -e AWS_SECRET_ACCESS_KEY=test -e AWS_DEFAULT_REGION=us-east-1 \
    localstack aws --endpoint-url http://127.0.0.1:4566 sqs get-queue-attributes \
    --queue-url "http://127.0.0.1:4566/000000000000/$queue" \
    --attribute-names ApproximateNumberOfMessages ApproximateNumberOfMessagesNotVisible
done
```
5. Pare o `statement` (com `bootRun`, Ctrl+C):
```bash
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml --profile apps stop statement
```
6. Suba só o banco novo, citando o serviço, para o LocalStack não ser recriado:
```bash
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml up --wait dynamodb
```
7. Copie e confira. O script exige que os endpoints sejam as portas do projeto informado:
```bash
python3 infra/local/dynamodb/migrate_from_localstack.py --project "$COMPOSE_PROJECT_NAME" \
  --source-endpoint http://127.0.0.1:4566 --target-endpoint http://127.0.0.1:8000
```
Só siga com `OK: <n> itens na origem e no destino, conteudo identico`, com `<n>` diferente de zero se havia contas. Se falhar, pare aqui: a origem está intacta, e `docker start` nos contêineres parados devolve a stack ao que era.

8. Troque a stack. Este passo recria o LocalStack e descarta a origem; com `bootRun`, tire o `--profile apps` e suba os serviços de novo com o `.env` atualizado:
```bash
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml --profile apps up --wait --no-build
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml exec nginx nginx -s reload
```
Confira o extrato de uma conta pela API. Depois da troca, rodar o script de novo falha sem mudar nada, porque a origem não existe mais.

**Extrato já perdido** (o LocalStack antigo parou ou foi recriado antes da cópia; `docker start` não devolve a tabela, porque ele roda sem persistência). Não há o que migrar: suba a stack nova (passo 8), faça backup com `pg_dump` do banco `accounts` e republique pelo outbox, primeiro os lançamentos e depois as aberturas de conta (`update outbox set published_at = null where event_type = 'LedgerEntryRecorded'`, depois `'AccountOpened'`). O `statement` grava por `accountId` + `sequence` com `PutItem` condicional e o `notification` descarta eventos já processados, então o reenvio não duplica nada. Confira que o extrato tem um item por lançamento do ledger e que a contagem de notificações por tipo não mudou. Foi assim que a stack `capzzpay` voltou em 2026-11-23 (#234). Antes de começar, confira de qual pasta e com qual `.env` a stack foi subida (`docker compose ls` mostra o arquivo de configuração): uma stack subida por uma worktree usa o `.env` dela, e o do checkout principal pode estar desatualizado. O `smoke.sh` cria clientes, contas e transferências: não o rode numa stack com dados que você quer manter como estão.

## 5. Conferir a API pelo nginx
Primeiro o health, que deve responder `"status":"UP"` (aberto, sem token; os detalhes só para admin). Depois o seed do dono (abaixo) e o smoke. Os dois pegam tokens no `mock-oauth2-server` com `infra/local/auth/token.sh` e precisam dos cinco serviços no ar:
```bash
curl http://localhost/accounts/actuator/health
infra/local/seed.sh http://localhost
infra/local/smoke.sh http://localhost
```
O smoke é o mesmo que roda no CI. Cada cliente do smoke é um `sub` novo, cadastrado no `customer` com o próprio token; créditos e débitos de preparo vão com o token de admin do usuário de teste do dono (sem depender do seed). Ele começa pela identidade: `GET /customers/me` sem token dá `401`, o cadastro novo fica `ACTIVE` (`OK: cliente <id> cadastrado e ACTIVE`), e a conta aberta pelo cliente dá `401` sem token e `404` para outro cliente (`OK: conta <id> sem token 401 e de outro cliente 404`). Na parte do `accounts`, ele imprime `OK: eventos de <id> na fila accounts-events-inspection.fifo: AccountOpened LedgerEntryRecorded:1 LedgerEntryRecorded:2 LedgerEntryRecorded:3` (o lançamento 1 é o bônus de boas-vindas); a parte do `transfers` (transferência com o token do cliente da origem) termina em `OK: eventos de <transferId> na fila transfers-events-inspection.fifo: TransferCompleted`; em seguida o smoke confere, em `GET /accounts/<origem>/daily-limit` pelo nginx, o limite diário da origem (`OK: limite diario da conta <id> (limite:usado:disponivel): 500000:400:499600`). As duas linhas seguintes são as do `notification`: o smoke espera até 45 s (45 tentativas de 1 s, acima da visibilidade de 30 s da fila: o evento da transferência pode chegar antes do da conta e voltar à fila só depois de 30 s) e confere `TRANSFER_SENT` para o cliente da origem e `TRANSFER_RECEIVED` para o da conta de destino, lendo `GET /notifications` pelo nginx com o token de cada um (`OK: cliente <id> recebeu TRANSFER_SENT` e `OK: cliente <id> recebeu TRANSFER_RECEIVED`). A última linha é a do `statement`: o smoke espera até 30 s e confere, em `GET /accounts/<id>/statement` pelo nginx, os três lançamentos da primeira conta (bônus, crédito e débito), do mais novo para o mais antigo (`OK: extrato da conta <id>: 3:DEBIT:300:10700 2:CREDIT:1000:11000 1:CREDIT:10000:10000`, no formato `sequence:kind:amount:balanceAfter`). Ele apaga da fila de inspeção `accounts-events-inspection.fifo` só as mensagens da conta que criou; as de outras contas ficam escondidas por 2 minutos depois do smoke. Como a fila de inspeção não tem consumidor (diferente de `statement-accounts.fifo`, que o `statement` consome), ela acumula: se o smoke não achar os próprios eventos, recrie o ambiente com `down -v`.

### Seed do usuário de teste do dono
`infra/local/seed.sh` prepara o usuário de teste do dono (`sub` `00000000-0000-7000-8000-000000000001`, `Owner Test`, `owner@example.test`, grupo `admin`): cadastra o cliente no `customer`, o deixa `ACTIVE` pelo papel de admin, abre a conta dele e credita o saldo inicial pelo admin (1000000 centavos; com o bônus de boas-vindas de 10000 da abertura, o saldo do dono é 1010000). Uma conta só: o `accounts` aceita uma conta não encerrada por cliente (`409 customer-already-has-account`; depois de encerrar, o cliente pode abrir outra, mas sem novo bônus de boas-vindas). As chaves de idempotência são fixas: rodar de novo devolve a mesma conta e não credita outra vez. O seed também cadastra o usuário de demonstração (`demo@example.test`, telefone `+5511999990001`) e o parceiro (`+5511999990002`), abre as contas, credita 200000 e 100000 e faz as transferências do demo: 12000 enviados, 30000 recebidos e uma de 495000 recusada pelo limite diário. Ao fim, imprime as linhas legíveis por máquina `OWNER_ACCOUNT`, `OWNER_BALANCE`, `DEMO_ACCOUNT`, `DEMO_BALANCE`, `DEMO_COMPLETED`, `DEMO_REFUSED`, `DEMO_NOTIFICATIONS` e `DEMO_COUNTERPART`; o CI roda o seed duas vezes e compara as duas execuções com `infra/local/seed-check.sh` (o mesmo script serve à mão). Com o saldo esperado como 2º argumento (ambiente novo), o script confere também os saldos absolutos (dono 1010000; demo 228000, com o bônus). Sem o 2º argumento, como na reexecução do `aws-up` com o demo já usado por visitantes, só vale a igualdade entre as duas execuções; as transferências do seed têm chave fixa e não mudam com o uso nem com o dia. Para começar do zero, recrie o ambiente com `down -v`.

### A mesma sequência à mão
Toda chamada leva `Authorization: Bearer <token>`. O cliente se cadastra no `customer` antes de abrir a conta, e o cliente da conta vem do token (sem `customerId` no corpo). Créditos e débitos são do admin (e da saga, com token de máquina); um cliente recebe `403`:
```bash
CUSTOMER=$(uuidgen | tr 'A-Z' 'a-z')
CUSTOMER_TOKEN=$(infra/local/auth/token.sh user $CUSTOMER)
ADMIN_TOKEN=$(infra/local/auth/token.sh user 00000000-0000-7000-8000-000000000001 admin)

curl -sS -X POST http://localhost/customers -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H 'Content-Type: application/json' -d '{"name":"Cliente Teste","email":"cliente@example.test","phone":"+5531988880001"}'

ACCOUNT_ID=$(curl -sS -X POST http://localhost/accounts -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H "Idempotency-Key: $(uuidgen)" | jq -r .id)

curl -sS -X POST http://localhost/accounts/$ACCOUNT_ID/credits -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' -H "Idempotency-Key: $(uuidgen)" \
  -d '{"amount":1000,"description":"credito"}'

curl -sS -X POST http://localhost/accounts/$ACCOUNT_ID/debits -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' -H "Idempotency-Key: $(uuidgen)" \
  -d '{"amount":300,"description":"debito"}'

curl -sS http://localhost/accounts/$ACCOUNT_ID -H "Authorization: Bearer $CUSTOMER_TOKEN"
```
A última resposta mostra `"balance":10700` (o bônus de boas-vindas de 10000 da abertura, mais 1000 e menos 300). O bônus é só da primeira conta de cada cliente: quem encerra a conta e abre outra não recebe de novo ([ADR 0026](adr/0026-bonus-de-boas-vindas-por-cliente.md)). O telefone é obrigatório e único (celular E.164): rodar o cadastro de novo com outro e-mail e o mesmo telefone dá `409`; os dois usados aqui (`+55319888800xx`) não colidem com o seed, o smoke nem as coleções Bruno. Os telefones que a migração V4 do `customer` deu aos clientes antigos (`+5511900000001` em diante) são fictícios e podem colidir com um número real escolhido depois (`409`). Valores em centavos. `Idempotency-Key` é obrigatório nos `POST` do `accounts` e do `transfers`; repetir a mesma chave devolve `200` com o resultado original. O header `X-Correlation-Id` é repassado pelo nginx e devolvido na resposta.

### Transferência
O smoke acima também cobre o `transfers`: abre duas contas, credita 1000 na origem, transfere 400, espera o status `COMPLETED`, confere os saldos (10600 na origem, 10400 no destino, com o bônus de 10000 de cada conta) e o evento `TransferCompleted` na fila `transfers-events-inspection.fifo`. A mesma sequência à mão (reaproveita o `ACCOUNT_ID` e o `CUSTOMER_TOKEN` do passo anterior como origem, que precisa de saldo para os 400; com o saldo 10700 do passo anterior, sobra 10300). A transferência vai com o token do cliente da origem; origem de outro cliente dá `404 account-not-found`, e cliente bloqueado (`PUT /admin/customers/{id}/status` com `{"status":"BLOCKED"}`) dá `422 customer-blocked`:
```bash
OTHER=$(uuidgen | tr 'A-Z' 'a-z')
OTHER_TOKEN=$(infra/local/auth/token.sh user $OTHER)
curl -sS -X POST http://localhost/customers -H "Authorization: Bearer $OTHER_TOKEN" \
  -H 'Content-Type: application/json' -d '{"name":"Outro Cliente","email":"outro@example.test","phone":"+5531988880002"}'
DESTINATION_ID=$(curl -sS -X POST http://localhost/accounts -H "Authorization: Bearer $OTHER_TOKEN" \
  -H "Idempotency-Key: $(uuidgen)" | jq -r .id)

TRANSFER_KEY=$(uuidgen)
curl -sS -i -X POST http://localhost/transfers -H "Authorization: Bearer $CUSTOMER_TOKEN" \
  -H 'Content-Type: application/json' -H "Idempotency-Key: $TRANSFER_KEY" \
  -d "{\"sourceAccountId\":\"$ACCOUNT_ID\",\"destinationAccountId\":\"$DESTINATION_ID\",\"amount\":400,\"description\":\"transferencia\"}"
```
A primeira resposta é `201` com `"status":"COMPLETED"` (ou `202` se a saga ainda estiver em andamento; consulte `GET /transfers/<id>` até concluir). Repetir o mesmo comando com a mesma `Idempotency-Key` devolve `200` com a mesma transferência, sem novo débito. Os saldos ficam em 10300 (origem) e 10400 (destino):
```bash
curl -sS http://localhost/accounts/$ACCOUNT_ID -H "Authorization: Bearer $CUSTOMER_TOKEN" | jq .balance
curl -sS http://localhost/accounts/$DESTINATION_ID -H "Authorization: Bearer $OTHER_TOKEN" | jq .balance
```
O health do serviço sai em `http://localhost/transfers/actuator/health`.

## 6. Ver os eventos na fila de inspeção
Cada evento do `accounts` chega à fila `accounts-events-inspection.fifo`, e cada evento do `transfers` à `transfers-events-inspection.fifo`; nenhuma das duas tem consumidor (troque o nome da fila nos comandos abaixo para ler a do `transfers`). O corpo é o envelope do evento e `eventType` e `correlationId` vêm como atributos da mensagem. A CLI da AWS que vem na imagem do LocalStack lê a fila sem instalar nada no Mac:
```bash
docker compose -f infra/local/docker-compose.yml exec -T \
  -e AWS_ACCESS_KEY_ID=test -e AWS_SECRET_ACCESS_KEY=test -e AWS_DEFAULT_REGION=us-east-1 \
  localstack aws --endpoint-url http://127.0.0.1:4566 sqs receive-message \
  --queue-url http://127.0.0.1:4566/000000000000/accounts-events-inspection.fifo \
  --max-number-of-messages 10 --message-attribute-names All \
  | jq '.Messages[] | {eventType: .MessageAttributes.eventType.StringValue, body: (.Body | fromjson)}'
```
A leitura não apaga as mensagens: elas voltam para a fila depois de 30 s. Numa fila FIFO, enquanto uma mensagem de uma conta está em leitura, as seguintes da mesma conta esperam. Com a CLI da AWS instalada no Mac (`brew install awscli`), o mesmo comando funciona direto com `aws --endpoint-url http://localhost:4566` e as mesmas variáveis `AWS_*`. O relay também expõe métricas em `http://localhost/accounts/actuator/metrics/outbox.pending` e `.../outbox.relay.cycles`.

### DLQ do notification
Cada fila do `notification` tem uma DLQ FIFO: `notification-accounts-dlq.fifo`, `notification-transfers-dlq.fifo` e `notification-customer-dlq.fifo` ([ADR 0025](adr/0025-nome-da-contraparte-nas-notificacoes.md)). A mensagem vai para lá na 5ª tentativa que falha (`NOTIFICATION_MAX_RECEIVE_COUNT`, visibilidade de 30 s em `NOTIFICATION_VISIBILITY_TIMEOUT_SECONDS`). A métrica `notification.dlq.messages` também mostra a quantidade, lida do SQS a cada 30 s (`NOTIFICATION_DLQ_METRICS_REFRESH_INTERVAL`). Os comandos usam a CLI da AWS da imagem do LocalStack.

Para ver quantas mensagens estão paradas (troque o nome para a DLQ de `transfers`):
```bash
docker compose -f infra/local/docker-compose.yml exec -T \
  -e AWS_ACCESS_KEY_ID=test -e AWS_SECRET_ACCESS_KEY=test -e AWS_DEFAULT_REGION=us-east-1 \
  localstack aws --endpoint-url http://127.0.0.1:4566 sqs get-queue-attributes \
  --queue-url http://127.0.0.1:4566/000000000000/notification-accounts-dlq.fifo \
  --attribute-names ApproximateNumberOfMessages QueueArn
```
Depois de corrigir a causa, devolva as mensagens à fila de origem com o ARN da DLQ (campo `QueueArn` acima) e o da fila (`arn:aws:sqs:us-east-1:000000000000:notification-accounts.fifo`):
```bash
docker compose -f infra/local/docker-compose.yml exec -T \
  -e AWS_ACCESS_KEY_ID=test -e AWS_SECRET_ACCESS_KEY=test -e AWS_DEFAULT_REGION=us-east-1 \
  localstack aws --endpoint-url http://127.0.0.1:4566 sqs start-message-move-task \
  --source-arn <arn da DLQ> --destination-arn <arn da fila>
```
O consumo é idempotente (`processed_events`), então devolver uma mensagem já processada não duplica a notificação.

### Cache do extrato
A primeira página do extrato fica no Redis na chave `statement:first-page:v1:{accountId}`, com TTL de 50 a 70 s, e o consumo de um lançamento da conta apaga a chave e incrementa a versão da conta em `statement:first-page-version:v1:{accountId}` (TTL de 1 dia); a leitura só grava a página se a versão não mudou. Para ver o que está guardado:
```bash
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml exec redis redis-cli --scan --pattern 'statement:first-page*:v1:*'
```
As métricas do cache saem pelo actuator, direto no serviço ou pelo nginx (`statement.cache.requests` com a tag `result` = `hit`, `miss` ou `error`, só para leituras; `statement.cache.writes` com `result` = `ok`, `stale` (gravação descartada porque a versão mudou) ou `error`, para a gravação da página; `statement.cache.evictions` com `result` = `ok` ou `error`):
```bash
curl http://localhost:8084/actuator/metrics/statement.cache.requests
curl http://localhost/statement/actuator/metrics/statement.cache.evictions
```

### DLQ do statement
A fila `statement-accounts.fifo` tem a DLQ FIFO `statement-accounts-dlq.fifo`. A mensagem vai para lá na 120ª tentativa que falha, cerca de 1 hora com a visibilidade de 30 s (`STATEMENT_MAX_RECEIVE_COUNT`, padrão 120: para provocar a DLQ localmente, suba o compose com um valor baixo, como 3; visibilidade de 30 s em `STATEMENT_VISIBILITY_TIMEOUT_SECONDS`, ambos lidos pelo compose; veja o `.env.example`). A métrica `statement.dlq.messages` também mostra a quantidade (o valor é relido do SQS a cada 30 s, `STATEMENT_DLQ_METRICS_REFRESH`); consulte pelo actuator, direto no serviço ou pelo nginx:
```bash
curl http://localhost:8084/actuator/metrics/statement.dlq.messages
curl http://localhost/statement/actuator/metrics/statement.dlq.messages
```
A resposta traz `"measurements":[{"statistic":"VALUE","value":0.0}]` com a DLQ vazia. Para ver as mensagens paradas pela CLI da AWS:
```bash
docker compose -f infra/local/docker-compose.yml exec -T \
  -e AWS_ACCESS_KEY_ID=test -e AWS_SECRET_ACCESS_KEY=test -e AWS_DEFAULT_REGION=us-east-1 \
  localstack aws --endpoint-url http://127.0.0.1:4566 sqs get-queue-attributes \
  --queue-url http://127.0.0.1:4566/000000000000/statement-accounts-dlq.fifo \
  --attribute-names ApproximateNumberOfMessages QueueArn
```
Depois de corrigir a causa, devolva as mensagens à fila de origem com o ARN da DLQ (campo `QueueArn` acima) e o da fila (`arn:aws:sqs:us-east-1:000000000000:statement-accounts.fifo`):
```bash
docker compose -f infra/local/docker-compose.yml exec -T \
  -e AWS_ACCESS_KEY_ID=test -e AWS_SECRET_ACCESS_KEY=test -e AWS_DEFAULT_REGION=us-east-1 \
  localstack aws --endpoint-url http://127.0.0.1:4566 sqs start-message-move-task \
  --source-arn <arn da DLQ> --destination-arn <arn da fila>
```
O consumo é idempotente (`PutItem` condicional pela chave `accountId` + `sequence`), então devolver uma mensagem já gravada não duplica o lançamento. Para conferir a tabela:
```bash
curl -sS http://127.0.0.1:8000/ \
  -H 'Content-Type: application/x-amz-json-1.0' \
  -H 'X-Amz-Target: DynamoDB_20120810.Scan' \
  -H 'Authorization: AWS4-HMAC-SHA256 Credential=test/20260101/us-east-1/dynamodb/aws4_request, SignedHeaders=host, Signature=0' \
  -d '{"TableName":"statement","Limit":10}' | jq .
```

## 7. Coleção Bruno
São cinco coleções em `api/bruno/` (`accounts`, `transfers`, `notification`, `statement` e `customer`), cada uma com o ambiente `local` (`baseUrl`, `tokenUrl` do `mock-oauth2-server` e `adminSub`, o usuário de teste do dono). O script da coleção (`collection.bru`) pega os tokens no mock uma vez por execução: `customerToken` (um cliente novo, já cadastrado no `customer`; nas quatro primeiras coleções é o token padrão das requisições, por herança), `otherCustomerToken` (outro cliente cadastrado, para o `404` do recurso alheio) e `adminToken`. As requisições de admin (créditos, débitos, troca de tipo, `/admin/**`) usam o `adminToken`. Precisa do compose e dos cinco serviços no ar. O script de `accounts`, `notification`, `statement` e `transfers` é uma cópia idêntica (o Bruno não compartilha scripts entre coleções): mude as quatro juntas, porque o job `lint` do CI compara as cópias e falha se divergirem; o do `customer` é uma variante (cliente ainda sem cadastro) com o mesmo pedido de token.

### Coleção do `accounts`
A coleção fica em `api/bruno/accounts/`, com uma requisição por endpoint e `Idempotency-Key` nova a cada envio (`{{$guid}}`, ou uma chave gerada no script da requisição). A consulta de lançamento por chave (`08`) usa a chave do crédito (`03`): rode o `03` antes do `08`. Depois vêm `09` (conta de outro cliente, `404`), `10` (sem token, `401`) `11` (crédito com o token do cliente, `403`), `12` e `13` (o admin bloqueia e desbloqueia a conta), `14` (esvazia a conta), `15` (`GET /accounts/me`, a própria conta), `16` (`GET /accounts/recipient`, só o `accountId` do destino), `17` (encerra a conta de saldo zero) e `18` (a conta encerrada some de `/accounts/recipient?customerId=`, `404`).
- **App do Bruno:** "Open Collection" → `api/bruno/accounts`, escolha o ambiente `local` e rode "Abrir conta" primeiro (ela guarda o `accountId` para as outras).
- **Terminal** (precisa da CLI dos pré-requisitos opcionais):
  ```bash
  (cd api/bruno/accounts && bru run --env local)
  ```
  Esperado: `Requests 18 (18 Passed)`. A requisição de limite grava o valor da migration (`GOLD` = 200000), sem alterar o ambiente.

### Coleção do `notification`
A coleção `api/bruno/notification/` também roda sozinha: o script da requisição `01` abre uma conta para o cliente e outra para o outro cliente, credita a origem pelo admin, transfere com o token do cliente e espera até 45 s pela notificação (acima da visibilidade de 30 s da fila: o evento da transferência pode chegar antes do da conta e voltar à fila só depois de 30 s). O token diz de quem são as notificações. As requisições seguem a ordem: `01` lista as notificações do remetente (`TRANSFER_SENT`), `02` as do destinatário com o token dele (`TRANSFER_RECEIVED`), `03` marca como lidas até a mais recente, `04` confere `unreadCount` zero, `05` mostra o `400` de um `customerId` que não é UUID e `06` o `404` de um cliente que pede as notificações de outro.
- **Terminal:**
  ```bash
  (cd api/bruno/notification && bru run --env local)
  ```
  Esperado: `Requests 6 (6 Passed)`.

### Coleção do `statement`
A coleção `api/bruno/statement/` também roda sozinha: o script da requisição `01` abre uma conta para o cliente, faz dois créditos pelo admin (1000 e 2000) e espera até 30 s pelo extrato ter os três lançamentos (o bônus de boas-vindas de 10000 e os dois créditos). As requisições seguem a ordem: `01` lê o extrato (sequences 3, 2 e 1, valores 2000, 1000 e 10000, saldos 13000, 11000 e 10000), `02` pede a primeira página com `limit=2` e guarda o `nextCursor`, `03` segue o cursor até o último lançamento, `04` mostra o `404` de uma conta desconhecida, `05` o `400` de um `limit` fora de 1 a 100 e `06` o `404` do extrato lido por outro cliente.
- **Terminal:**
  ```bash
  (cd api/bruno/statement && bru run --env local)
  ```
  Esperado: `Requests 6 (6 Passed)`.

### Coleção do `transfers`
A coleção `api/bruno/transfers/` roda sozinha, sem depender da coleção do `accounts`: o script da requisição `01` abre a origem do cliente e o destino do outro cliente pelo `accounts`, credita 1000 na origem pelo admin e guarda a `Idempotency-Key` e os ids em variáveis. Rode as requisições na ordem: `01` cria a transferência (`201`), `02` repete a mesma chave (`200`, mesma transferência) e `03` consulta pelo id, `04` consulta o limite diário da origem (300 usados de 500000), `05` pede uma transferência de 500000 e recebe `422 transfer-failed` com o motivo `daily-limit-exceeded`, `06` confere que a recusa não consumiu o limite e `07` mostra o `404 account-not-found` de quem transfere de uma origem de outro cliente.
- **App do Bruno:** "Open Collection" → `api/bruno/transfers`, ambiente `local`.
- **Terminal:**
  ```bash
  (cd api/bruno/transfers && bru run --env local)
  ```
  Esperado: `Requests 7 (7 Passed)`.

### Coleção do `customer`
A coleção `api/bruno/customer/` usa um cliente novo, ainda sem cadastro: `01` cadastra (`201`, `ACTIVE`, o id é o `sub` do token), `02` repete o cadastro (`200`, o mesmo), `03` lê `GET /customers/me`, `04` o admin consulta `GET /admin/customers/{id}`, `05` o admin bloqueia (`BLOCKED`), `06` o cliente chama a rota de admin (`403 forbidden`), `07` o admin desbloqueia (`ACTIVE`) e `08` mostra o `401` sem token, `09` o `400` do cadastro sem telefone válido, `10` a troca do telefone, `11` o `409` do cadastro com um telefone em uso, `12` a busca por telefone, `13` o `404` da busca sem cliente e `14` o `400` da busca com telefone inválido.
- **Terminal:**
  ```bash
  (cd api/bruno/customer && bru run --env local)
  ```
  Esperado: `Requests 14 (14 Passed)`.

## Identidade local (mock-oauth2-server)
O compose sobe o `mock-oauth2-server` (`ghcr.io/navikt/mock-oauth2-server`) na porta 8090 (`MOCK_OAUTH2_PORT`), no lugar do Cognito. O emissor é `http://localhost:8090/default`; os serviços o recebem em `*_JWT_ISSUER_URI` (`ACCOUNTS_`, `TRANSFERS_`, `NOTIFICATION_`, `STATEMENT_` e `CUSTOMER_`, veja o `.env.example`). Os serviços que chamam outros serviços usam um cliente de máquina (client credentials, escopo `digitalbanking/internal`): `ACCOUNTS_OAUTH_CLIENT_ID`/`_SECRET` e `TRANSFERS_OAUTH_CLIENT_ID`/`_SECRET`. O mock aceita qualquer segredo, então `local-secret` serve; são valores só de desenvolvimento local.

A configuração dos tokens fica em `infra/local/auth/mock-oauth2-config.json` (montada no contêiner e lida por `JSON_CONFIG_PATH`; o login interativo fica desligado). O mock emite o `sub` e o claim `groups` pedidos na requisição:
- **Usuário** (`password grant`): o `sub` é o `username` enviado, que deve ser um UUID (o id do cliente é o `sub`); `groups` é `["admin"]` com o parâmetro `user_groups=admin` e `[]` sem ele. Não leva o escopo `digitalbanking/internal`.
- **Serviço** (`client_credentials` com `scope=digitalbanking/internal`): `sub` igual ao `client_id` e `scope` igual a `digitalbanking/internal`.

Para pegar um token, use `infra/local/auth/token.sh` (precisa de `curl` e `jq`; a URL do emissor muda com `MOCK_OAUTH2_URL`):
```bash
infra/local/auth/token.sh user 11111111-1111-7111-8111-111111111111        # cliente
infra/local/auth/token.sh user 00000000-0000-7000-8000-000000000001 admin  # usuário de teste do dono (admin e cliente)
infra/local/auth/token.sh service                                          # serviço (client_id accounts); outro: service transfers
curl -H "Authorization: Bearer $(infra/local/auth/token.sh user <uuid>)" http://localhost/...
```
**Peça o token sempre em `localhost:8090`.** O mock monta o `iss` do token a partir do Host da requisição, e os serviços só aceitam `iss` igual a `http://localhost:8090/default`. Um token pedido em `http://127.0.0.1:8090/default/token` sai com `iss` `http://127.0.0.1:8090/default` e leva `401` em todos os serviços, mesmo válido. Por isso o `token.sh`, o `tokenUrl` das coleções Bruno e o seed usam `localhost`, e não `127.0.0.1` (a regra do `127.0.0.1` vale para healthchecks e curls dentro dos contêineres, não para o emissor). Se trocar `MOCK_OAUTH2_URL`, troque também os `*_JWT_ISSUER_URI` para o mesmo host. A exceção é o perfil `apps` do compose (serviços em contêineres), em que o emissor é `http://mock-oauth2-server:8080/default` e o `token.sh` pede os tokens com `MOCK_OAUTH2_HOST=mock-oauth2-server:8080`.

O usuário de teste do dono tem `sub` `00000000-0000-7000-8000-000000000001`, nome `Owner Test`, e-mail `owner@example.test` e o grupo `admin`; o nome e o e-mail entram no cadastro do `customer` pelo seed local. Os tokens duram 1 hora.

## 8. Derrubar tudo
Pare os cinco `bootRun` com Ctrl+C nos terminais deles. Depois derrube os contêineres (com o perfil `apps`, acrescente `--profile apps` para derrubar também os serviços). Sem `-v`, o ledger e o extrato ficam nos volumes `postgres-data` e `dynamodb-data`, e o próximo `up` os encontra; as filas do LocalStack se perdem:
```bash
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml down
```
O CI prova isso com `infra/local/dynamodb/persistence-check.sh --project <projeto-descartável> [url-base]`, que grava, derruba, sobe e compara o extrato pela API. Ele derruba a stack do projeto informado: não o rode numa stack com dados que importam.

Com `-v`, os dados são apagados, o extrato inclusive:
```bash
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml down -v
```

## Opcional: grafo do código (graphify)
O repositório versiona o grafo do código em `graphify-out/`, usado por agentes para navegar o código. Instale a CLI com o uv (dos pré-requisitos opcionais), sempre com o extra `sql`:
```bash
uv tool install "graphifyy[sql]"
```
Sem o extra, as migrations ficam fora do grafo, o grafo novo sai com menos nós que o versionado e o `graphify update .` se recusa a sobrescrevê-lo ("Refusing to overwrite"). Se o graphify já estiver instalado sem o extra, reinstale com `uv tool install --force "graphifyy[sql]"` (ou, se veio pelo pip, `python3 -m pip install "graphifyy[sql]"`; num Python que recusa com "externally-managed-environment", use o uv). Para o agente usar o grafo, instale também a skill no Claude Code:
```bash
graphify install --platform claude
```
Uma vez por clone, instale os hooks do git, que atualizam o grafo a cada commit, checkout e merge:
```bash
graphify hook install
```
Para atualizar o grafo manualmente:
```bash
graphify update .
```
