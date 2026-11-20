import { defineConfig, devices } from '@playwright/test';

// BASE_URL points at the stack under test (nginx in compose, ALB on AWS).
// Without it, the skeleton run serves the built web app locally.
const externalBaseUrl = process.env['BASE_URL'];
const onCi = !!process.env['CI'];

export default defineConfig({
  testDir: './tests',
  // Worst case from sign-up to the first transfer: 3 x 15 s of `eventually` + 30 s waiting for the transfer status + UI steps (sign-up, profile, account opening).
  timeout: 90_000,
  // Every spec creates its own customer (unique e-mail and phone), so files run in parallel.
  fullyParallel: true,
  forbidOnly: onCi,
  // One retry on CI only, for the saga and the event projections to settle on a cold runner.
  retries: onCi ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: externalBaseUrl ?? 'http://127.0.0.1:4200',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: externalBaseUrl
    ? undefined
    : {
        command: 'npm run start -w web -- --host 127.0.0.1 --port 4200',
        url: 'http://127.0.0.1:4200',
        reuseExistingServer: true,
        timeout: 120_000,
      },
});
