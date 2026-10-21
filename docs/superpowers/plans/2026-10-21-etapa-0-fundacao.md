# Etapa 0 — Fundação: plano de implementação

> **Para agentes:** SUB-SKILL OBRIGATÓRIO: `superpowers:subagent-driven-development` (recomendado) ou `superpowers:executing-plans` para executar este plano task a task. Passos usam checkbox (`- [ ]`).

**Objetivo:** monorepo Gradle com `build-logic`, ktlint, ArchUnit, esqueleto do `accounts` provado com Testcontainers, docker-compose local e CI no GitHub Actions.

**Arquitetura:** um módulo Gradle por camada (`domain`, `application`, `adapters`, `bootstrap`); plugins de convenção num included build `build-logic`; versões num catálogo único. O `bootstrap` junta tudo e carrega os testes de arquitetura e de integração.

**Stack:** Gradle 9.8.2, Kotlin 2.3.21, Spring Boot 4.1.1, JDK 21, ktlint 1.8.0 (plugin `org.jlleitschuh.gradle.ktlint` 14.2.0), ArchUnit 1.5.1, Testcontainers 2.0.5 (gerenciado pelo Boot), PostgreSQL 17.

**Spec:** [`docs/superpowers/specs/2026-10-21-etapa-0-fundacao-design.md`](../specs/2026-10-21-etapa-0-fundacao-design.md)

## Restrições globais
- Pacote raiz: `io.github.capozziv.digitalbanking.accounts`.
- Kotlin 2.3.21 é a versão que o Spring Boot 4.1.1 gerencia; plugin Kotlin na mesma versão.
- Dependências do Spring só pelo BOM `org.springframework.boot:spring-boot-dependencies:4.1.1` via `platform(...)`; sem `io.spring.dependency-management`.
- Spring Boot 4 tem starters modulares: `spring-boot-starter-webmvc`, `spring-boot-starter-jdbc`, `spring-boot-starter-flyway`, `spring-boot-starter-actuator`, `spring-boot-testcontainers`, `spring-boot-starter-test`.
- Testcontainers 2: classe `org.testcontainers.postgresql.PostgreSQLContainer`, artefatos `org.testcontainers:testcontainers-postgresql` e `testcontainers-junit-jupiter`.
- ArchUnit pelo artefato core `com.tngtech.archunit:archunit:1.5.1`, chamado de testes JUnit normais (sem o engine `archunit-junit5`, evitando acoplamento com a versão do JUnit Platform 6 gerenciada pelo Boot).
- `domain` e `application` sem nenhuma dependência Spring.
- Imagem `postgres:17-alpine` nos testes e no compose.
- Toolchain Java 21 sem download automático (sem resolver foojay): usa o JDK instalado.
- Commits com autor `capozziV <vitorcapozzi4@gmail.com>`, sem trailers.

## Ambiente de execução
- **Edição e git:** na pasta do usuário (VM do Mac). A VM não tem limite de taxa no Maven Central, mas não tem Docker.
- **Build completo (Testcontainers, compose):** no ambiente de nuvem, que tem JDK 21 e Docker. Antes do primeiro build lá:
  - `dockerd` com `/etc/docker/daemon.json` = `{"registry-mirrors":["https://mirror.gcr.io"]}`;
  - init script do Gradle em `~/.gradle/init.d/mirror.init.gradle.kts` trocando Maven Central por `https://maven-central.storage-download.googleapis.com/maven2/` em `pluginManagement` e em `dependencyResolutionManagement` (o Maven Central devolve `429` para o IP compartilhado da nuvem). Configuração do ambiente; não vai para o repo.
- **Sincronização:** empacotar a árvore de trabalho em `.scratch/sync.tar` (pasta no `.gitignore`), trazer para a nuvem e extrair numa pasta de trabalho; build roda lá. Correções voltam para a pasta do usuário antes do commit.
- **Push:** só o usuário consegue (`git push` no Mac) enquanto não houver credencial do GitHub nos ambientes do Claude. O critério "CI verde" é conferido depois do push.

