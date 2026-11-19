import { defineConfig } from 'vitest/config';
import swc from 'unplugin-swc';

export default defineConfig({
  test: {
    globals: true,
    root: './',
    include: ['src/**/*.spec.ts'],
  },
  // Nest relies on decorator metadata, which esbuild does not emit; SWC does.
  plugins: [swc.vite({ module: { type: 'es6' } })],
});
