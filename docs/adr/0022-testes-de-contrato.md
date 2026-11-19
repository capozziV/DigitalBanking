# 0022 — Testes de contrato com Stubborn Contract

- **Status:** aceito
- **Data:** 2026-11-18

## Contexto
Os serviços se falam por HTTP (`transfers` → `accounts`, `accounts` → `customer`, `transfers` → `customer` e `accounts` → `transfers`, este para checar transferências pendentes no encerramento de conta, [ADR 0023](0023-status-da-conta-e-encerramento.md)) e por eventos do outbox (`accounts` → `statement`/`notification`, `transfers` → `notification`). Stubs escritos à mão no consumidor não provam que o produtor ainda responde assim: o produtor muda um campo e só o teste ponta a ponta percebe. A etapa 8 pede que mudar o contrato quebre o build do produtor.

O Spring Cloud Contract, a escolha inicial, passou para o Stubborn.sh e não recebe mais atualizações (blog da Spring, 2026-07-06). A última versão, 5.0.3, foi feita sobre o Boot 4.0.8, e os BOMs do Spring Cloud 2025.1.3 e 2025.1.4 já não trazem o contract. O start.spring.io declara só o `stubborn-contract` 0.2.2 para o Boot >=4.0.0 <4.2.0-M1; o projeto usa o Boot 4.1.1. Levantamento completo no relatório da Task 7 da etapa 8.

## Decisão
- Contratos HTTP entre `transfers`, `accounts` e `customer`, incluindo o de transferências pendentes (produtor `transfers`, consumidor `accounts`), e contratos de mensagem dos eventos do outbox, versionados no repositório.
- O produtor gera os testes a partir dos contratos e publica o jar de stubs; o consumidor roda os testes de integração contra esses stubs, no lugar de stubs escritos à mão.
- A ferramenta é o **Stubborn Contract 0.2.2** (`sh.stubborn:stubborn-contract`), sucessor oficial do Spring Cloud Contract.
- A configuração da ferramenta fica nas convenções do `build-logic` (`digitalbanking.contract-producer` e `digitalbanking.contract-consumer`), não nos módulos dos serviços.
- Coordenadas: plugin `sh.stubborn.contract` (`stubborn-contract-gradle-plugin` 0.2.2) e BOM `stubborn-contract-dependencies`; o produtor usa `stubborn-contract-starter-verifier` e o consumidor, `stubborn-contract-stub-runner`.
- Contratos em **Groovy DSL**: só ele permite `execute()` para gerar o token de máquina assinado na hora; o Kotlin DSL (`stubborn-contract-spec-kotlin`) puxa o Kotlin 2.4.20, que conflita com o 2.3.21 do projeto.
- Stubs locais com `StubsMode.CLASSPATH`, pela configuração Gradle entre projetos, sem repositório de artefatos.
- A base do produtor é um `@WebMvcTest` com os casos de uso mockados. Entram a serialização e a segurança reais; ficam de fora o `JwtDecoder` real e a semântica dos casos de uso, que os testes de integração (ex.: os 152 do `accounts`) cobrem.
- `wiremock-standalone` removido do `transfers`: conflita com o Stub Runner (`ClassCastException` no Handlebars). O WireMock agora chega só como dependência transitiva do Stub Runner.
- No CI, o `paths-filter` faz o contrato HTTP do `accounts` disparar o `transfers`, e o contrato do `customer` disparar o `accounts` e o `transfers`. Os contratos de mensagem do `accounts` disparam `statement` e `notification`; os do `transfers`, `notification`.
- Contratos de mensagem dos eventos do outbox (7 do `accounts`, incluindo os três eventos de status da conta, e 4 do `transfers`, que passa a ser produtor de mensagem). O Stubborn 0.2.2 oferece `MessageVerifierReceiver` e `MessageVerifierSender` para plugar um verificador, mas os prontos cobrem só JMS, Kafka, Rabbit, Camel, Spring Integration e Spring Cloud Stream; não há SNS/SQS.
- Produtor: lib `libs/testing-outbox-contract`, só de infraestrutura. O `OutboxMessageVerifier` grava pelo mapeador do outbox do serviço num PostgreSQL de contêiner, lê pela consulta do relay e devolve o envelope em JSON, o mesmo corpo que o `SnsEventPublisher` envia. O `sentTo` é o nome do tópico tirado do ARN configurado, então o contrato confere também o tópico.
- O projeto declara o bean `contractVerifierMessaging` inteiro: o bean no-op padrão monta receivers próprios e ignora o receiver declarado.
- Consumidor: `statement` e `notification` usam `StubRunnerExtension.messageVerifierSender(...)` com `trigger(label)` e entregam a mensagem direto ao tratador do listener, sem SQS.
- Fora do contrato de mensagem: atributos de mensagem do SNS e eventos do `customer`. O `statement` não tem contrato de consumo dos eventos de status da conta: o leitor ignora tipos desconhecidos.

## Consequências
- Positivas: contrato quebrado no produtor derruba o build do produtor; os stubs do consumidor deixam de divergir do produtor; ferramenta mantida e declarada compatível com o Boot do projeto.
- Negativas: ferramenta nova e de versão 0.x, com risco de mudança de API entre versões; menos material de apoio que o Spring Cloud Contract. Se vier outra ferramenta, a troca fica isolada nas convenções do `build-logic` e nos arquivos de contrato.
- Positiva: o contrato HTTP roda no `check` sem Docker, porque a base é `@WebMvcTest`.
- Negativa: o `contractTest` de `accounts` e `transfers` exige Docker, por causa do PostgreSQL de contêiner dos contratos de mensagem. O `./gradlew test` segue sem Docker.
- Negativa: declarar o bean `contractVerifierMessaging` inteiro acopla o projeto ao funcionamento interno do Stubborn; uma atualização pode mudar a autoconfiguração no-op ou a assinatura do bean e quebrar os contratos de mensagem. Reconferir ao subir a versão.
- Negativa: o contrato não prova o `JwtDecoder` real nem a semântica dos casos de uso (mockados); isso fica com os testes de integração.
- Negativa: falhas de rede (timeouts, corpos estranhos, conexão cortada) não cabem em stub de contrato e continuam com WireMock escrito à mão nos testes do `transfers`.
- Negativa: contratos em Groovy DSL são uma segunda linguagem no repositório, e a troca de versão do Kotlin não os afeta nem os valida.
- Negativa: o `paths-filter` precisa listar à mão quem consome cada contrato; consumidor novo exige atualizar o CI.

## Alternativas consideradas
- **Spring Cloud Contract 5.0.3:** fora do suporte no Boot 4.1.1 e sem novas versões; pode quebrar sem aviso.
- **Sem testes de contrato na etapa:** perde a prova de que mudar o produtor quebra o build do produtor.
- **Pact:** pede broker ou arranjo próprio no monorepo (descartado na spec da etapa 8).
