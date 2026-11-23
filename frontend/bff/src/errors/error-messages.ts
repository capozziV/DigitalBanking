export const GENERIC_ERROR = 'Não foi possível concluir a operação. Tente de novo.';
export const UNAVAILABLE = 'Serviço indisponível. Tente novamente.';

/**
 * What the screen says for each `type` the services answer on the routes the BFF calls
 * (`ProblemDetailsExceptionHandler` of each service and `SecurityProblems`). Their `detail` is English and may
 * carry ids, so it never goes on. A Map, so that a crafted type does not find a prototype property.
 */
export const PROBLEM_MESSAGES: ReadonlyMap<string, string> = new Map([
  ['/problems/phone-already-in-use', 'Este celular já está em uso.'],
  ['/problems/customer-not-found', 'Cadastro não encontrado. Complete seu cadastro.'],
  ['/problems/customer-blocked', 'Seu cadastro está bloqueado para esta operação.'],
  ['/problems/customer-already-has-account', 'Você já tem uma conta.'],
  ['/problems/account-not-found', 'Conta não encontrada.'],
  ['/problems/account-closed', 'Esta conta está encerrada.'],
  [
    '/problems/account-busy',
    'Sua conta está processando outra operação. Tente de novo em instantes.',
  ],
  ['/problems/balance-not-zero', 'Para encerrar a conta, o saldo precisa estar zerado.'],
  [
    '/problems/pending-transfers',
    'Há transferências em andamento. Tente encerrar quando elas terminarem.',
  ],
  ['/problems/same-account-transfer', 'Não é possível transferir para você mesmo.'],
  ['/problems/insufficient-available-balance', 'Saldo insuficiente para esta transferência.'],
  ['/problems/transfer-failed', 'A transferência não pôde ser concluída.'],
  ['/problems/transfer-not-found', 'Transferência não encontrada.'],
  [
    '/problems/idempotency-key-reused',
    'Este envio já foi feito com outros dados. Confira o extrato antes de tentar de novo.',
  ],
  ['/problems/notification-not-found', 'Notificação não encontrada.'],
  ['/problems/accounts-unavailable', UNAVAILABLE],
  ['/problems/customers-unavailable', UNAVAILABLE],
  ['/problems/transfers-unavailable', UNAVAILABLE],
  ['/problems/validation-error', 'Confira os dados informados.'],
  ['/problems/missing-idempotency-key', GENERIC_ERROR],
  ['/problems/unauthorized', 'Sessão expirada. Entre de novo.'],
  ['/problems/forbidden', 'Você não pode fazer esta operação.'],
  ['/problems/internal-error', GENERIC_ERROR],
]);
