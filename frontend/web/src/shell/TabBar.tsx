import { NavLink } from 'react-router';
import { cn } from 'cn';
import { NAV_ITEMS } from '@/shell/nav-items';

/**
 * Phone and tablet navigation (below 1024 px): a floating glass bar above the home indicator. The
 * current tab is marked by a glass pill behind its icon, not by color alone.
 */
export function TabBar() {
  return (
    <nav
      aria-label="Abas"
      className="fixed inset-x-3 bottom-[max(0.75rem,env(safe-area-inset-bottom))] z-30 mx-auto flex h-16 max-w-md items-stretch justify-around rounded-[1.4rem] border border-line bg-[rgb(20_20_24/0.72)] px-1 backdrop-blur-2xl lg:hidden"
    >
      {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
        <NavLink
          key={to}
          to={to}
          className={({ isActive }) =>
            cn(
              'group flex min-w-0 flex-1 flex-col items-center justify-center gap-1 rounded-2xl text-[0.6875rem] leading-none tracking-[-0.01em] transition-colors duration-200',
              isActive ? 'text-foreground' : 'text-muted-foreground hover:text-foreground',
            )
          }
        >
          <span className="grid h-7 w-12 place-items-center rounded-full transition-colors duration-200 group-aria-[current=page]:bg-glass-strong">
            <Icon aria-hidden="true" className="size-5" strokeWidth={1.8} />
          </span>
          <span className="max-w-full truncate px-0.5">{label}</span>
        </NavLink>
      ))}
    </nav>
  );
}
