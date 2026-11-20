import { defineConfig, devices } from '@playwright/test';

// BASE_URL points at the stack under test (nginx in compose, ALB on AWS).
// Without it, the skeleton run serves the built web app locally.
const externalBaseUrl = process.env['BASE_URL'];
const onCi = !!process.env['CI'];

export default defineConfig({
  testDir: './tests',
  // Sign-up, the saga and the event projections run for real: more room than the default 30 s; each `eventually` waits up to 15 s and a spec chains a few.
  timeout: 60_000,
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
