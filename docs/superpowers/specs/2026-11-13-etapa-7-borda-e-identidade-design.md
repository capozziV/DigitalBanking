# Etapa 7 — borda e identidade: design

- **Data:** 2026-11-13
- **Status:** aprovada pelo usuário em 2026-11-13
- **Roadmap:** etapa 7: `customer`, Cognito, API Gateway HTTP API (autorizador JWT + VPC Link), validação JWT nos serviços, correlation ID em `libs/observabilidade`, `/admin` com grupo de administradores, `/actuator/health` restrito, exceções de segurança e desconexão do cliente fora do handler de 500.
- **ADRs base:** [0004 — API Gateway HTTP API na borda](../../adr/0004-api-gateway-http-api-na-borda.md), [0019 — EKS](../../adr/0019-runtime-na-aws-com-eks.md).

## Objetivo
Toda requisição de cliente chega com um JWT, e cada serviço só deixa o cliente mexer no que é dele. O admin opera tudo pelo grupo `admin`. O cliente passa a existir num serviço próprio, o `customer`, dono do cadastro e do status. Na AWS, a borda autentica com o Cognito; localmente, o `mock-oauth2-server` emite os tokens e o nginx roteia.

## Decisões desta etapa (usuário, 2026-11-12/13)
1. **Escopo completo:** local (`mock-oauth2-server`, nginx) e Terraform do Cognito e do API Gateway escrito e validado, **sem aplicar**. O deploy fica na etapa 8, depois do #146.
2. **Dono dos dados:** o cliente só vê e opera as próprias contas, transferências, extrato, limite e notificações. O admin vê tudo.
3. **`customer` no MVP:** cadastro ligado ao usuário logado (nome e e-mail fictícios), consulta do próprio cadastro e status `ACTIVE`/`BLOCKED`, que o admin altera. PostgreSQL próprio e eventos pelo outbox.
4. **Cliente bloqueado:** o `transfers` **consulta o `customer` na hora**, a fonte da verdade, ao receber o pedido. Bloqueado → `422 customer-blocked`, sem criar a transferência; `customer` fora → `503`.
5. **Abertura de conta:** o cliente vem do token, não do corpo. O `accounts` consulta o `customer` na hora: não cadastrado ou bloqueado → `422`; `customer` fora → `503`.
6. **Admin:** grupo `admin` no token (claim `groups` no mock, `cognito:groups` na AWS; nome do claim configurável) protege `/admin/**`.

## Critério de pronto
- Build completo verde (ktlint, unidade, integração, ArchUnit, e2e).
- `terraform fmt -check` e `terraform validate` verdes no CI, sem `apply`.
- Smoke e Bruno das cinco coleções verdes, com tokens do `mock-oauth2-server`.
- Testes provam: sem token → `401`; recurso de outro cliente → `404`; rota de admin sem o grupo → `403`; admin passa; cliente bloqueado não transfere; serviço chamando serviço com token de máquina.
- Revisão de cada task e revisão final da branch pela Beatriz, com mutantes vermelhos.

## Restrições
- Clean Architecture: o `domain` não conhece token. O `application` recebe um `Caller` (cliente ou admin ou serviço) já resolvido pelo `adapters/web`.
- Código em inglês, termos do `CONTEXT.md`: `Customer` (cliente) e `User` (usuário de login).
- Eventos só pelo outbox; consumidores idempotentes.

## 1. Identidade e o id do cliente
- **O id do cliente é o `sub` do usuário** (UUID). Um usuário tem no máximo um cliente. Com isso, todo serviço sabe quem é o cliente só pelo token, sem consulta extra. O Cognito emite `sub` UUID; o mock é configurado para emitir UUID.
- O `customerId` some dos corpos e parâmetros em que hoje vem do chamador (`POST /accounts`, `GET /notifications?customerId=`).
- **Negar por padrão:** rota sem regra explícita responde `403`; cada serviço lista todas as suas rotas.
- **Três tipos de chamador** (`Caller`), resolvidos de um JWT validado (Spring Security resource server, emissor e JWKS configuráveis):
  - **cliente:** token de usuário sem o grupo `admin`; `customerId = sub`;
  - **admin:** token de usuário com o grupo `admin`;
  - **serviço:** token de máquina (client credentials) com o escopo `digitalbanking/internal`, sem `sub` UUID e sem grupos. Token de usuário com o escopo interno é rejeitado (`401`).

