# 0009 — Cache do extrato

- **Status:** aceito
- **Data:** 2026-10-21 (revisado em 2026-11-12)

## Contexto
A primeira página do extrato é a consulta mais comum. Um cache reduz leituras no DynamoDB, mas não pode servir dados velhos por muito tempo nem derrubar o serviço se falhar.

## Decisão
- **Cache-aside só no Redis**, sem Caffeine local na frente. Um cache por instância exigiria invalidação em todas as instâncias, e o ganho sobre o Redis é pequeno. O Caffeine fica para depois do MVP, se uma medição pedir.
- **Só a 1ª página com o `limit` padrão (20)** entra no cache; cursor ou outro `limit` vão direto ao DynamoDB.
- **TTL de 60 s com jitter de ±10 s** (entre 50 e 70 s).
- **Invalidação por remoção da chave** ao consumir o lançamento, sem reescrever a página.
- **Fallback para o DynamoDB** se o Redis estiver lento ou fora: timeout de 100 ms, sem circuit breaker.
- Este cache não é usado para autorizar débitos (ver ADR 0007).

## Consequências
- Positivas: menos leituras no DynamoDB na consulta mais comum; jitter evita expiração em massa; indisponibilidade do Redis não derruba o extrato; uma só camada de cache, sem invalidação distribuída entre instâncias.
- Negativas: janela curta de dado desatualizado entre a gravação do lançamento e a remoção da chave; lógica extra de invalidação no consumidor; sem Caffeine, toda leitura em acerto ainda faz uma ida ao Redis.
- **Corrida aceita:** uma leitura que erra o cache pode ler a página antiga no DynamoDB e gravá-la no cache depois da remoção feita pelo consumidor. Essa página velha dura no máximo um TTL (até 70 s). O mesmo limite vale se o Redis falhar na remoção. A 1ª página é lida no DynamoDB com leitura fortemente consistente, para que uma leitura feita depois da remoção sempre veja o lançamento novo. É aceito porque o extrato é leitura e o saldo de autorização nunca vem de cache.

## Alternativas consideradas
- **Caffeine local na frente do Redis:** descartada por ora (invalidação em todas as instâncias, ganho pequeno).
- **Reescrever a página no cache ao consumir:** descartada; remover a chave é mais simples e a próxima leitura repõe.
- **Circuit breaker no Redis:** descartado; o timeout de 100 ms limita o custo da falha. Reavaliar na etapa 8, se precisar.
