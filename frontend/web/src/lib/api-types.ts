export type EntryKind = 'CREDIT' | 'DEBIT';

export type AccountType = 'FREE' | 'STANDARD' | 'GOLD' | 'BLACK' | 'DIAMOND';

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

export interface Customer {
  name: string;
  email: string;
  phone: string;
}

export interface Me {
  customer: Customer | null;
  account: AccountSummary | null;
}

export interface NotificationDTO {
  id: string;
  kind: string;
  title: string;
  body: string;
  createdAt: string;
  readAt: string | null;
}

export interface NotificationPageDTO {
  items: NotificationDTO[];
  unreadCount: number;
  nextCursor: string | null;
}

const TYPE_LABELS: Record<string, string> = {
  FREE: 'Free',
  STANDARD: 'Standard',
  GOLD: 'Gold',
  BLACK: 'Black',
  DIAMOND: 'Diamond',
};

export const accountTypeLabel = (type: string): string => TYPE_LABELS[type] ?? type;