## 2. Serviço `customer`
- Porta **8085**, PostgreSQL próprio (banco `customer`, usuários `customer_owner`/`customer_app`), Clean Architecture como os outros e `libs/outbox` + `libs/outbox-relay` (tópico `customer-events.fifo`).
- **API:**
  - `POST /customers` (cliente): cadastra o cliente do token com `name` e `email`. Idempotente por `sub`: repetir devolve o mesmo cadastro (`200`); o primeiro devolve `201`.
  - `GET /customers/me` (cliente) → cadastro e status.
  - `GET /admin/customers/{id}` e `PUT /admin/customers/{id}/status` (admin), corpo `{ status: ACTIVE | BLOCKED }`.
  - `GET /internal/customers/{id}` (serviço) → `{ id, status }`; `404` se não existir.
- **Eventos:** `CustomerRegistered`, `CustomerBlocked`, `CustomerUnblocked` (nesta etapa, ninguém consome; ficam para avisos e para o frontend).
- `Customer` com `id`, `name`, `email`, `status`, `createdAt`, `updatedAt`. Dados fictícios.

## 3. Autorização nos serviços
| Serviço | Cliente | Admin | Serviço (máquina) |
|---|---|---|---|
| `accounts` | `POST /accounts` (abre para si; consulta o `customer`), `GET /accounts/{id}` e `/entries` da própria conta | tudo, inclusive `/admin/**`, créditos e débitos manuais | `GET /accounts/{id}`, débitos, créditos, busca de lançamento (saga) |
| `transfers` | `POST /transfers` com origem própria (consulta o `customer`), `GET /transfers/{id}` em que é a origem, `GET /accounts/{id}/daily-limit` da própria conta | tudo | — |
| `statement` | extrato da própria conta | tudo | — |
| `notification` | `GET /notifications` e `POST /notifications/read` do próprio cliente | tudo | — |
| `customer` | `POST /customers`, `GET /customers/me` | `/admin/**` | `/internal/**` |

- **De quem é a conta:**
  - `accounts` tem o `customerId` da conta;
  - `statement` já guarda o `customerId` pelo `AccountOpened`;
  - `transfers` grava `source_customer_id` no pedido, conferido na consulta da conta de origem ao `accounts`, que devolve o `customerId`;
  - o limite diário confere a conta pelo `accounts`.
- Recurso de outro cliente → **`404`** (não `403`), para não revelar que existe. Sem token ou token inválido → `401`. Rota de admin sem o grupo → `403`.
- **Chamadas entre serviços** (`transfers` → `accounts`, `transfers` → `customer`, `accounts` → `customer`) levam um token de máquina obtido por client credentials (cacheado até perto de expirar). Localmente, o `mock-oauth2-server` emite esse token; na AWS, um app client do Cognito com o escopo `digitalbanking/internal`.

## 4. Consulta ao `customer` no pedido
- `accounts` (`POST /accounts`) e `transfers` (`POST /transfers`) chamam `GET /internal/customers/{id}` com timeout curto (configurável, padrão 500 ms) e sem cache, por decisão do usuário: sempre a fonte da verdade.
- `404` → `422 customer-not-found`; `BLOCKED` → `422 customer-blocked`; erro, timeout ou `5xx` → `503 customers-unavailable`. Nada é criado nesses casos.
- Uma transferência que já passou da consulta segue até o fim, mesmo que o cliente seja bloqueado durante a saga, que dura milissegundos.

## 5. `libs/observabilidade`
- Junta os `CorrelationIdFilter` repetidos nos quatro serviços num só filtro: recebe `X-Correlation-Id` ou gera um, põe no MDC e devolve no cabeçalho.
- Propaga o correlation ID nas chamadas HTTP de saída (interceptor do `RestClient`).
- Os serviços apagam os próprios filtros e usam a lib. A propagação por eventos continua pelo `libs/outbox`.

## 6. Segurança transversal
- **Problem Details** para `401` (`/problems/unauthorized`) e `403` (`/problems/forbidden`), fora do handler genérico de `500`.
- Desconexão do cliente (`AsyncRequestNotUsableException`, `ClientAbortException`; uma `IOException` genérica não entra, para não mascarar erros reais) não vira `500` nem log de ERROR: vira DEBUG, sem resposta.
- `/actuator/health` aberto sem detalhes (`show-details: when-authorized`, só admin); `/actuator/metrics` e o resto do actuator só para admin.
- OpenAPI de cada serviço declara o esquema `bearerAuth`.

