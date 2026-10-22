# 0014 — Tipo de conta e cheque especial

- **Status:** aceito
- **Data:** 2026-10-22

## Contexto
O saldo pode ficar negativo ([ADR 0012](0012-saldo-no-lancamento-e-trava-pessimista.md)), então é preciso definir até onde. O limite varia por tipo de conta, e o banco simulado quer poder ajustá-lo sem novo deploy.

## Decisão
- O saldo pode ficar negativo **até o limite de cheque especial**, que vem do **tipo de conta**.
- Tipos: `FREE`, `STANDARD`, `GOLD`, `BLACK`, `DIAMOND`. Limites iniciais (em reais; no código, em centavos): Free 0, Standard 500, Gold 2.000, Black 10.000, Diamond 50.000.
- Os limites ficam numa **tabela no banco do `accounts`**, semeada por migration, com `updated_at`.
- **Alteração do limite** por endpoint administrativo `/admin/account-types/{type}`, aberto como o resto da API até a etapa 7, quando passa a exigir um grupo de administradores no Cognito. Validação: só limite ≥ 0; a ordem entre os tipos é convenção comercial, não regra do domínio.
- **Alteração concorrente com débito:** o débito lê o limite do tipo na própria transação, sem trava extra na tabela de tipos. Um débito em andamento usa o limite que leu; a alteração vale a partir do próximo débito. É comportamento esperado, não falha.
- **Conta nova** nasce `FREE`. A troca de tipo (upgrade ou downgrade) é por endpoint, sob a mesma trava `FOR UPDATE` da conta.
- **Downgrade ou redução de limite** é aceito mesmo com o saldo abaixo do novo limite: a conta continua negativa, novos débitos ficam bloqueados até o saldo voltar para dentro do limite, e créditos entram normalmente. O limite só é verificado no débito.
- Sem histórico de troca de tipo no MVP.

## Consequências
- Positivas: limites ajustáveis sem deploy nem reinício; regra do limite concentrada no tipo de conta; débitos de um mesmo tipo não disputam uma linha da tabela de tipos.
- Negativas: uma alteração de limite não afeta débitos já em andamento; contas podem ficar negativas além do novo limite após um downgrade; o endpoint `/admin` fica sem proteção até a etapa 7; sem histórico, não há como saber qual tipo a conta teve antes.

## Alternativas consideradas
- **Valores no código, junto ao enum:** descartado; alterar um limite exigiria deploy.
- **Configuração em `application.yml`:** descartada; alterar um limite exigiria reinício.
