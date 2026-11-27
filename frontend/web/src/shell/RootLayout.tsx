import { useEffect, useRef } from 'react';
import { Outlet, useLocation, useMatches, useNavigate } from 'react-router';
import { onSessionLost, onUnavailable, session } from '@/shell/session-stub';

export interface RouteHandle {
  title?: string;
}

const APP_NAME = 'capzzPay';

/**
 * Top of every route: names the document after the screen and reacts to the BFF. A lost session
 * (401) only clears the session; the guards then send the user to Entrar, a single navigation.
 * An unreachable BFF goes to Indisponível, remembering the address.
 */
export function RootLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const current = useRef(location.pathname + location.search);
  const title = [...useMatches()]
    .reverse()
    .map((match) => (match.handle as RouteHandle | undefined)?.title)
    .find(Boolean);

  useEffect(() => {
    current.current = location.pathname + location.search;
  }, [location.pathname, location.search]);

  useEffect(() => {
    document.title = title ? `${title} · ${APP_NAME}` : APP_NAME;
  }, [title]);

  useEffect(() => onSessionLost(() => session.clear()), []);

  useEffect(
    () =>
      onUnavailable(() => {
        const from = current.current;
        if (from.startsWith('/indisponivel')) return;
        void navigate('/indisponivel', { replace: true, state: { from } });
      }),
    [navigate],
  );

  return <Outlet />;
}
