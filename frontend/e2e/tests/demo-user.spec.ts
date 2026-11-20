import { expect, test } from '@playwright/test';
import { DEMO, eventually, signIn } from './support';

test('the demo user signs in and sees balance, statement entries and notifications', async ({
  page,
}) => {
  await signIn(page, DEMO.email, DEMO.password);

  await expect(page.getByText(`Olá, ${DEMO.fullName.split(' ')[0]}`)).toBeVisible();
  await expect(page.getByText('Saldo disponível')).toBeVisible();

  await page.getByRole('link', { name: 'Extrato', exact: true }).first().click();
  await eventually(page, async () => {
    await expect(page.getByText('Bônus de boas-vindas')).toBeVisible({ timeout: 2_000 });
    await expect(page.locator('app-entry-list li').nth(1)).toBeVisible({ timeout: 2_000 });
  });

  await page
    .getByRole('link', { name: /^Notificações/ })
    .first()
    .click();
  await eventually(page, async () => {
    await expect(page.getByText('Transferência enviada').first()).toBeVisible({ timeout: 2_000 });
    await expect(page.getByText('Você recebeu uma transferência').first()).toBeVisible({
      timeout: 2_000,
    });
  });

  await page.getByRole('button', { name: 'Sair' }).click();
  await expect(page).toHaveURL(/\/entrar$/);
  await page.goto('/inicio');
  await expect(page).toHaveURL(/\/entrar$/);
  await page.goBack();
  await expect(page).toHaveURL(/\/entrar$/);
});
