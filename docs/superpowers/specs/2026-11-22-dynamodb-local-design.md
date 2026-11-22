# DynamoDB Local com volume (design curto)

**Data:** 2026-11-22
**Status:** execução liberada pelo usuário em 2026-11-22 ("vamos utilizar o DynamoDB local [...] pode fazer essa alteração")
**Cartão:** [#234](https://github.com/capozziV/DigitalBanking/issues/234), em PR próprio depois do merge da etapa 9

## Intenção
A tabela do extrato não pode se perder quando um contêiner local é recriado. Em 2026-11-20 um `compose down` apagou o extrato: o LocalStack do plano Hobby não tem persistência.

## Abordagens
1. **DynamoDB Local da AWS, com volume nomeado (escolhida pelo usuário).** Grátis e em disco. Custo: um contêiner e um endpoint a mais.
2. Persistência do LocalStack: exige plano pago ou de estudante, e grava por snapshot periódico.
3. DynamoDB real no desenvolvimento: exige conta, credencial e internet de quem clonar.

## Desenho
- Serviço `dynamodb` no compose (versão fixa, `-sharedDb -dbPath /data`, volume nomeado `dynamodb-data`), com healthcheck em `127.0.0.1` e porta configurável.
- A tabela `statement` é criada de forma idempotente nesse serviço, e o `statement` só sobe depois dela. O LocalStack fica só com SNS e SQS.
- O `statement` tem um endpoint do DynamoDB separado do de SQS. Vazio segue sendo a AWS real: nada muda no deploy.
- A conferência Terraform x scripts continua cobrindo a tabela.
- Prova automática: grava, derruba sem apagar volumes, sobe e o extrato continua igual.
- Stack existente: copiar os itens do LocalStack para o banco novo, com contagem e comparação, antes de trocar o `statement`. Procedimento idempotente no guia.

## Fora do escopo
- Mensagens em trânsito ou em DLQ no LocalStack, que seguem sem persistência (resíduo aceito e documentado).
- Proteções da AWS (retenção, tentativas, alarme, backup): aguardam decisão do usuário.
- Reconstrução pelo ledger (#117).
