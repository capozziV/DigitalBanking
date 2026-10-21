# 0004 — API Gateway HTTP API na borda

- **Status:** aceito
- **Data:** 2026-10-21

## Contexto
É preciso uma borda que roteie por caminho para customer, accounts, transfers e statement e que autentique requisições com tokens do Cognito.

## Decisão
- **API Gateway HTTP API** como borda, no lugar de um gateway escrito em Spring.
- Roteamento por caminho e **autorizador JWT nativo do Cognito**.
- **VPC Link** até um ALB interno; chamadas entre serviços via ECS Service Connect.
- Os serviços **também** validam o JWT (Spring Security resource server); não confiam só na borda.
- **Localmente**, nginx faz o roteamento por caminho.

## Consequências
- Positivas: um serviço a menos para manter; autenticação na borda sem código próprio; defesa em profundidade com validação JWT nos serviços.
- Negativas: comportamento local (nginx) difere da AWS; Cognito no LocalStack é recurso pago, então o ambiente local não reproduz a autenticação real.

## Pontos em aberto
- Estratégia exata de autenticação no ambiente local (sem Cognito); o planejamento prevê um modo local com usuários/tokens de teste, sem detalhar como.

## Alternativas consideradas
- **Gateway em Spring:** descartado em favor do API Gateway HTTP API, que já entrega roteamento e autorizador JWT gerenciados.
