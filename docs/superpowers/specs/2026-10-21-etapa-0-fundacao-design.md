# Etapa 0 — Fundação: design

- **Data:** 2026-10-21
- **Status:** aguardando revisão do usuário
- **Roadmap:** etapa 0 de [`docs/roadmap.md`](../../roadmap.md)

## Objetivo
Criar o molde que os cinco microsserviços do monorepo vão seguir e provar que ele funciona com um serviço real (`accounts`) ainda sem regra de negócio. A etapa 1 começa direto no domínio.

## Critério de pronto
1. `./gradlew build` verde no ambiente de nuvem do Claude (compilação, ktlint, testes, ArchUnit, Testcontainers).
2. Jobs `build` e `smoke` do CI verdes no GitHub Actions.
3. `docker compose -f infra/local/docker-compose.yml up --wait` termina com todos os contêineres saudáveis.

## Restrições (de `CLAUDE.md` e ADRs)
- Kotlin, Spring Boot 4.1, JDK 21, Spring MVC com virtual threads ([ADR 0001](../../adr/0001-stack-kotlin-spring-boot-aws.md)).
- Clean Architecture por serviço, `domain` sem Spring, regras validadas por ArchUnit ([ADR 0002](../../adr/0002-clean-architecture-por-servico.md)).
- Lint com ktlint. Commits só com o autor do usuário.

## 1. Estrutura do monorepo
```
DigitalBanking/
├── settings.gradle.kts          # inclui build-logic e os módulos
├── gradle/
│   ├── libs.versions.toml       # versões únicas do repo
│   └── wrapper/
├── build-logic/                 # included build com plugins de convenção
├── services/
│   └── accounts/
│       ├── domain/              # Kotlin puro
│       ├── application/         # casos de uso e portas, sem Spring
│       ├── adapters/            # web e persistência (Spring)
│       └── bootstrap/           # @SpringBootApplication, configuração, jar executável
├── infra/local/                 # docker-compose, nginx, init do PostgreSQL, .env.example
├── .github/workflows/ci.yml
└── docs/
```
- **Um módulo Gradle por camada.** `domain` e `application` não têm Spring no classpath: violação não compila. ArchUnit cobre as regras que o Gradle não expressa.
- **Dependências:** `application` → `domain`; `adapters` → `application`, `domain`; `bootstrap` → os três. Só o `bootstrap` gera jar executável.
- **Pacote raiz:** `io.github.capozziv.digitalbanking.accounts` com subpacotes `domain`, `application`, `adapters`, `bootstrap`.
- **Caminhos Gradle:** `:services:accounts:domain`, `:services:accounts:application`, `:services:accounts:adapters`, `:services:accounts:bootstrap`.
- **Cada serviço é independente:** próprio banco, própria aplicação, comunicação com outros só por HTTP ou eventos. Nada de domínio compartilhado.

## 2. Build e convenções
- **Versões** em `gradle/libs.versions.toml`: Kotlin 2.x, Spring Boot 4.1, Gradle 9, ktlint 1.8, ArchUnit, Testcontainers, Flyway, driver PostgreSQL. Números exatos conferidos em fonte primária no plano.
- **Dependências do Spring** pelo BOM do Spring Boot como `platform(...)` (sem o plugin `io.spring.dependency-management`).
- **Plugins de convenção em `build-logic`:**

| Plugin | Usado por | Aplica |
|---|---|---|
| `digitalbanking.kotlin-library` | `domain`, `application` | Kotlin JVM, toolchain 21, JUnit 5 + AssertJ, ktlint, warnings como erro. Sem Spring. |
| `digitalbanking.spring-module` | `adapters` | `kotlin-library` + plugin `kotlin-spring` + BOM do Spring Boot |
| `digitalbanking.spring-boot-app` | `bootstrap` | `spring-module` + plugin Spring Boot |

- **ktlint** faz parte de `./gradlew check` e quebra o build; `./gradlew ktlintFormat` corrige.
- **Build cache e configuration cache** ligados em `gradle.properties` desde o início.

