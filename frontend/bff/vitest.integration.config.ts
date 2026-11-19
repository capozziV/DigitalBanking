import { defineConfig } from 'vitest/config';
import swc from 'unplugin-swc';

// Integration specs need the compose services (Redis and mock-oauth2-server); `npm test` skips them.
export default defineConfig({
  test: {
    globals: true,
    root: './',
    include: ['src/**/*.integration.spec.ts'],
  },
  plugins: [swc.vite({ module: { type: 'es6' } })],
});
