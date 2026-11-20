import { BackendProblemError } from '../backend/backend-client.js';

/** What the services answer, reduced to the fields the screens use. Amounts are cents; dates are ISO strings. */
export interface AccountDTO {
  id: string;
  type: string;
  status: string;
  balance: number;
  overdraftLimit: number;
  availableBalance: number;
}

export interface CustomerDTO {
  id: string;
  name: string;
  email: string;
  phone: string;
}

export interface StatementEntryDTO {
  entryId: string;
  kind: string;
  amount: number;
  balanceAfter: number;
  description: string | null;
  createdAt: string;
}

export interface StatementDTO {
  entries: StatementEntryDTO[];
  nextCursor: string | null;
}

export interface TransferDTO {
  id: string;
  status: string;
  failureReason: string | null;
}

/** The account as the screens see it: no customer id, no more than the screens show. */
export function toAccountView(account: AccountDTO) {
  return {
    id: account.id,
    type: account.type,
    status: account.status,
    balance: account.balance,
    overdraftLimit: account.overdraftLimit,
    availableBalance: account.availableBalance,
  };
}

export function toEntryView(entry: StatementEntryDTO) {
  return {
    id: entry.entryId,
    kind: entry.kind,
    amount: entry.amount,
    description: entry.description,
    balanceAfter: entry.balanceAfter,
    createdAt: entry.createdAt,
  };
}

export function isNotFound(error: unknown): error is BackendProblemError {
  return error instanceof BackendProblemError && error.status === 404;
}

/** Resolves with `null` when the service answers 404; every other failure goes on. */
export async function orNull<T>(call: Promise<T>): Promise<T | null> {
  try {
    return await call;
  } catch (error) {
    if (isNotFound(error)) return null;
    throw error;
  }
}

export const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