## 3. Esqueleto do `accounts`
- `domain` e `application`: sem classes. Nada de domínio inventado; `Conta` e `Lançamento` nascem com TDD na etapa 1.
- `adapters`: dependências de Spring Web, JDBC do PostgreSQL e Flyway; `src/main/resources/db/migration/` vazio.
- `bootstrap`: `AccountsApplication`, `application.yml` com `spring.threads.virtual.enabled: true`, Actuator expondo `/actuator/health` com detalhe do banco.

### Testes (no `bootstrap`, que enxerga todos os módulos)
1. **ArchUnit**
   - `..domain..` não depende de `org.springframework..`, `..application..` nem `..adapters..`.
   - `..application..` não depende de `org.springframework..` nem `..adapters..`.
   - `..adapters.web..` e `..adapters.persistence..` não dependem um do outro.
   - Regras com `allowEmptyShould(true)` enquanto não houver classes; a permissão sai na etapa 1.
2. **Integração com Testcontainers:** `@SpringBootTest` com PostgreSQL 17 via `@ServiceConnection`; `GET /actuator/health` responde `UP` incluindo o componente `db`.

## 4. Ambiente local
`infra/local/docker-compose.yml`:

| Contêiner | Imagem | Papel |
|---|---|---|
| `postgres` | `postgres:17-alpine` | Uma instância; script de init cria um banco e um usuário por serviço (na etapa 0, só `accounts`). |
| `redis` | `redis:7-alpine` | Usado a partir das etapas 5 e 6. |
| `nginx` | `nginx:alpine` | Papel do API Gateway: `/accounts/` → `accounts` em `host.docker.internal` (com `extra_hosts: host-gateway` para Linux). |

- Todos com healthcheck; `docker compose up --wait` é o critério de saúde.
- `accounts` roda fora do compose: `./gradlew :services:accounts:bootRun`.
- Portas e credenciais de desenvolvimento em `infra/local/.env.example` (versionado); `.env` no `.gitignore`.
- Ambiente de nuvem do Claude: `dockerd` com `registry-mirrors: ["https://mirror.gcr.io"]` para evitar o limite anônimo do Docker Hub (configuração do ambiente, não do repo).

## 5. CI (GitHub Actions)
`.github/workflows/ci.yml`, em push na `main` e em pull request, com `concurrency` cancelando o run anterior do mesmo branch.

- **Job `build`:** checkout → `actions/setup-java` (Temurin 21) → `gradle/actions/setup-gradle` → `./gradlew build`. Relatórios de teste anexados se falhar.
- **Job `smoke`** (após `build`): `docker compose up --wait` → `accounts` (jar do `bootstrap`) em segundo plano apontando para o PostgreSQL do compose → `curl http://localhost/accounts/actuator/health` via nginx esperando `UP` → `docker compose down`.

## Fora de escopo (e onde foi registrado)
| Item | Quando | Registrado em |
|---|---|---|
| Biblioteca de persistência (Spring JDBC, JPA ou jOOQ) | decidir antes da etapa 1 | `arquitetura.md` → Em aberto |
| Mecanismo de concorrência no saldo | decidir antes da etapa 1 | `arquitetura.md` → Em aberto |
| Tirar `allowEmptyShould` das regras ArchUnit | etapa 1 | `roadmap.md` |
| Build seletivo por serviço no CI | etapa 2 | `roadmap.md` |
| Emulador AWS local: LocalStack (exige conta e token desde 2026-03-23) ou alternativa aberta | decidir antes da etapa 3 | `arquitetura.md` → Em aberto; `roadmap.md` |
| `libs/outbox` | etapa 3 | `roadmap.md` |
| `libs/observabilidade` (correlation ID, logs JSON) | etapa 7 | `roadmap.md` |
| `terraform/`, imagem Docker por serviço, deploy no CI | etapa 8 | `roadmap.md` |
| `frontend/` | etapa 9 | `roadmap.md` |
| Biblioteca de mock (ex.: MockK) | quando um teste precisar | esta spec |
| Proteção da `main` no GitHub | com o usuário, após o CI existir | `status.md` |

## Riscos
- **Spring Boot 4.1 é recente:** material de estudo ainda mira a linha 3; mudanças como Jackson 3 podem surpreender. Mitigação: conferir a documentação oficial da 4.1 no plano.
- **Limite do Docker Hub** no ambiente de nuvem: mitigado pelo espelho `mirror.gcr.io`.
