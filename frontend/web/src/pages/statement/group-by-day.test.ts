import { describe, expect, it } from 'vitest';
import type { Entry } from '@/lib/api-types';
import { groupByDay } from './group-by-day';

const entry = (id: string, createdAt: string): Entry => ({
  id,
  kind: 'CREDIT',
  amount: 100,
  description: id,
  balanceAfter: 100,
  createdAt,
});

// 2026-11-20 12:00 in São Paulo (UTC-3).
const NOW = new Date('2026-11-20T15:00:00Z');

describe('groupByDay', () => {
  it('groups entries by São Paulo day ("Hoje", "Ontem", date), keeping the order', () => {
    const groups = groupByDay(
      [
        entry('a', '2026-11-20T14:00:00Z'),
        entry('b', '2026-11-20T10:00:00Z'),
        entry('c', '2026-11-19T20:00:00Z'),
        entry('d', '2026-11-10T12:00:00Z'),
      ],
      NOW,
    );
    expect(groups.map((g) => g.label)).toEqual(['Hoje', 'Ontem', '10 de novembro de 2026']);
    expect(groups.map((g) => g.entries.map((e) => e.id))).toEqual([['a', 'b'], ['c'], ['d']]);
  });

  it('uses the São Paulo day, not the UTC day, at the edge of midnight', () => {
    // 02:30 UTC on the 20th is 23:30 on the 19th in São Paulo.
    const [group] = groupByDay([entry('late', '2026-11-20T02:30:00Z')], NOW);
    expect(group.label).toBe('Ontem');
  });

  it('keeps the same day in one group across pages', () => {
    const groups = groupByDay(
      [entry('a', '2026-11-20T14:00:00Z'), entry('b', '2026-11-20T11:00:00Z')],
      NOW,
    );
    expect(groups).toHaveLength(1);
  });

  it('returns no group for no entries', () => {
    expect(groupByDay([], NOW)).toEqual([]);
  });
});