## Foco de revisão
1. Arquivo Kotlin mal formatado precisa quebrar `./gradlew build` (ktlint) — Task 1, passo de verificação.
2. Classe em `adapters.web` usando classe de `adapters.persistence` precisa quebrar o build (ArchUnit) — Task 2.
3. `import org.springframework...` dentro de `domain` precisa falhar na compilação — Task 2.
4. `/actuator/health` precisa mostrar o componente `db` `UP`, não só o status geral — Task 3.
5. `GET /accounts/actuator/health` no nginx precisa chegar em `/actuator/health` da aplicação (prefixo removido) — Tasks 4 e 5.

---

### Task 1: Esqueleto do build (wrapper, catálogo, `build-logic`, módulos vazios, ktlint)

**Arquivos:**
- Criar: `settings.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradle/wrapper/*`, `gradlew`, `gradlew.bat`, `.gitignore`, `.gitattributes`
- Criar: `build-logic/settings.gradle.kts`, `build-logic/build.gradle.kts`
- Criar: `build-logic/src/main/kotlin/digitalbanking.kotlin-library.gradle.kts`, `digitalbanking.spring-module.gradle.kts`, `digitalbanking.spring-boot-app.gradle.kts`
- Criar: `services/accounts/{domain,application,adapters,bootstrap}/build.gradle.kts`

**Interfaces:**
- Produz: plugins `digitalbanking.kotlin-library`, `digitalbanking.spring-module`, `digitalbanking.spring-boot-app`; projetos `:services:accounts:domain|application|adapters|bootstrap`; aliases do catálogo usados pelas tasks seguintes (`libs.spring.boot.bom`, `libs.archunit`, `libs.testcontainers.postgresql`, `libs.testcontainers.junit.jupiter`, `libs.flyway.postgresql`, `libs.postgresql`).

- [ ] **Passo 1: Gerar o wrapper** baixando `https://services.gradle.org/distributions/gradle-9.8.2-bin.zip` para fora do repo e rodando `gradle wrapper --gradle-version 9.8.2 --distribution-type bin`. Conferir `distributionUrl` com `gradle-9.8.2-bin.zip`.
- [ ] **Passo 2: `gradle/libs.versions.toml`** com versões `kotlin = "2.3.21"`, `spring-boot = "4.1.1"`, `ktlint = "1.8.0"`, `ktlint-gradle = "14.2.0"`, `archunit = "1.5.1"`, `junit = "6.0.3"`, `assertj = "3.27.7"` (as duas últimas iguais às gerenciadas pelo Boot 4.1.1); bibliotecas `junit-bom` (`org.junit:junit-bom`), `assertj-core`, bibliotecas `spring-boot-bom` (`org.springframework.boot:spring-boot-dependencies`), `archunit`, `testcontainers-postgresql`, `testcontainers-junit-jupiter`, `flyway-postgresql` (`org.flywaydb:flyway-database-postgresql`), `postgresql` (`org.postgresql:postgresql`) — as quatro últimas sem versão (BOM); e as dependências de plugin para o `build-logic`: `kotlin-gradle-plugin`, `kotlin-allopen` (`org.jetbrains.kotlin:kotlin-allopen`), `spring-boot-gradle-plugin`, `ktlint-gradle` (`org.jlleitschuh.gradle:ktlint-gradle`).
- [ ] **Passo 3: `build-logic`** como included build (`pluginManagement { includeBuild("build-logic") }` no `settings.gradle.kts` raiz), com plugin `kotlin-dsl`, catálogo apontando para `../gradle/libs.versions.toml` e as quatro dependências de plugin acima. Nos precompiled plugins, acessar o catálogo por `extensions.getByType<VersionCatalogsExtension>().named("libs")`.
- [ ] **Passo 4: Plugins de convenção.**
  - `digitalbanking.kotlin-library`: `kotlin("jvm")`, `org.jlleitschuh.gradle.ktlint` com `version = 1.8.0`; `kotlin { jvmToolchain(21); compilerOptions { allWarningsAsErrors = true } }`; testes sem Spring: `testImplementation(platform(junit-bom))`, `testImplementation("org.junit.jupiter:junit-jupiter")`, `testImplementation(assertj-core)`, `testRuntimeOnly("org.junit.platform:junit-platform-launcher")`; `tasks.test { useJUnitPlatform() }`.
  - `digitalbanking.spring-module`: aplica `digitalbanking.kotlin-library` + `kotlin("plugin.spring")`; `implementation(platform(spring-boot-bom))`, `testImplementation(platform(spring-boot-bom))`, `implementation("org.jetbrains.kotlin:kotlin-reflect")`.
  - `digitalbanking.spring-boot-app`: aplica `digitalbanking.spring-module` + `org.springframework.boot`.
