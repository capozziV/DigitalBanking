import { expect, test } from '@playwright/test';
import {
  completeProfile,
  DEMO,
  DEMO_PHONE_DIGITS,
  eventually,
  openAccount,
  signUp,
  transfer,
  uniqueEmail,
  uniquePhoneDigits,
} from './support';

test('a new customer signs up, opens the account, sees the bonus and sends the first transfer', async ({
  page,
}) => {
  await signUp(page, uniqueEmail());
  await completeProfile(page, 'Cliente Novo E2E', uniquePhoneDigits());

  await openAccount(page);
  await expect(page.getByText('R$ 100,00').first()).toBeVisible();
  // The statement is projected from events: it may take a moment to show the bonus.
  await page.getByRole('link', { name: 'Extrato', exact: true }).first().click();
  await eventually(page, () =>
    expect(page.getByText('Bônus de boas-vindas')).toBeVisible({ timeout: 2_000 }),
  );

  // Step 2 shows the recipient's full name before anything is sent.
  await transfer(page, DEMO_PHONE_DIGITS, '10,00', DEMO.fullName);
  await expect(page.getByRole('status')).toContainText('Concluída', { timeout: 30_000 });

  await page.getByRole('link', { name: 'Extrato', exact: true }).first().click();
  await eventually(page, () =>
    expect(page.getByText(/−\s*R\$\s*10,00/)).toBeVisible({ timeout: 2_000 }),
  );
  await page
    .getByRole('link', { name: /^Avisos/ })
    .first()
    .click();
  await eventually(page, () =>
    expect(page.getByText('Transferência enviada')).toBeVisible({ timeout: 2_000 }),
  );
});
