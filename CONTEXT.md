# Banco digital

Linguagem do domínio do banco digital simulado. Use o termo em português em docs, ADRs e conversas, e o nome de código (em inglês) no código, testes e eventos. Glossário apenas: decisões técnicas ficam em `docs/arquitetura.md` e nos ADRs.

## Clientes e contas

**Cliente**:
Pessoa cadastrada no banco, com um status que indica se pode operar.
_Código_: `Customer`
_Evite_: usuário (identidade de login), correntista, titular

**Usuário**:
Identidade de login usada para autenticar um cliente.
_Código_: `User`
_Evite_: cliente, conta

**Conta**:
Conta de um cliente que guarda valores e possui um saldo.
_Código_: `Account`
_Evite_: carteira, wallet

**Tipo de conta**:
Categoria da conta que define o limite de cheque especial; valores Free, Standard, Gold, Black e Diamond.
_Código_: `AccountType`
_Evite_: plano, categoria, nível

## Ledger

**Lançamento**:
Registro imutável de um crédito ou débito numa conta, com o saldo resultante.
_Código_: `LedgerEntry`
_Evite_: transação, movimentação, entrada

**Valor**:
Quantia positiva, em centavos, de um lançamento.
_Código_: `Money`
_Evite_: quantia, montante

**Tipo de lançamento**:
Indica se o lançamento é um crédito ou um débito; valores `CREDIT` e `DEBIT`.
_Código_: `EntryKind`

**Crédito**:
Lançamento que aumenta o saldo da conta.
_Código_: `Credit`

**Débito**:
Lançamento que diminui o saldo da conta.
_Código_: `Debit`

**Ledger**:
Sequência imutável de lançamentos de uma conta; a fonte da verdade do saldo.
_Código_: `Ledger`
_Evite_: histórico, livro

**Saldo**:
Resultado dos lançamentos da conta; pode ser negativo.
_Código_: `Balance`
_Evite_: balanço

**Limite de cheque especial**:
Quanto o saldo pode ficar abaixo de zero; definido pelo tipo de conta.
_Código_: `OverdraftLimit`
_Evite_: crédito, limite (sem qualificador, para não confundir com limite diário)

**Saldo disponível**:
Saldo somado ao limite de cheque especial; o máximo que um débito pode tirar agora.
_Código_: `AvailableBalance`

## Transferências

**Transferência**:
Envio de um valor de uma conta de origem para uma conta de destino.
_Código_: `Transfer`
_Evite_: pagamento, PIX, TED, transação

**Saga de transferência**:
Sequência de etapas que conclui uma transferência ou a desfaz por compensação.
_Código_: `TransferSaga`
_Evite_: fluxo, workflow

**Compensação**:
Etapa da saga que desfaz o efeito de uma etapa anterior quando a transferência não pode ser concluída.
_Código_: `Compensation`
_Evite_: rollback, estorno

**Limite diário**:
Valor máximo que uma conta pode transferir num mesmo dia.
_Código_: `DailyLimit`

**Reserva de limite**:
Parte do limite diário separada para uma transferência em andamento; consumida quando ela conclui e liberada na compensação.
_Código_: `LimitReservation`
_Evite_: bloqueio, hold

## Consulta e avisos

**Extrato**:
Lista dos lançamentos de uma conta em ordem cronológica, para consulta do cliente.
_Código_: `Statement`
_Evite_: histórico

**Notificação**:
Aviso enviado ao cliente sobre algo que aconteceu na conta.
_Código_: `Notification`
_Evite_: alerta, mensagem
