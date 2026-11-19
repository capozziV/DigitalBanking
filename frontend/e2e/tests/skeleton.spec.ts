import { expect, test } from '@playwright/test';

test('opens the app root and shows the bank name', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByRole('heading', { level: 1 })).toContainText('Banco Digital');
});
