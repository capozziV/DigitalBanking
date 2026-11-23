import { defineConfig } from 'vitest/config';
import swc from 'unplugin-swc';

export default defineConfig({
  test: {
    globals: true,
    root: './',
    include: ['src/**/*.spec.ts'],
    exclude: ['**/node_modules/**', 'src/**/*.integration.spec.ts'],
    env: {
      IDENTITY_PROVIDER: 'local',
      REDIS_URL: 'redis://127.0.0.1:1',
      LOG_LEVEL: 'silent',
    },
  },
  // Nest relies on decorator metadata, which esbuild does not emit; SWC does.
  plugins: [swc.vite({ module: { type: 'es6' } })],
});
