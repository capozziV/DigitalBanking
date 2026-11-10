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
Número de centavos, sempre positivo, de um lançamento.
_Código_: `Money`
_Evite_: quantia, montante

**Tipo de lançamento**:
Indica se o lançamento é um crédito ou um débito; valores `CREDIT` e `DEBIT`.
_Código_: `EntryKind`

**Crédito**:
Lançamento que aumenta o saldo da conta; valor de Tipo de lançamento.
_Código_: `EntryKind.CREDIT`

**Débito**:
Lançamento que diminui o saldo da conta; valor de Tipo de lançamento.
_Código_: `EntryKind.DEBIT`

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

**Status da transferência**:
Situação da transferência; valores `PENDING`, `COMPLETED`, `FAILED` e `COMPENSATED`.
_Código_: `TransferStatus`
_Evite_: estado (sem qualificador, para não confundir com o passo)

**Pendente**:
A transferência ainda não terminou; valor de Status da transferência.
_Código_: `TransferStatus.PENDING`

**Concluída**:
O valor saiu da origem e chegou ao destino; valor de Status da transferência.
_Código_: `TransferStatus.COMPLETED`

**Falhou**:
A transferência foi recusada antes de tirar valor da origem; valor de Status da transferência.
_Código_: `TransferStatus.FAILED`

**Compensada**:
O valor chegou a sair da origem, não pôde chegar ao destino e voltou para a origem; valor de Status da transferência.
_Código_: `TransferStatus.COMPENSATED`

**Passo da saga**:
Etapa da saga de transferência em andamento (débito, crédito, consulta, compensação) ou terminal; define o status da transferência.
_Código_: `TransferStep`
_Evite_: estágio, fase

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

**Tipo de notificação**:
Categoria da notificação, que define o texto mostrado ao cliente; valores Boas-vindas, Tipo de conta alterado, Transferência enviada, Transferência recebida, Transferência recusada e Transferência devolvida.
_Código_: `NotificationKind` (valores `WELCOME`, `ACCOUNT_TYPE_CHANGED`, `TRANSFER_SENT`, `TRANSFER_RECEIVED`, `TRANSFER_FAILED`, `TRANSFER_COMPENSATED`)
