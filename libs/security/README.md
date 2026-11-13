# libs/security

Segurança comum dos serviços: resource server JWT, o `Caller`, as respostas `401`/`403` em Problem Details, o token de máquina entre serviços e o tratamento de cliente desconectado. Tudo entra por autoconfiguração do Spring Boot.

## Módulos
- `:libs:security:caller`: só o tipo `Caller`, sem Spring. O `application` de cada serviço depende **só** dele.
- `:libs:security`: o resto (depende do `caller` com `api`). Entra no `adapters` do serviço.
- `:libs:testing-security`: `JwtTestSupport` para os testes de integração (ver abaixo).

## O que a lib faz sozinha
- Valida o JWT com o `JwtDecoder` que o Spring Boot monta a partir de `spring.security.oauth2.resourceserver.jwt.issuer-uri`.
- Resolve o `Caller` (`CallerResolver`):
  - escopo `digitalbanking/internal` (em `scope` ou `scp`) → `Caller.Service`;
  - grupo `admin` no claim de grupos → `Caller.Admin(sub)`;
  - qualquer outro → `Caller.Customer(sub)`;
  - `sub` que não é UUID num token de usuário → `401`.
- Cada chamador recebe um papel: `CUSTOMER`, `ADMIN` ou `SERVICE` (`CallerRoles`).
- Regras fixas, nesta ordem e antes das do serviço:
  - abertos: `/actuator/health/**`, `/v3/api-docs/**`, `/swagger-ui/**` e `/error`;
  - `/admin/**` só `ADMIN`;
  - `/internal/**` só `SERVICE`;
  - o resto de `/actuator/**` só `ADMIN`.
- Depois das regras do serviço, **tudo o mais é negado**: com token, `403`; sem token, `401`. Rota nova sem regra nunca fica aberta por acidente.
- Respostas `401` (`/problems/unauthorized`) e `403` (`/problems/forbidden`) em `application/problem+json`, inclusive quando a exceção de segurança sai de um controller.
- Cliente que desconectou no meio da resposta (`AsyncRequestNotUsableException`, `ClientAbortException`) vira log DEBUG, sem `500` e sem ERROR.
- Sem sessão e sem CSRF (só bearer token).

## Configuração
```yaml
spring.security.oauth2.resourceserver.jwt.issuer-uri: ${ACCOUNTS_JWT_ISSUER_URI:http://localhost:8090/default}
digitalbanking.security:
  groups-claim: groups            # cognito:groups na AWS
  service-token:                  # só em quem chama outro serviço
    token-uri: ${ACCOUNTS_OAUTH_TOKEN_URI:http://localhost:8090/default/token}
    client-id: ${ACCOUNTS_OAUTH_CLIENT_ID:accounts}
    client-secret: ${ACCOUNTS_OAUTH_CLIENT_SECRET:accounts-secret}
    # scope: digitalbanking/internal (padrão); timeout: 2s (padrão)
management.endpoint.health:
  show-details: when-authorized
  roles: ADMIN
```
Sem `service-token.token-uri`, não há `ServiceTokenClient`. Com ele, `client-id` e `client-secret` são obrigatórios: a aplicação não sobe sem eles.

## Uso
Regras de rota do serviço, num bean (o bloco é o DSL Kotlin do `authorizeHttpRequests`):
```kotlin
@Bean
fun accountsSecurityRules() =
    SecurityRules {
        authorize(HttpMethod.POST, "/accounts", hasAnyRole(CallerRoles.CUSTOMER, CallerRoles.ADMIN))
        authorize(HttpMethod.POST, "/accounts/{id}/debits", hasAnyRole(CallerRoles.SERVICE, CallerRoles.ADMIN))
    }
```
As regras dizem só **que tipo** de chamador entra. Se o recurso é do cliente, quem decide é o caso de uso, com `caller.actsAsCustomer(customerId)`, e o recurso alheio responde `404`.

No controller, `currentCaller()` devolve o `Caller` da requisição e o caso de uso o recebe.

Chamada a outro serviço com token de máquina e correlation ID:
```kotlin
RestClient.builder()
    .requestInterceptor(CorrelationIdRestClientInterceptor())
    .requestInterceptor(serviceTokenClient.interceptor())
```
Sem token (emissor fora, timeout, erro), a chamada falha com `ResourceAccessException`, como um par inalcançável. O gateway do serviço já responde a isso com `503` ou nova tentativa.

Nos testes de integração, `@Import(JwtTestSupport.DecoderConfiguration::class)` troca o decoder por um que confia na chave do teste. Os tokens saem de `JwtTestSupport.customerToken(id)`, `adminToken(id)`, `serviceToken()` ou `token(sub, groups, scopes, expiresIn, issuer)`.
