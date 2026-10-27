# Rodando o projeto localmente (macOS)

Guia para subir a infraestrutura local, o serviço `accounts` e conferir a API pelo nginx.
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
Preencha `LOCALSTACK_AUTH_TOKEN` no `infra/local/.env`. Os outros valores do `.env.example` servem para desenvolvimento. Se a porta 80 ou 5432 já estiver em uso, troque `NGINX_PORT` ou `POSTGRES_PORT` no `infra/local/.env`. Ao trocar `POSTGRES_PORT`, ajuste a porta em `ACCOUNTS_DB_URL` no mesmo arquivo. Ao trocar `NGINX_PORT`, use a porta nova nas URLs abaixo (`http://localhost:<porta>`) e no `baseUrl` de `api/bruno/accounts/environments/local.bru`.

## 2. Subir a infraestrutura (PostgreSQL, Redis, nginx, LocalStack)
```bash
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml up --wait
```
O comando volta quando os quatro contêineres estão saudáveis. O LocalStack só fica saudável depois que o script de `infra/local/localstack/init/ready.d/` cria o tópico `accounts-events.fifo` e a fila `accounts-events-inspection.fifo`, assinada nele. O script de init do PostgreSQL (`infra/local/postgres/init/01-create-service-databases.sh`) cria um banco por serviço, cada um com dois roles: `<serviço>_owner` (dono, usado pelo Flyway) e `<serviço>_app` (só `LOGIN`; as permissões vêm das migrations). Hoje são `accounts` e `transfers`. Os dados ficam no volume nomeado `postgres-data`, que sobrevive a `down` e `up`; o script só roda com volume novo, então, se mudar senhas ou o script, recrie com `down -v` (passo 8). O script é idempotente: para rodá-lo de novo sem apagar nada, execute `docker compose exec postgres /docker-entrypoint-initdb.d/01-create-service-databases.sh`. Ele cria só o que falta e não troca a senha de um role que já existe.

Para adicionar o banco de um serviço novo: acrescente ao fim do script a linha `create_service_db <serviço> <SERVICO>_DB_OWNER_PASSWORD <SERVICO>_DB_PASSWORD` (os dois últimos argumentos são os nomes das variáveis com as senhas), declare essas duas variáveis no `environment` do `postgres` em `docker-compose.yml` e em `infra/local/.env.example`, e recrie o volume com `down -v`. Sem o token, o `up --wait` volta com erro porque o LocalStack encerra, mas PostgreSQL, Redis e nginx ficam de pé.

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
O smoke é o mesmo que roda no CI e termina com `OK: eventos da conta <id> na fila accounts-events-inspection.fifo: AccountOpened LedgerEntryRecorded:1 LedgerEntryRecorded:2`. Ele apaga da fila só as mensagens da conta que criou; as de outras contas ficam escondidas por 2 minutos depois do smoke. Como a fila não tem consumidor, ela acumula: se o smoke não achar os próprios eventos, recrie o ambiente com `down -v`. A mesma sequência à mão:
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
