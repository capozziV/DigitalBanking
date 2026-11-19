import { expect, test } from '@playwright/test';

test('sends an anonymous visitor from the root to Entrar', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveURL(/\/entrar$/);
  await expect(page.getByRole('heading', { level: 1 })).toContainText('Entrar');
});
