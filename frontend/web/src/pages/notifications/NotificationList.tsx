import type { NotificationDTO } from '@/lib/api-types';
import { cn } from 'cn';
import { groupByDay, notificationTime } from './notification-days';

/**
 * The notifications grouped by day, shared by the Notificações screen and the bell panel. Unread
 * rows carry `data-unread` and a dot, with the state also said in text for screen readers.
 */
export function NotificationList({
  items,
  compact = false,
}: {
  items: NotificationDTO[];
  compact?: boolean;
}) {
  const days = groupByDay(items);
  const DayHeading = compact ? 'p' : 'h2';
  return (
    <div className={cn('flex flex-col', compact ? 'gap-3' : 'gap-6')}>
      {days.map((day) => (
        <section key={day.key} className="flex flex-col gap-1">
          <DayHeading className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
            {day.label}
          </DayHeading>
          <ul className="m-0 flex list-none flex-col p-0">
            {day.items.map((item) => (
              <li
                key={item.id}
                data-unread={item.readAt ? undefined : 'true'}
                className={cn(
                  'flex gap-3 border-t border-line py-3 first:border-t-0',
                  item.readAt ? 'text-muted-foreground' : 'text-foreground',
                )}
              >
                <span
                  aria-hidden="true"
                  className={cn(
                    'mt-1.5 size-2 shrink-0 rounded-full',
                    item.readAt ? 'bg-transparent' : 'bg-accent-1',
                  )}
                />
                <div className="flex min-w-0 flex-1 flex-col gap-0.5">
                  <span className={cn('text-sm', !item.readAt && 'font-semibold')}>
                    {item.title}
                    {item.readAt ? null : <span className="sr-only"> (não lida)</span>}
                  </span>
                  <span className="text-sm break-words">{item.body}</span>
                  <time dateTime={item.createdAt} className="text-xs text-muted-foreground">
                    {notificationTime(item.createdAt)}
                  </time>
                </div>
              </li>
            ))}
          </ul>
        </section>
      ))}
    </div>
  );
}
