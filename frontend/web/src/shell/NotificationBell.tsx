import { Bell } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router';
import { cn } from 'cn';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { unreadBadge } from '@/shell/nav-items';
import { bff } from '@/lib/bff-client';
import type { NotificationDTO, NotificationPageDTO } from '@/lib/api-types';
import { refreshUnread, useSession } from '@/lib/session';
import { LoadNotice } from '@/pages/notifications/LoadNotice';
import { NotificationList } from '@/pages/notifications/NotificationList';

/** Distance from the end of the list, in px, at which the next page is fetched. */
const LOAD_MORE_THRESHOLD = 24;

/**
 * The bell with the unread count. The panel opens without leaving the page, lists the recent
 * notifications and marks them read as it opens (the rows stay highlighted until it closes).
 */
export function NotificationBell({
  align = 'end',
  className,
}: {
  align?: 'start' | 'end';
  className?: string;
}) {
  const { unreadCount } = useSession();
  const [open, setOpen] = useState(false);
  const label = unreadCount > 0 ? `Notificações, ${unreadCount} não lidas` : 'Notificações';

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger
        aria-label={label}
        className={cn(
          'relative grid size-11 place-items-center rounded-full border border-transparent text-foreground transition-colors hover:border-line hover:bg-glass aria-expanded:border-line aria-expanded:bg-glass',
          className,
        )}
      >
        <Bell aria-hidden="true" className="size-[1.15rem]" strokeWidth={1.8} />
        {unreadCount > 0 ? (
          <span
            aria-hidden="true"
            className="absolute top-0 right-0 min-w-[1.15rem] rounded-full bg-alert px-1 text-center text-[0.6875rem] leading-[1.15rem] font-semibold text-white tabular-nums"
          >
            {unreadBadge(unreadCount)}
          </span>
        ) : null}
      </PopoverTrigger>
      <PopoverContent
        aria-label="Notificações"
        align={align}
        sideOffset={10}
        className="w-80 gap-3 rounded-2xl border border-line bg-popover/95 p-4 backdrop-blur-xl"
      >
        <RecentNotifications onNavigate={() => setOpen(false)} />
      </PopoverContent>
    </Popover>
  );
}

/** Mounted only while the panel is open, so every open loads from the first page. */
function RecentNotifications({ onNavigate }: { onNavigate: () => void }) {
  const [items, setItems] = useState<NotificationDTO[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [failed, setFailed] = useState(false);
  const nextCursor = useRef<string | null>(null);
  const fetchingMore = useRef(false);
  /** Bumped on every load and on unmount, so a response that arrives late is ignored. */
  const generation = useRef(0);
  /** Bumped to load again (retry). */
  const [version, setVersion] = useState(0);

  useEffect(() => {
    const mine = ++generation.current;
    fetchingMore.current = false;
    const isCurrent = (): boolean => mine === generation.current;
    bff<NotificationPageDTO>('/bff/notifications').then(
      async (page) => {
        if (!isCurrent()) return;
        setItems(page.items);
        nextCursor.current = page.nextCursor;
        setFailed(false);
        setLoaded(true);
        if (!page.items.some((item) => !item.readAt)) return;
        try {
          await bff('/bff/notifications/read', {
            method: 'POST',
            body: { upTo: page.items[0].id },
          });
          if (isCurrent()) await refreshUnread({ fresh: true });
        } catch {
          // Not marked: the counter stays and the next open tries again.
        }
      },
      () => {
        if (isCurrent()) setFailed(true);
      },
    );
    return () => {
      // Invalidates this load (and any "load more" of it); a counter, not a DOM node.
      // eslint-disable-next-line react-hooks/exhaustive-deps
      generation.current++;
    };
  }, [version]);

  async function loadMore(): Promise<void> {
    const cursor = nextCursor.current;
    if (!cursor || fetchingMore.current) return;
    const mine = generation.current;
    fetchingMore.current = true;
    try {
      const page = await bff<NotificationPageDTO>(
        `/bff/notifications?cursor=${encodeURIComponent(cursor)}`,
      );
      if (mine !== generation.current) return;
      setItems((current) => [...current, ...page.items]);
      nextCursor.current = page.nextCursor;
    } catch {
      // The next scroll tries again.
    } finally {
      if (mine === generation.current) fetchingMore.current = false;
    }
  }

  function onScroll(event: React.UIEvent<HTMLDivElement>): void {
    const list = event.currentTarget;
    if (list.scrollHeight - list.clientHeight - list.scrollTop <= LOAD_MORE_THRESHOLD) {
      void loadMore();
    }
  }

  return (
    <>
      <p className="font-heading text-base font-semibold tracking-tight">Notificações</p>
      {failed ? (
        <LoadNotice
          onRetry={() => {
            setFailed(false);
            setVersion((value) => value + 1);
          }}
        />
      ) : null}
      {loaded ? (
        <div
          data-testid="notification-scroll"
          onScroll={onScroll}
          className="max-h-80 overflow-y-auto overscroll-contain"
        >
          {items.length > 0 ? (
            <NotificationList items={items} compact />
          ) : (
            <p className="py-4 text-sm text-muted-foreground">Nenhuma notificação por enquanto.</p>
          )}
        </div>
      ) : null}
      <Link
        to="/notificacoes"
        onClick={onNavigate}
        className="self-start text-sm font-medium text-accent-1 hover:underline"
      >
        Ver todas
      </Link>
    </>
  );
}
