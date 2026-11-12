# 0004 — API Gateway HTTP API na borda

- **Status:** aceito
- **Data:** 2026-10-21

## Contexto
É preciso uma borda que roteie por caminho para customer, accounts, transfers e statement e que autentique requisições com tokens do Cognito.

## Decisão
- **API Gateway HTTP API** como borda, no lugar de um gateway escrito em Spring.
- Roteamento por caminho e **autorizador JWT nativo do Cognito**.
- **VPC Link** até um ALB interno; chamadas entre serviços pelo DNS de Service do Kubernetes ([ADR 0019](0019-runtime-na-aws-com-eks.md)).
- Os serviços **também** validam o JWT (Spring Security resource server); não confiam só na borda.
- **Localmente**, nginx faz o roteamento por caminho.
- **Autenticação local** (decidido em 2026-11-12): `mock-oauth2-server` no docker-compose emite JWT com JWKS, e os serviços validam o token do mesmo jeito que validam o do Cognito na AWS (só muda o emissor configurado).

## Consequências
- Positivas: um serviço a menos para manter; autenticação na borda sem código próprio; defesa em profundidade com validação JWT nos serviços.
- Negativas: comportamento local (nginx) difere da AWS; Cognito no LocalStack é recurso pago, então o ambiente local usa o `mock-oauth2-server` e não reproduz o Cognito em si, só a validação do JWT.

## Alternativas consideradas
- **Gateway em Spring:** descartado em favor do API Gateway HTTP API, que já entrega roteamento e autorizador JWT gerenciados.