## 7. Ambiente local
- `mock-oauth2-server` no compose (porta `8090`), emissor `default`, com `sub` UUID e o claim `groups` configurados por requisição de token. Usuários de teste documentados: um cliente, outro cliente e um admin. Client credentials para os serviços.
- nginx: rota `/customers` e `/admin/customers` para o `customer`, as demais como hoje.
- **Usuário de teste do dono do projeto** (pedido do usuário, 2026-11-13): um usuário fixo, cliente e admin ao mesmo tempo, com `sub` fixo, nome fictício (`Owner Test`) e e-mail fictício. Ele tem cadastro no `customer`, duas contas e saldo inicial, criados por um script de seed local (`infra/local/seed.sh`) depois do compose. Serve para testar transferências entre as próprias contas e operar tudo como admin. Na AWS ele vira um usuário do Cognito no grupo `admin`, criado na etapa 8. Um admin que também é cliente opera as próprias contas como cliente, e o resto pelo papel de admin.
- `infra/local/smoke.sh` e as coleções Bruno pegam tokens no mock antes das chamadas. Coleção nova `api/bruno/customer`.
- Os testes de integração usam um emissor de JWT de teste da `libs/testing-security` (chave gerada no teste, `JwtDecoder` apontando para ela) e não dependem do mock. O e2e sobe o `mock-oauth2-server` em Testcontainers.

## 8. Terraform (sem aplicar)
- `infra/terraform/modules/identity`: User Pool do Cognito, grupo `admin`, app client do frontend (authorization code + PKCE), resource server `digitalbanking` com o escopo `internal` e app client de máquina (client credentials).
- `infra/terraform/modules/edge`: API Gateway HTTP API, autorizador JWT do Cognito, rotas por caminho para cada serviço, VPC Link para um listener de ALB interno recebido por variável (a rede e o EKS vêm na etapa 8).
- `infra/terraform/envs/aws`: compõe os módulos com variáveis, sem backend remoto ainda.
- CI: job `terraform` com `fmt -check`, `init -backend=false` e `validate`. Sem credenciais da AWS.

## 9. Testes
- **Unidade:** resolução do `Caller` a partir das claims; regras de dono em cada caso de uso; mapeamento das respostas do `customer`.
- **Integração (por serviço):** `401` sem token, `403` em `/admin` sem grupo, `404` em recurso de outro cliente, admin passa; serviço com token de máquina nas rotas internas; `customer` (cadastro idempotente, status, eventos no outbox); `accounts` e `transfers` com o `customer` simulado (`WireMock` ou servidor HTTP de teste): bloqueado, inexistente, fora do ar.
- **e2e:** cliente se cadastra, abre conta, recebe crédito do admin, transfere; outro cliente não vê o extrato; admin bloqueia o cliente e a próxima transferência dá `422 customer-blocked`.
- **Mutantes na revisão:** sem a checagem de dono, o teste do `404` fica vermelho; sem a consulta ao `customer`, o teste do bloqueado fica vermelho; aceitar token de cliente em rota interna deixa o teste do `403` vermelho.

## 10. Documentação
- ADR novo: **identidade e autorização** (id do cliente = `sub`, três chamadores, token de máquina, `404` para recurso de outro cliente, consulta síncrona ao `customer`).
- ADR 0004 atualizado com o Terraform da borda e o `mock-oauth2-server` no compose.
- `CONTEXT.md`: status do cliente (`CustomerStatus`, `ACTIVE`/`BLOCKED`) e "Administrador".
- `docs/arquitetura.md`, `docs/rodando-localmente.md` (tokens, usuários de teste) e `docs/roadmap.md`.

## Fora de escopo
- Tela de login e fluxo do frontend: etapa 9.
- `apply` do Terraform, VPC, EKS e ALB: etapa 8.
- Consumo dos eventos do `customer`: depois (avisos, frontend).
- Recuperação de senha, MFA, cadastro com documento real: não previstos (banco simulado).

## Riscos
- **`customer` vira dependência síncrona** de abrir conta e de transferir: fora do ar, essas operações dão `503`. É escolha do usuário (fonte da verdade sempre); o timeout curto limita o impacto.
- **Escopo grande:** o plano divide em tasks por serviço, que rodam em paralelo depois da base comum (lib de segurança e observabilidade).
- **Diferença entre o mock e o Cognito:** o nome do claim de grupos e o formato do escopo são configuráveis; o Terraform é validado mas não exercitado até a etapa 8.
