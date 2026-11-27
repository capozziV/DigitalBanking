import { BellRing } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { Button } from '@/components/ui/button';
import { GlassPanel } from '@/design/GlassPanel';
import { Reveal } from '@/design/motion';
import { bff } from '@/lib/bff-client';
import type { NotificationDTO, NotificationPageDTO } from '@/lib/api-types';
import { problemMessage } from '@/lib/problem';
import { refreshUnread } from '@/lib/session';
import { LoadNotice } from './LoadNotice';
import { NotificationList } from './NotificationList';

export function NotificationsPage() {
  const [items, setItems] = useState<NotificationDTO[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [failed, setFailed] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  /** Bumped to fetch the list again (retry, after marking as read). */
  const [version, setVersion] = useState(0);
  const alive = useRef(true);

  useEffect(() => {
    let current = true;
    bff<NotificationPageDTO>('/bff/notifications').then(
      (page) => {
        if (!current) return;
        setItems(page.items);
        setFailed(false);
        setLoaded(true);
      },
      () => {
        if (current) setFailed(true);
      },
    );
    return () => {
      current = false;
    };
  }, [version]);

  useEffect(() => {
    alive.current = true;
    return () => {
      alive.current = false;
    };
  }, []);

  const hasUnread = items.some((item) => !item.readAt);

  /** Marks as read everything up to the newest notification (first in the list, by id). */
  async function markAllRead(): Promise<void> {
    const newest = items[0]?.id;
    if (!newest || busy) return;
    setBusy(true);
    setError(null);
    try {
      await bff('/bff/notifications/read', { method: 'POST', body: { upTo: newest } });
      setVersion((value) => value + 1);
      await refreshUnread({ fresh: true });
    } catch (caught) {
      if (alive.current) setError(problemMessage(caught, 'Não foi possível marcar como lidas.'));
    } finally {
      if (alive.current) setBusy(false);
    }
  }

  const retry = (): void => {
    setFailed(false);
    setVersion((value) => value + 1);
  };

  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="font-heading text-3xl font-semibold tracking-tight">Notificações</h1>
        {loaded && hasUnread ? (
          <Button variant="outline" disabled={busy} onClick={() => void markAllRead()}>
            Marcar como lidas
          </Button>
        ) : null}
      </div>
      {failed ? <LoadNotice onRetry={retry} /> : null}
      {error ? (
        <p role="alert" className="text-sm text-alert">
          {error}
        </p>
      ) : null}
      {loaded ? (
        <Reveal>
          <GlassPanel className="p-5 md:p-6">
            {items.length > 0 ? (
              <NotificationList items={items} />
            ) : (
              <div className="flex flex-col items-center gap-2 py-10 text-center">
                <BellRing aria-hidden="true" className="size-8 text-muted-foreground" />
                <p className="font-heading text-lg font-semibold tracking-tight">
                  Nenhuma notificação por enquanto.
                </p>
                <p className="max-w-xs text-sm text-muted-foreground">
                  Transferências, mudanças na conta e avisos do banco aparecem aqui.
                </p>
              </div>
            )}
          </GlassPanel>
        </Reveal>
      ) : null}
    </div>
  );
}
