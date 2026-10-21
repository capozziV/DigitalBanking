# Banco digital

Linguagem do domínio do banco digital simulado. Use estes termos em código, testes, eventos, ADRs e conversas. Glossário apenas: decisões técnicas ficam em `docs/arquitetura.md` e nos ADRs.

## Clientes e contas

**Cliente**:
Pessoa cadastrada no banco, com um status que indica se pode operar.
_Evite_: usuário (identidade de login), correntista, titular

**Usuário**:
Identidade de login usada para autenticar um cliente.
_Evite_: cliente, conta

**Conta**:
Conta de um cliente que guarda valores e possui um saldo.
_Evite_: carteira, wallet

## Ledger

**Lançamento**:
Registro imutável de um crédito ou débito numa conta.
_Evite_: transação, movimentação, entrada

**Crédito**:
Lançamento que aumenta o saldo da conta.

**Débito**:
Lançamento que diminui o saldo da conta.

**Ledger**:
Sequência imutável de lançamentos de uma conta; a fonte da verdade do saldo.
_Evite_: histórico, livro

**Saldo**:
Valor disponível numa conta, resultado dos seus lançamentos.
_Evite_: balanço

## Transferências

**Transferência**:
Envio de um valor de uma conta de origem para uma conta de destino.
_Evite_: pagamento, PIX, TED, transação

**Saga de transferência**:
Sequência de etapas que conclui uma transferência ou a desfaz por compensação.
_Evite_: fluxo, workflow

**Compensação**:
Etapa da saga que desfaz o efeito de uma etapa anterior quando a transferência não pode ser concluída.
_Evite_: rollback, estorno

**Limite diário**:
Valor máximo que uma conta pode transferir num mesmo dia.

**Reserva de limite**:
Parte do limite diário separada para uma transferência em andamento; consumida quando ela conclui e liberada na compensação.
_Evite_: bloqueio, hold

## Consulta e avisos

**Extrato**:
Lista dos lançamentos de uma conta em ordem cronológica, para consulta do cliente.
_Evite_: histórico, statement (nome do serviço, não do conceito)

**Notificação**:
Aviso enviado ao cliente sobre algo que aconteceu na conta.
_Evite_: alerta, mensagem
