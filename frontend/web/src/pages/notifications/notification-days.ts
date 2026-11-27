import type { NotificationDTO } from '@/lib/api-types';

const TIME_ZONE = 'America/Sao_Paulo';
const DAY_KEY = new Intl.DateTimeFormat('en-CA', { timeZone: TIME_ZONE });
const TIME = new Intl.DateTimeFormat('pt-BR', {
  timeZone: TIME_ZONE,
  hour: '2-digit',
  minute: '2-digit',
});

export interface NotificationDay {
  /** `YYYY-MM-DD` in the bank's time zone. */
  key: string;
  label: string;
  items: NotificationDTO[];
}

const dayKey = (date: Date): string => DAY_KEY.format(date);

const keyToUtc = (key: string): Date => new Date(`${key}T00:00:00Z`);

function dayLabel(key: string, now: Date): string {
  const today = dayKey(now);
  if (key === today) return 'Hoje';
  const yesterday = new Date(keyToUtc(today).getTime() - 24 * 60 * 60 * 1000);
  if (key === yesterday.toISOString().slice(0, 10)) return 'Ontem';
  const date = keyToUtc(key);
  const sameYear = key.slice(0, 4) === today.slice(0, 4);
  return new Intl.DateTimeFormat('pt-BR', {
    timeZone: 'UTC',
    day: 'numeric',
    month: 'long',
    ...(sameYear ? {} : { year: 'numeric' }),
  }).format(date);
}

/** The time of day of a notification, as "15:30". */
export const notificationTime = (createdAt: string): string => TIME.format(new Date(createdAt));

/** Groups the list (newest first) into days, keeping the order. */
export function groupByDay(items: NotificationDTO[], now: Date = new Date()): NotificationDay[] {
  const days: NotificationDay[] = [];
  for (const item of items) {
    const key = dayKey(new Date(item.createdAt));
    const last = days[days.length - 1];
    if (last?.key === key) last.items.push(item);
    else days.push({ key, label: dayLabel(key, now), items: [item] });
  }
  return days;
}