- [ ] **Passo 5: `settings.gradle.kts`** com `rootProject.name = "digitalbanking"`, `dependencyResolutionManagement { repositories { mavenCentral() } }`, `include(":services:accounts:domain", ":services:accounts:application", ":services:accounts:adapters", ":services:accounts:bootstrap")`; `gradle.properties` com `org.gradle.caching=true`, `org.gradle.configuration-cache=true`, `org.gradle.java.installations.auto-download=false`.
- [ ] **Passo 6: Módulos.** `domain` e `application`: plugin `digitalbanking.kotlin-library`; `application` com `implementation(project(":services:accounts:domain"))`. `adapters`: `digitalbanking.spring-module`, depende de `application` e `domain`. `bootstrap`: `digitalbanking.spring-boot-app`, depende dos três.
- [ ] **Passo 7: Verificar o build vazio.** Rodar `./gradlew build`. Esperado: `BUILD SUCCESSFUL`.
- [ ] **Passo 8: Verificar que o ktlint quebra o build (foco 1).** Criar temporariamente `services/accounts/domain/src/main/kotlin/.../Tmp.kt` com `class Tmp{ val x=1 }`; `./gradlew build` → esperado `FAILED` em `ktlintMainSourceSetCheck`. Apagar o arquivo; `./gradlew build` → `BUILD SUCCESSFUL`.
- [ ] **Passo 9: `.gitignore`** com `.gradle/`, `build/`, `.kotlin/`, `.idea/`, `*.iml`, `.scratch/`, `infra/local/.env`. `.gitattributes` com `gradlew text eol=lf` e `*.bat text eol=crlf`.
- [ ] **Passo 10: Commit** `build: monorepo gradle com build-logic, catalogo e ktlint`.

### Task 2: Regras de arquitetura (ArchUnit)

**Arquivos:**
- Criar: `services/accounts/bootstrap/src/test/kotlin/io/github/capozziv/digitalbanking/accounts/bootstrap/ArchitectureTest.kt`
- Modificar: `services/accounts/bootstrap/build.gradle.kts` (`testImplementation(libs.archunit)`)

**Interfaces:**
- Consome: projetos da Task 1.
- Produz: `ArchitectureTest` com três testes, importando `io.github.capozziv.digitalbanking.accounts` por `ClassFileImporter().withImportOption(ImportOption.DoNotIncludeTests())`.

- [ ] **Passo 1: Escrever os testes.**
  - `domain nao depende de spring nem de outras camadas`: `noClasses().that().resideInAPackage("..accounts.domain..").should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "..accounts.application..", "..accounts.adapters..").allowEmptyShould(true)`.
  - `application nao depende de spring nem de adapters`: idem para `..accounts.application..` contra `org.springframework..` e `..accounts.adapters..`.
  - `adapters web e persistence sao independentes`: `noClasses()` em `..adapters.web..` não dependem de `..adapters.persistence..`, e o inverso, ambos com `allowEmptyShould(true)`.
- [ ] **Passo 2: Ver vermelho (foco 2).** Criar temporariamente `adapters/src/main/kotlin/.../adapters/persistence/TmpRepo.kt` (`class TmpRepo`) e `.../adapters/web/TmpController.kt` (`class TmpController(val repo: TmpRepo)`). Rodar `./gradlew :services:accounts:bootstrap:test --tests '*ArchitectureTest*'` → esperado FAIL no teste de adapters.
- [ ] **Passo 3: Ver verde.** Apagar os dois arquivos temporários; rodar o mesmo comando → PASS.
- [ ] **Passo 4: Verificar a barreira do Gradle (foco 3).** Criar temporariamente em `domain` um arquivo com `import org.springframework.stereotype.Component`; `./gradlew :services:accounts:domain:compileKotlin` → esperado FAIL com `Unresolved reference 'springframework'`. Apagar o arquivo.
- [ ] **Passo 5: `./gradlew build`** → `BUILD SUCCESSFUL`.
- [ ] **Passo 6: Commit** `test: regras de arquitetura com archunit`.

