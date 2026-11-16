# 0020 — Identidade e autorização

- **Status:** aceito
- **Data:** 2026-11-13

## Contexto
Até a etapa 6 os serviços aceitam qualquer chamador e o `customerId` vem do corpo ou da query. A etapa 7 exige que cada cliente mexa só no que é dele, que o admin opere tudo e que serviços se chamem com identidade própria. O cliente passa a existir num serviço próprio, o `customer`, dono do cadastro e do status.

## Decisão
- **O id do cliente é o `sub` do usuário** (UUID). Um usuário tem no máximo um cliente. Todo serviço sabe quem é o cliente só pelo token.
- **Três chamadores**, resolvidos de um JWT validado: cliente (token de usuário sem o grupo `admin`), admin (token de usuário com o grupo `admin`) e serviço (token de máquina).
- **Token de máquina** por client credentials, com o escopo `digitalbanking/internal`, nas chamadas entre serviços e nas rotas `/internal/**`.
- **Recurso de outro cliente devolve `404`**, para não revelar que existe. Sem token ou token inválido: `401`. Rota de admin sem o grupo: `403`.
- **Consulta síncrona ao `customer`, sem cache**, ao abrir conta (`accounts`) e ao criar transferência (`transfers`), porque ele é a fonte da verdade do status (decisão do usuário). Cliente inexistente ou `BLOCKED`: `422`. `customer` fora, com erro ou timeout: `503`.
- **Admin pelo grupo** no token; o nome do claim é configurável (`groups` no mock, `cognito:groups` no Cognito).
- **Usuário de teste do dono do projeto:** um usuário fixo que é admin e cliente ao mesmo tempo. Opera as próprias contas como cliente e o resto como admin.

## Consequências
- Positivas: nenhuma consulta extra para saber quem é o cliente; bloqueio vale na hora; `domain` sem noção de token (o `application` recebe um `Caller` já resolvido); `404` não vaza a existência de recursos.
- Negativas: o `customer` vira dependência síncrona de abrir conta e de transferir (fora do ar, essas operações dão `503`; o timeout curto limita o impacto); o `sub` amarra o cliente ao provedor de identidade; a diferença entre mock e Cognito (claim de grupos, formato do escopo) só é exercitada na AWS na etapa 8.

## Alternativas consideradas
- **Cópia local do status do cliente por eventos:** evitaria a dependência síncrona, mas o bloqueio demoraria a valer (consistência eventual). Descartada pelo usuário em favor da fonte da verdade.
- **`customerId` em claim customizado:** exigiria gatilho no Cognito e um mapeamento a manter; usar o `sub` dispensa isso.
- **`403` em vez de `404` para recurso de outro cliente:** revelaria que o recurso existe.
