import { useCallback, useEffect, useRef, useState } from 'react';
import { Button } from '@/components/ui/button';
import { GlassPanel } from '@/design/GlassPanel';
import { Reveal } from '@/design/motion';
import type { Entry } from '@/lib/api-types';
import { bff } from '@/lib/bff-client';
import { EntryList } from './EntryList';
import { groupByDay } from './group-by-day';
import { LoadError } from './LoadError';

interface StatementPageDTO {
  entries: Entry[];
  nextCursor: string | null;
}

/** Statement: entries grouped by São Paulo day, with the balance after each, paged by cursor. */
export function StatementPage() {
  const [entries, setEntries] = useState<Entry[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [loaded, setLoaded] = useState(false);
  const [failed, setFailed] = useState(false);
  const [busy, setBusy] = useState(false);
  const busyRef = useRef(false);
  const cursorRef = useRef<string | null>(null);

  /** Loads the first page, or the next one by the cursor. */
  const loadMore = useCallback(async () => {
    if (busyRef.current) return;
    busyRef.current = true;
    setBusy(true);
    setFailed(false);
    const cursor = cursorRef.current;
    const path = cursor
      ? (`/bff/statement?cursor=${encodeURIComponent(cursor)}` as const)
      : '/bff/statement';
    try {
      const page = await bff<StatementPageDTO>(path);
      setEntries((current) => [...current, ...page.entries]);
      cursorRef.current = page.nextCursor;
      setNextCursor(page.nextCursor);
      setLoaded(true);
    } catch {
      setFailed(true);
    } finally {
      busyRef.current = false;
      setBusy(false);
    }
  }, []);

  useEffect(() => {
    void loadMore();
  }, [loadMore]);

  const groups = groupByDay(entries);

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="mb-6 font-heading text-3xl font-bold tracking-[-0.03em]">Extrato</h1>
      {failed ? <LoadError onRetry={() => void loadMore()} /> : null}
      {loaded ? (
        <Reveal>
          {groups.length === 0 ? (
            <GlassPanel className="p-5">
              <p className="text-sm text-muted-foreground">Nenhum lançamento ainda.</p>
            </GlassPanel>
          ) : (
            groups.map((group) => (
              <section key={group.key} className="mb-5">
                <h2 className="mb-2 px-1 text-xs font-medium tracking-wide text-muted-foreground uppercase">
                  {group.label}
                </h2>
                <GlassPanel className="px-4 py-1">
                  <EntryList entries={group.entries} detail="statement" />
                </GlassPanel>
              </section>
            ))
          )}
          {nextCursor ? (
            <Button
              type="button"
              variant="outline"
              className="mx-auto flex"
              disabled={busy}
              onClick={() => void loadMore()}
            >
              Carregar mais
            </Button>
          ) : null}
        </Reveal>
      ) : null}
    </div>
  );
}
