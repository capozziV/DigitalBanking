import { defineConfig, devices } from '@playwright/test';

// BASE_URL points at the stack under test (nginx in compose, ALB on AWS).
// Without it, the skeleton run serves the built web app locally.
const externalBaseUrl = process.env['BASE_URL'];

export default defineConfig({
  testDir: './tests',
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: externalBaseUrl ?? 'http://127.0.0.1:4200',
    trace: 'retain-on-failure',
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
