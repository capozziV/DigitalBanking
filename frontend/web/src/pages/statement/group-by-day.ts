import type { Entry } from '@/lib/api-types';

export const BANK_TIME_ZONE = 'America/Sao_Paulo';

export interface DayGroup {
  /** `YYYY-MM-DD` of the São Paulo day. */
  key: string;
  label: string;
  entries: Entry[];
}

const dayKeyFormat = new Intl.DateTimeFormat('en-CA', {
  timeZone: BANK_TIME_ZONE,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
});

const longDayFormat = new Intl.DateTimeFormat('pt-BR', {
  timeZone: 'UTC',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
});

/** The calendar day of an instant in São Paulo, as `YYYY-MM-DD`. */
export const dayKey = (instant: Date): string => dayKeyFormat.format(instant);

const previousDayKey = (key: string): string => {
  const [year, month, day] = key.split('-').map(Number);
  return new Date(Date.UTC(year, month - 1, day - 1)).toISOString().slice(0, 10);
};

function labelOf(key: string, today: string): string {
  if (key === today) return 'Hoje';
  if (key === previousDayKey(today)) return 'Ontem';
  return longDayFormat.format(new Date(`${key}T00:00:00Z`));
}

/** Groups entries (newest first) by São Paulo day, keeping their order. */
export function groupByDay(entries: Entry[], now: Date = new Date()): DayGroup[] {
  const today = dayKey(now);
  const groups = new Map<string, DayGroup>();
  for (const entry of entries) {
    const key = dayKey(new Date(entry.createdAt));
    const group = groups.get(key) ?? { key, label: labelOf(key, today), entries: [] };
    group.entries.push(entry);
    groups.set(key, group);
  }
  return [...groups.values()];
}
