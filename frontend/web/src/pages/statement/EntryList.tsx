import type { Entry } from '@/lib/api-types';
import { formatCents } from '@/lib/money';
import { EntryIcon } from './EntryIcon';
import { BANK_TIME_ZONE } from './group-by-day';

const timeFormat = new Intl.DateTimeFormat('pt-BR', {
  timeZone: BANK_TIME_ZONE,
  hour: '2-digit',
  minute: '2-digit',
});
const dateFormat = new Intl.DateTimeFormat('pt-BR', {
  timeZone: BANK_TIME_ZONE,
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
});

function detailOf(entry: Entry, detail: 'home' | 'statement'): string {
  const at = new Date(entry.createdAt);
  const time = timeFormat.format(at);
  return detail === 'statement'
    ? `${time} · saldo ${formatCents(entry.balanceAfter)}`
    : `${dateFormat.format(at)} às ${time}`;
}

/** Entries as rows: the description is the counterpart's name, or "Crédito" / "Débito". */
export function EntryList({
  entries,
  detail = 'home',
}: {
  entries: Entry[];
  detail?: 'home' | 'statement';
}) {
  return (
    <ul className="m-0 list-none p-0">
      {entries.map((entry) => {
        const credit = entry.kind === 'CREDIT';
        return (
          <li
            key={entry.id}
            className="flex items-center gap-3 border-t border-line py-3 first:border-t-0"
          >
            <EntryIcon kind={entry.kind} />
            <span className="flex min-w-0 flex-1 flex-col">
              <span className="truncate text-sm">
                {entry.description ?? (credit ? 'Crédito' : 'Débito')}
              </span>
              <span className="text-xs text-muted">{detailOf(entry, detail)}</span>
            </span>
            <span
              data-testid="amount"
              data-kind={entry.kind}
              className={
                credit
                  ? 'text-sm font-semibold whitespace-nowrap text-positive'
                  : 'text-sm whitespace-nowrap text-muted'
              }
            >
              {credit ? '+' : '−'} {formatCents(entry.amount)}
            </span>
          </li>
        );
      })}
    </ul>
  );
}