### Task 3: Aplicação `accounts` com health do banco (Testcontainers)

**Arquivos:**
- Criar: `services/accounts/bootstrap/src/main/kotlin/io/github/capozziv/digitalbanking/accounts/bootstrap/AccountsApplication.kt`
- Criar: `services/accounts/bootstrap/src/main/resources/application.yml`
- Criar: `services/accounts/adapters/src/main/resources/db/migration/.gitkeep`
- Criar: `services/accounts/bootstrap/src/test/kotlin/io/github/capozziv/digitalbanking/accounts/bootstrap/AccountsApplicationIT.kt`
- Modificar: `services/accounts/adapters/build.gradle.kts`, `services/accounts/bootstrap/build.gradle.kts`

**Interfaces:**
- Consome: plugins e catálogo da Task 1.
- Produz: `AccountsApplication` (`@SpringBootApplication(scanBasePackages = ["io.github.capozziv.digitalbanking.accounts"])`, `fun main(args: Array<String>)`); aplicação na porta `8081`; variáveis `ACCOUNTS_DB_URL`, `ACCOUNTS_DB_USER`, `ACCOUNTS_DB_PASSWORD` com padrões `jdbc:postgresql://localhost:5432/accounts`, `accounts`, `accounts`.

- [ ] **Passo 1: Dependências.** `adapters`: `implementation("org.springframework.boot:spring-boot-starter-webmvc")`, `spring-boot-starter-jdbc`, `spring-boot-starter-flyway`, `implementation(libs.flyway.postgresql)`, `runtimeOnly(libs.postgresql)`. `bootstrap`: `implementation("org.springframework.boot:spring-boot-starter-actuator")`; testes: `spring-boot-starter-test`, `spring-boot-testcontainers`, `libs.testcontainers.postgresql`, `libs.testcontainers.junit.jupiter`.
- [ ] **Passo 2: Escrever o teste** `AccountsApplicationIT`: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@Testcontainers`; `companion object { @Container @ServiceConnection @JvmStatic val postgres = PostgreSQLContainer("postgres:17-alpine") }`; `@LocalServerPort port`. Testes:
  - `health responde UP com o banco UP` (foco 4): `RestClient.create("http://localhost:$port").get().uri("/actuator/health").retrieve().body<Map<String, Any>>()`; asserts `status == "UP"` e `components.db.status == "UP"`.
  - `virtual threads ligadas`: `Environment.getProperty("spring.threads.virtual.enabled") == "true"`.
- [ ] **Passo 3: Ver vermelho.** `./gradlew :services:accounts:bootstrap:test --tests '*AccountsApplicationIT*'` → FAIL (classe de aplicação/configuração ausente).
- [ ] **Passo 4: Implementar** `AccountsApplication` e `application.yml`: `spring.application.name: accounts`, `server.port: 8081`, `spring.threads.virtual.enabled: true`, `spring.datasource.url/username/password` das variáveis acima com os padrões, `management.endpoints.web.exposure.include: health`, `management.endpoint.health.show-details: always`, `management.endpoint.health.show-components: always`.
- [ ] **Passo 5: Ver verde.** Mesmo comando → PASS. Depois `./gradlew build` → `BUILD SUCCESSFUL` (no ambiente de nuvem, com Docker).
- [ ] **Passo 6: Commit** `feat(accounts): aplicacao com health do banco e teste testcontainers`.

### Task 4: Ambiente local (docker-compose)

**Arquivos:**
- Criar: `infra/local/docker-compose.yml`, `infra/local/.env.example`, `infra/local/postgres/init/01-create-service-databases.sh`, `infra/local/nginx/nginx.conf`

**Interfaces:**
- Consome: variáveis e porta `8081` da Task 3.
- Produz: `docker compose --env-file infra/local/.env.example -f infra/local/docker-compose.yml up --wait`; nginx em `http://localhost:${NGINX_PORT:-80}`; `GET /healthz` do nginx responde 200.

