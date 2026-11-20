import type { EntryKind } from './entry-icon.component';

/** Ledger entry as the BFF sends it: amounts in cents, dates in ISO. */
export interface Entry {
  id: string;
  kind: EntryKind;
  amount: number;
  description: string | null;
  balanceAfter: number;
  createdAt: string;
}

export interface AccountSummary {
  id: string;
  type: string;
  status: string;
  balance: number;
  overdraftLimit: number;
  availableBalance: number;
}

export interface DailyLimit {
  limit: number;
  used: number;
  available: number;
}

const TYPE_LABELS: Record<string, string> = {
  FREE: 'Free',
  STANDARD: 'Standard',
  GOLD: 'Gold',
  BLACK: 'Black',
  DIAMOND: 'Diamond',
};

export const accountTypeLabel = (type: string): string => TYPE_LABELS[type] ?? type;
