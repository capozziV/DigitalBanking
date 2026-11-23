import { expect, test } from '@playwright/test';
import {
  completeProfile,
  DEMO,
  DEMO_PHONE_DIGITS,
  openAccount,
  signUp,
  transfer,
  uniqueEmail,
  uniquePhoneDigits,
} from './support';

// A fresh customer, not the demo: the saga reserves the daily limit (R$ 5.000,00) before it debits, so an amount above
// it is refused whatever the balance, and parallel specs never share the demo's own limit.
test('a transfer above the daily limit is refused with the reason in Portuguese', async ({
  page,
}) => {
  await signUp(page, uniqueEmail());
  await completeProfile(page, 'Cliente Limite E2E', uniquePhoneDigits());
  await openAccount(page);

  await transfer(page, DEMO_PHONE_DIGITS, '5.000,01', DEMO.fullName);
  const status = page.getByRole('status');
  await expect(status).toContainText('Recusada', { timeout: 30_000 });
  await expect(status).toContainText('Você atingiu o limite diário de transferências.');
});