- [ ] **Passo 1: `.env.example`** com `POSTGRES_PORT=5432`, `POSTGRES_PASSWORD=postgres`, `ACCOUNTS_DB_PASSWORD=accounts`, `REDIS_PORT=6379`, `NGINX_PORT=80` (valores só de desenvolvimento).
- [ ] **Passo 2: Script de init** (`set -euo pipefail`, `psql -v ON_ERROR_STOP=1`): cria role `accounts` com senha `$ACCOUNTS_DB_PASSWORD` e banco `accounts` com dono `accounts`. Um bloco por serviço; novos serviços acrescentam o seu.
- [ ] **Passo 3: `docker-compose.yml`** com `postgres` (`postgres:17-alpine`, init montado em `/docker-entrypoint-initdb.d`, healthcheck `pg_isready -U postgres`), `redis` (`redis:7-alpine`, healthcheck `redis-cli ping`), `nginx` (`nginx:alpine`, `nginx.conf` montado, `extra_hosts: ["host.docker.internal:host-gateway"]`, healthcheck `wget -qO- http://localhost/healthz`).
- [ ] **Passo 4: `nginx.conf`** com `location = /healthz { return 200 "ok"; }` e `location /accounts/ { proxy_pass http://host.docker.internal:8081/; }` (barra final remove o prefixo).
- [ ] **Passo 5: Verificar compose.** `docker compose --env-file infra/local/.env.example -f infra/local/docker-compose.yml up --wait` → todos `Healthy`.
- [ ] **Passo 6: Verificar a rota (foco 5).** Com o compose no ar, `./gradlew :services:accounts:bootRun` em segundo plano; `curl -fsS http://localhost/accounts/actuator/health` → JSON com `"status":"UP"`. Parar o `bootRun` e `docker compose ... down -v`.
- [ ] **Passo 7: Commit** `build: docker-compose local com postgres, redis e nginx`.

### Task 5: CI no GitHub Actions

**Arquivos:**
- Criar: `.github/workflows/ci.yml`

**Interfaces:**
- Consome: comandos das Tasks 1 a 4.

- [ ] **Passo 1: Workflow** com `on: push (branches: [main])` e `pull_request`; `concurrency: { group: ci-${{ github.ref }}, cancel-in-progress: true }`; `permissions: contents: read`.
  - Job `build` (`ubuntu-latest`): `actions/checkout@v5`, `actions/setup-java@v5` (`temurin`, `21`), `gradle/actions/setup-gradle@v5`, `./gradlew build`; `actions/upload-artifact@v5` com `**/build/reports/tests/` se `failure()`.
  - Job `smoke` (`needs: build`): checkout, setup-java, setup-gradle, `./gradlew :services:accounts:bootstrap:bootJar`, `docker compose --env-file infra/local/.env.example -f infra/local/docker-compose.yml up --wait`, `java -jar services/accounts/bootstrap/build/libs/*.jar &`, laço de até 60 s com `curl -fsS http://localhost/accounts/actuator/health`, conferir `"status":"UP"`, `docker compose ... down -v` em `if: always()`.
  - Antes de escrever, conferir as majors atuais de cada action no GitHub; usar a mais recente estável.
- [ ] **Passo 2: Validar sintaxe** com `actionlint` (binário da release no GitHub) → sem erros.
- [ ] **Passo 3: Commit** `ci: build e smoke no github actions`.
- [ ] **Passo 4: Pedir ao usuário o `git push`** e conferir os dois jobs verdes no GitHub (pela página do run ou `gh run list`, se houver credencial).

### Task 6: Fechamento

- [ ] **Passo 1:** `superpowers:verification-before-completion`: `./gradlew build` limpo na nuvem, compose saudável, resultado dos jobs do CI.
- [ ] **Passo 2:** `mattpocock-skills:code-review` da branch contra a spec.
- [ ] **Passo 3:** Atualizar `docs/status.md` (etapa 0 concluída, próxima etapa 1) e `docs/arquitetura.md` se algo mudou. Commit `docs: etapa 0 concluida`.
