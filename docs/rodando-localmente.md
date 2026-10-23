# Rodando o projeto localmente (macOS)

Guia para subir a infraestrutura local, o serviço `accounts` e conferir a API pelo nginx.
Todos os comandos rodam na raiz do repositório e valem tanto para o zsh (o shell padrão do macOS) quanto para o bash. Os blocos de comando não têm comentários: as explicações ficam no texto antes e depois de cada bloco, porque o zsh do macOS, por padrão, não aceita `# comentário` na linha de comando.

## Pré-requisitos
- **JDK 21 instalado.** O Gradle não baixa JDK sozinho (`org.gradle.java.installations.auto-download=false` em `gradle.properties`). Instale e confira com:
  ```bash
  brew install --cask temurin@21
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
- **jq** para o smoke: `brew install jq`.
- **Token do LocalStack.** O LocalStack emula o SNS e o SQS e exige um token, mesmo no plano gratuito. Crie uma conta Hobby em [app.localstack.cloud](https://app.localstack.cloud), copie o seu Auth Token no painel da conta e preencha `LOCALSTACK_AUTH_TOKEN` no `infra/local/.env` (passo 1). Sem o token, o contêiner do LocalStack encerra na partida e os testes com LocalStack são pulados. Nunca coloque o token no `.env.example` nem em outro arquivo versionado; o `infra/local/.env` está no `.gitignore`.
- **Opcional:** Node (para a CLI do Bruno) e uv (para o graphify):
  ```bash
  brew install node uv
  npm install -g @usebruno/cli
  ```

## 1. Configurar o ambiente
```bash
cp infra/local/.env.example infra/local/.env
```
Preencha `LOCALSTACK_AUTH_TOKEN` no `infra/local/.env`. Os outros valores do `.env.example` servem para desenvolvimento. Se a porta 80 ou 5432 já estiver em uso, troque `NGINX_PORT` ou `POSTGRES_PORT` no `infra/local/.env`. Ao trocar `POSTGRES_PORT`, ajuste a porta em `ACCOUNTS_DB_URL` no mesmo arquivo. Ao trocar `NGINX_PORT`, use a porta nova nas URLs abaixo (`http://localhost:<porta>`) e no `baseUrl` de `api/bruno/accounts/environments/local.bru`.

## 2. Subir a infraestrutura (PostgreSQL, Redis, nginx, LocalStack)
```bash
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml up --wait
```
O comando volta quando os quatro contêineres estão saudáveis. O LocalStack só fica saudável depois que o script de `infra/local/localstack/init/ready.d/` cria o tópico `accounts-events.fifo` e a fila `accounts-events-inspection.fifo`, assinada nele. O script de init do PostgreSQL (roles `accounts_owner` e `accounts_app`, banco `accounts`) só roda com volume novo: se mudar senhas ou o script, recrie com `down -v` (passo 8).

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

## 4. Subir o serviço `accounts`
O serviço lê as variáveis `ACCOUNTS_DB_*` e `ACCOUNTS_AWS_ENDPOINT` do ambiente. Num terminal novo, carregue o `.env` no shell (e o `JAVA_HOME`, se não estiver no `~/.zshrc`) e rode:
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
set -a; . infra/local/.env; set +a
./gradlew :services:accounts:bootstrap:bootRun
```
O serviço sobe na porta 8081 e aplica as migrations (Flyway) ao iniciar. A cada 500 ms o relay publica os eventos pendentes do outbox no tópico SNS do LocalStack. Deixe esse terminal aberto e use outro para os passos seguintes.

## 5. Conferir a API pelo nginx
Primeiro o health, que deve responder `"status":"UP"`; depois o smoke, que abre uma conta, credita 1000, debita 300, confere o saldo 700 e lê os eventos da conta na fila de inspeção:
```bash
curl http://localhost/accounts/actuator/health
infra/local/smoke.sh http://localhost
```
O smoke é o mesmo que roda no CI e termina com `OK: eventos da conta <id> na fila accounts-events-inspection.fifo: AccountOpened LedgerEntryRecorded:1 LedgerEntryRecorded:2`. Ele apaga da fila só as mensagens da conta que criou. A mesma sequência à mão:
```bash
ACCOUNT_ID=$(curl -sS -X POST http://localhost/accounts \
  -H 'Content-Type: application/json' -H "Idempotency-Key: $(uuidgen)" \
  -d "{\"customerId\":\"$(uuidgen)\"}" | jq -r .id)

