# Rodando o projeto localmente (macOS)

Guia para subir a infraestrutura local, o serviço `accounts` e conferir a API pelo nginx.
Todos os comandos rodam na raiz do repositório.

## Pré-requisitos
- **JDK 21 instalado.** O Gradle não baixa JDK sozinho (`org.gradle.java.installations.auto-download=false` em `gradle.properties`). Por exemplo:
  ```bash
  brew install --cask temurin@21
  export JAVA_HOME=$(/usr/libexec/java_home -v 21)
  java -version   # deve mostrar 21
  ```
- **Docker Desktop em execução** (o compose e os testes de integração com Testcontainers precisam dele). Confira com `docker info`.
- **jq** para o smoke: `brew install jq`.
- **A partir do bloco 2 da etapa 1:** `LOCALSTACK_AUTH_TOKEN` preenchido em `infra/local/.env` (token da conta gratuita do LocalStack). Nunca coloque o token no `.env.example` nem em outro arquivo versionado; o `infra/local/.env` está no `.gitignore`.
- Opcional: CLI do Bruno (`npm install -g @usebruno/cli`) para rodar a coleção pelo terminal.

## 1. Configurar o ambiente
```bash
cp infra/local/.env.example infra/local/.env
```
Os valores do `.env.example` servem para desenvolvimento. Se a porta 80 ou 5432 já estiver em uso, troque `NGINX_PORT` ou `POSTGRES_PORT` no `infra/local/.env`; ao trocar `POSTGRES_PORT`, ajuste a porta em `ACCOUNTS_DB_URL` no mesmo arquivo, e ao trocar `NGINX_PORT`, use a porta nova nas URLs abaixo e no `baseUrl` da coleção Bruno.

## 2. Subir a infraestrutura (PostgreSQL, Redis, nginx)
```bash
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml up --wait
```
O script de init do PostgreSQL (roles `accounts_owner` e `accounts_app`, banco `accounts`) só roda com volume novo. Se mudar senhas ou o script, recrie com `down -v` (passo 7).

## 3. Build e testes
```bash
./gradlew build   # compila, ktlint, testes unitários, ArchUnit e testes de integração (precisa do Docker)
./gradlew test    # só os testes unitários, sem Docker
```

## 4. Subir o serviço `accounts`
O serviço lê as variáveis `ACCOUNTS_DB_*` do ambiente. Carregue o `.env` no shell e rode:
```bash
set -a; . infra/local/.env; set +a
./gradlew :services:accounts:bootstrap:bootRun
```
O serviço sobe na porta 8081 e aplica as migrations (Flyway) ao iniciar. Deixe esse terminal aberto e use outro para os passos seguintes.

## 5. Conferir a API pelo nginx
```bash
curl http://localhost/accounts/actuator/health   # "status":"UP"
infra/local/smoke.sh http://localhost            # abre conta, credita 1000, debita 300, confere saldo 700
```
O smoke é o mesmo que roda no CI e termina com `OK: conta <id> com saldo 700`. A mesma sequência à mão:
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

curl -sS http://localhost/accounts/$ACCOUNT_ID   # "balance":700
```
Valores em centavos. `Idempotency-Key` é obrigatório nos `POST`; repetir a mesma chave devolve `200` com o resultado original. O header `X-Correlation-Id` é repassado pelo nginx e devolvido na resposta.

## 6. Coleção Bruno
A coleção fica em `api/bruno/accounts/`, com uma requisição por endpoint e `Idempotency-Key` nova a cada envio (`{{$guid}}`).
- **App do Bruno:** "Open Collection" → `api/bruno/accounts`, escolha o ambiente `local` e rode "Abrir conta" primeiro (ela guarda o `accountId` para as outras).
- **Terminal:**
  ```bash
  cd api/bruno/accounts && bru run --env local
  ```
  Esperado: `Requests 7 (7 Passed)`. A requisição de limite grava o valor da migration (`GOLD` = 200000), sem alterar o ambiente.

## 7. Derrubar tudo
Pare o `bootRun` com Ctrl+C e rode:
```bash
docker compose --env-file infra/local/.env -f infra/local/docker-compose.yml down -v   # apaga os dados
```

## Opcional: grafo do código (graphify)
O repositório versiona o grafo do código em `graphify-out/`, usado por agentes para navegar o código.
```bash
uv tool install graphifyy   # instala a CLI graphify
graphify hook install       # uma vez por clone: atualiza o grafo a cada commit, checkout e merge
graphify update .           # atualiza o grafo manualmente
```
