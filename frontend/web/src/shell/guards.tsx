import { useEffect, useState, type ReactNode } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router';
import { load, useSession, type SessionState } from '@/lib/session';

/** Where the guard sends the visitor instead, or null to let them through. */
type Decide = (state: SessionState) => string | null;

/**
 * Decides only on a session that could be loaded. A failed load says nothing about who the user
 * is: it goes to Indisponível, remembering the address for "Tentar de novo". While a reload is in
 * flight (sign-in, Completar cadastro), it keeps deciding on the last loaded session, so the
 * screen stays mounted; the new session decides once it is loaded.
 */
function OnLoadedSession({ decide, children }: { decide: Decide; children?: ReactNode }) {
  const current = useSession();
  const location = useLocation();
  const [lastLoaded, setLastLoaded] = useState<SessionState | null>(
    current.loaded ? current : null,
  );
  if (current.loaded && current !== lastLoaded) setLastLoaded(current);
  const state = current.loaded ? current : (lastLoaded ?? current);

  useEffect(() => {
    if (!current.loaded && !current.loadFailed) void load();
  }, [current.loaded, current.loadFailed]);

  if (current.loadFailed) {
    return (
      <Navigate to="/indisponivel" replace state={{ from: location.pathname + location.search }} />
    );
  }
  if (!state.loaded) return null;
  const target = decide(state);
  if (target) return <Navigate to={target} replace />;
  return children ?? <Outlet />;
}

const guest: Decide = (state) => (state.authenticated ? '/inicio' : null);

const unregistered: Decide = (state) => {
  if (!state.authenticated) return '/entrar';
  return state.customer ? '/inicio' : null;
};

const customer: Decide = (state) => {
  if (!state.authenticated) return '/entrar';
  return state.customer ? null : '/completar-cadastro';
};

/** Entrar and Criar conta are for visitors; a signed-in user goes into the app. */
export function GuestOnly({ children }: { children?: ReactNode }) {
  return <OnLoadedSession decide={guest}>{children}</OnLoadedSession>;
}

/** Completar cadastro is for signed-in users who are not customers yet. */
export function UnregisteredOnly({ children }: { children?: ReactNode }) {
  return <OnLoadedSession decide={unregistered}>{children}</OnLoadedSession>;
}

/** Customer screens: visitors go to Entrar, users without a customer to Completar cadastro. */
export function CustomerOnly({ children }: { children?: ReactNode }) {
  return <OnLoadedSession decide={customer}>{children}</OnLoadedSession>;
}
