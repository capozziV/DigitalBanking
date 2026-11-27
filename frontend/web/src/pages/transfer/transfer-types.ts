export interface Recipient {
  recipientName: string;
  recipientToken: string;
}

export interface TransferStatus {
  id: string;
  status: string;
  reason?: string | null;
}

export type TransferStep = 'phone' | 'amount' | 'confirm' | 'result';

const FINAL_STATUSES = ['COMPLETED', 'FAILED', 'COMPENSATED'];

export const isFinalStatus = (status: string): boolean => FINAL_STATUSES.includes(status);