curl -sS -X POST http://localhost/accounts/$ACCOUNT_ID/credits \
  -H 'Content-Type: application/json' -H "Idempotency-Key: $(uuidgen)" \
  -d '{"amount":1000,"description":"credito"}'

curl -sS -X POST http://localhost/accounts/$ACCOUNT_ID/debits \
  -H 'Content-Type: application/json' -H "Idempotency-Key: $(uuidgen)" \
  -d '{"amount":300,"description":"debito"}'

curl -sS http://localhost/accounts/$ACCOUNT_ID
```
A última resposta mostra `"balance":700`. Valores em centavos. `Idempotency-Key` é obrigatório nos `POST`; repetir a mesma chave devolve `200` com o resultado original. O header `X-Correlation-Id` é repassado pelo nginx e devolvido na resposta.

## 6. Ver os eventos na fila de inspeção
Cada evento publicado chega à fila `accounts-events-inspection.fifo`, que não tem consumidor. O corpo é o envelope do evento e `eventType` e `correlationId` vêm como atributos da mensagem. A CLI da AWS que vem na imagem do LocalStack lê a fila sem instalar nada no Mac:
```bash
docker compose -f infra/local/docker-compose.yml exec -T \
  -e AWS_ACCESS_KEY_ID=test -e AWS_SECRET_ACCESS_KEY=test -e AWS_DEFAULT_REGION=us-east-1 \
  localstack aws --endpoint-url http://127.0.0.1:4566 sqs receive-message \
  --queue-url http://127.0.0.1:4566/000000000000/accounts-events-inspection.fifo \
  --max-number-of-messages 10 --message-attribute-names All \
  | jq '.Messages[] | {eventType: .MessageAttributes.eventType.StringValue, body: (.Body | fromjson)}'
```
A leitura não apaga as mensagens: elas voltam para a fila depois de 30 s. Numa fila FIFO, enquanto uma mensagem de uma conta está em leitura, as seguintes da mesma conta esperam. Com a CLI da AWS instalada no Mac (`brew install awscli`), o mesmo comando funciona direto com `aws --endpoint-url http://localhost:4566` e as mesmas variáveis `AWS_*`. O relay também expõe métricas em `http://localhost/accounts/actuator/metrics/outbox.pending` e `.../outbox.relay.cycles`.

## 7. Coleção Bruno
A coleção fica em `api/bruno/accounts/`, com uma requisição por endpoint e `Idempotency-Key` nova a cada envio (`{{$guid}}`).
- **App do Bruno:** "Open Collection" → `api/bruno/accounts`, escolha o ambiente `local` e rode "Abrir conta" primeiro (ela guarda o `accountId` para as outras).
- **Terminal** (precisa da CLI dos pré-requisitos opcionais):
  ```bash
  (cd api/bruno/accounts && bru run --env local)
  ```
  Esperado: `Requests 7 (7 Passed)`. A requisição de limite grava o valor da migration (`GOLD` = 200000), sem alterar o ambiente.

## 8. Derrubar tudo
Pare o `bootRun` com Ctrl+C no terminal dele. Depois derrube os contêineres; o `-v` apaga os dados:
```bash
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml down -v
```

## Opcional: grafo do código (graphify)
O repositório versiona o grafo do código em `graphify-out/`, usado por agentes para navegar o código. Instale a CLI com o uv (dos pré-requisitos opcionais):
```bash
uv tool install graphifyy
```
Uma vez por clone, instale os hooks do git, que atualizam o grafo a cada commit, checkout e merge:
```bash
graphify hook install
```
Para atualizar o grafo manualmente:
```bash
graphify update .
```
