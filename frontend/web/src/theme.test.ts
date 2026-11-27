import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

describe('theme', () => {
  const root = resolve(__dirname, '..');

  it('serves the app in the dark theme', () => {
    expect(readFileSync(resolve(root, 'index.html'), 'utf8')).toMatch(/<html[^>]*class="dark"/);
  });

  const css = ['index.css', 'design/tokens.css', 'design/effects.css']
    .map((file) => readFileSync(resolve(__dirname, file), 'utf8'))
    .join('\n');

  it('uses the system font stack, not Inter', () => {
    const fontStacks = css.match(/--font-[\w-]+\s*:[^;]+;|font-family\s*:[^;]+;/g) ?? [];
    expect(fontStacks.length).toBeGreaterThan(0);
    for (const stack of fontStacks) expect(stack).not.toMatch(/\bInter\b/i);
    expect(css).toMatch(/--font-sans:[^;]*-apple-system[^;]*'SF Pro Text'[^;]*Roboto/);
  });

  it.each([
    '--bg',
    '--fg',
    '--muted',
    '--line',
    '--glass',
    '--positive',
    '--negative',
    '--accent-1',
    '--accent-2',
    '--accent-3',
  ])('defines the Noite Apple token %s', (token) => {
    expect(css).toMatch(new RegExp(`${token}\\s*:`));
  });

  it('ties the shadcn theme to the Noite Apple tokens, dark only', () => {
    expect(css).toMatch(/--color-background:\s*var\(--bg\)/);
    expect(css).toMatch(/--color-foreground:\s*var\(--fg\)/);
    expect(css).toMatch(/--color-muted-foreground:\s*var\(--muted\)/);
    expect(css).toMatch(/--color-border:\s*var\(--line\)/);
    expect(css).not.toMatch(/^\.dark\s*\{/m);
  });

  it('stops every loop when the visitor asks for reduced motion', () => {
    expect(css).toMatch(/@media \(prefers-reduced-motion: reduce\)/);
  });
});
