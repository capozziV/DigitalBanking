import { useEffect } from 'react';
import { Outlet } from 'react-router';
import { Button } from '@/components/ui/button';
import { AmbientLights } from '@/design/AmbientLights';
import { PageTransition } from '@/design/motion';
import { Footer } from '@/shell/Footer';
import { NotificationBell } from '@/shell/NotificationBell';
import { SideNav } from '@/shell/SideNav';
import { TabBar } from '@/shell/TabBar';
import {
  refreshUnread,
  signOut,
  startUnreadPolling,
  stopUnreadPolling,
  useSession,
} from '@/lib/session';

/** Frame of every customer screen: lights, navigation, the bell, the screen and the footer. */
export function AppShell() {
  const { signOutFailed } = useSession();

  // The bell is right at once (the session does not carry the count), then every 30 s.
  useEffect(() => {
    void refreshUnread();
    startUnreadPolling();
    return () => stopUnreadPolling();
  }, []);

  return (
    <div className="relative min-h-dvh">
      <AmbientLights />
      <SideNav />
      <div className="relative z-10 flex min-h-dvh flex-col lg:pl-60">
        <header className="flex items-center justify-between px-5 pt-[max(1rem,env(safe-area-inset-top))] pb-2 lg:hidden">
          <span className="font-heading text-lg font-bold tracking-[-0.03em]">capzzPay</span>
          <NotificationBell />
        </header>
        <main className="mx-auto w-full max-w-[70rem] flex-1 px-5 pt-4 pb-8 lg:px-10 lg:pt-10">
          {signOutFailed ? (
            <div
              role="alert"
              className="mb-6 flex flex-wrap items-center gap-3 rounded-2xl border border-negative/40 bg-negative/10 px-4 py-3 text-sm"
            >
              <span className="flex-1">
                Não foi possível confirmar a saída. Sua sessão pode continuar aberta.
              </span>
              <Button variant="outline" size="sm" onClick={() => void signOut()}>
                Tentar de novo
              </Button>
            </div>
          ) : null}
          <PageTransition>
            <Outlet />
          </PageTransition>
        </main>
        <Footer className="px-5 pb-28 lg:px-10 lg:pb-8" />
      </div>
      <TabBar />
    </div>
  );
}
