import { Bell } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router';
import { cn } from 'cn';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { unreadBadge } from '@/shell/nav-items';
import { useSession } from '@/shell/session-stub';

/**
 * The bell with the unread count. The panel opens without leaving the page; its list of recent
 * notifications and "marcar como lidas" arrive with the Notificações screen.
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
        className="w-80 rounded-2xl border border-line bg-popover/95 p-4 backdrop-blur-xl"
      >
        <p className="font-heading text-base font-semibold tracking-tight">Notificações</p>
        <p className="text-sm text-muted-foreground">
          {unreadCount > 0 ? `Você tem ${unreadCount} não lidas.` : 'Tudo em dia por aqui.'}
        </p>
        <Link
          to="/notificacoes"
          onClick={() => setOpen(false)}
          className="self-start text-sm font-medium text-accent-1 hover:underline"
        >
          Ver todas
        </Link>
      </PopoverContent>
    </Popover>
  );
}
