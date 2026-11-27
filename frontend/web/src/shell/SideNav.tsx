import { LogOut } from 'lucide-react';
import { NavLink } from 'react-router';
import { cn } from 'cn';
import { NotificationBell } from '@/shell/NotificationBell';
import { NAV_ITEMS } from '@/shell/nav-items';
import { signOut } from '@/lib/session';

/** Desktop navigation (1024 px and up): a quiet glass column; the bell carries the unread count. */
export function SideNav() {
  return (
    <aside className="fixed inset-y-0 left-0 z-20 hidden w-60 flex-col border-r border-line bg-white/[0.02] px-4 py-6 backdrop-blur-2xl lg:flex">
      <div className="mb-8 flex items-center justify-between pl-2">
        <span className="font-heading text-lg font-bold tracking-[-0.03em]">capzzPay</span>
        <NotificationBell align="start" />
      </div>
      <nav aria-label="Principal" className="flex flex-col gap-1">
        {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              cn(
                'flex items-center gap-3 rounded-xl border px-3 py-2.5 text-sm transition-colors duration-200',
                isActive
                  ? 'border-line bg-glass-strong text-foreground'
                  : 'border-transparent text-muted-foreground hover:bg-glass hover:text-foreground',
              )
            }
          >
            <Icon aria-hidden="true" className="size-[1.05rem]" strokeWidth={1.8} />
            {label}
          </NavLink>
        ))}
      </nav>
      <button
        type="button"
        onClick={() => void signOut()}
        className="mt-auto flex items-center gap-3 rounded-xl border border-transparent px-3 py-2.5 text-sm text-muted-foreground transition-colors hover:bg-glass hover:text-foreground"
      >
        <LogOut aria-hidden="true" className="size-[1.05rem]" strokeWidth={1.8} />
        Sair
      </button>
    </aside>
  );
}
