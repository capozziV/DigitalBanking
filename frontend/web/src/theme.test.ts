import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

describe('theme', () => {
  const root = resolve(__dirname, '..');

  it('serves the app in the dark theme', () => {
    expect(readFileSync(resolve(root, 'index.html'), 'utf8')).toMatch(/<html[^>]*class="dark"/);
  });

  it('uses the system font stack, not Inter', () => {
    expect(readFileSync(resolve(__dirname, 'index.css'), 'utf8')).not.toMatch(/inter/i);
  });
});
