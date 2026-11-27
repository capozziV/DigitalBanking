import { useEffect } from 'react';
import { Outlet, useMatches } from 'react-router';

export interface RouteHandle {
  title?: string;
}

const APP_NAME = 'capzzPay';

/**
 * Top of every route: names the document after the screen. A lost session (401) is cleared by the
 * session store itself; the guards then send the user to Entrar, a single navigation. Errors of
 * the BFF during a screen stay on that screen.
 */
export function RootLayout() {
  const title = [...useMatches()]
    .reverse()
    .map((match) => (match.handle as RouteHandle | undefined)?.title)
    .find(Boolean);

  useEffect(() => {
    document.title = title ? `${title} · ${APP_NAME}` : APP_NAME;
  }, [title]);

  return <Outlet />;
}
