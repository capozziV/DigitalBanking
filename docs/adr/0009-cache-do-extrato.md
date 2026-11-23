# 0009 — Cache do extrato

- **Status:** aceito
- **Data:** 2026-10-21 (revisado em 2026-11-12 e em 2026-11-20, bug #226)

## Contexto
A primeira página do extrato é a consulta mais comum. Um cache reduz leituras no DynamoDB, mas não pode servir dados velhos por muito tempo nem derrubar o serviço se falhar.

## Decisão
- **Cache-aside só no Redis**, sem Caffeine local na frente. Um cache por instância exigiria invalidação em todas as instâncias, e o ganho sobre o Redis é pequeno. O Caffeine fica para depois do MVP, se uma medição pedir.
- **Só a 1ª página com o `limit` padrão (20)** entra no cache; cursor ou outro `limit` vão direto ao DynamoDB.
- **TTL de 60 s com jitter de ±10 s** (entre 50 e 70 s).
- **Invalidação com versão** (bug #226): cada invalidação incrementa uma versão por conta no Redis (`statement:first-page-version:v1:{accountId}`, TTL de 1 dia) e apaga a página no mesmo script Lua, sem reescrever a página. A leitura que erra o cache lê a versão junto com a página e só grava a página do DynamoDB se a versão não mudou, conferida atomicamente em Lua; do contrário, descarta a gravação (`statement.cache.writes{result=stale}`). Assim, uma página lida antes de um lançamento chegar não volta ao cache depois da invalidação.
- **Fallback para o DynamoDB** se o Redis estiver lento ou fora: timeout de 100 ms, sem circuit breaker. Com o Redis fora, a leitura vem do DynamoDB e não grava no cache.
- Este cache não é usado para autorizar débitos (ver ADR 0007).

## Consequências
- Positivas: menos leituras no DynamoDB na consulta mais comum; jitter evita expiração em massa; indisponibilidade do Redis não derruba o extrato; uma só camada de cache, sem invalidação distribuída entre instâncias.
- Negativas: janela curta de dado desatualizado entre a gravação do lançamento e a remoção da chave; lógica extra de invalidação no consumidor; sem Caffeine, toda leitura em acerto ainda faz uma ida ao Redis.
- **Corrida corrigida (bug #226):** antes, uma leitura que errava o cache lia a página antiga no DynamoDB, o consumidor gravava o lançamento e apagava a chave, e a leitura então gravava a página velha no cache, que ficava até o TTL (~70 s). A versão elimina esse caso. A 1ª página segue lida no DynamoDB com leitura fortemente consistente.
- Negativa: custo de uma chave extra por conta e de scripts Lua na leitura e na invalidação.
- **Resíduos aceitos:**
  - Se a versão se perder (falta de memória ou reinício do Redis), uma página velha pode voltar ao cache; é raro e dura no máximo um TTL. O mesmo limite vale se o Redis falhar na invalidação.
  - As duas chaves não têm hash tag, pois o ElastiCache é de nó único. Num cluster, o script daria `CROSSSLOT` e o cache ficaria sem efeito, sem servir página velha.
- O extrato é leitura e o saldo de autorização nunca vem de cache.

## Alternativas consideradas
- **Caffeine local na frente do Redis:** descartada por ora (invalidação em todas as instâncias, ganho pequeno).
- **Reescrever a página no cache ao consumir:** descartada; remover a chave é mais simples e a próxima leitura repõe.
- **Circuit breaker no Redis:** descartado; o timeout de 100 ms limita o custo da falha. Reavaliar na etapa 8, se precisar.
