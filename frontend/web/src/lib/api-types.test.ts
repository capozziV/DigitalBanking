import { describe, expect, it } from 'vitest';
import { accountTypeLabel } from './api-types';

describe('accountTypeLabel', () => {
  it.each([
    ['FREE', 'Free'],
    ['STANDARD', 'Standard'],
    ['GOLD', 'Gold'],
    ['BLACK', 'Black'],
    ['DIAMOND', 'Diamond'],
  ])('labels %s as %s', (type, label) => {
    expect(accountTypeLabel(type)).toBe(label);
  });

  it('shows an unknown type as it came', () => {
    expect(accountTypeLabel('PLATINUM')).toBe('PLATINUM');
  });
});
