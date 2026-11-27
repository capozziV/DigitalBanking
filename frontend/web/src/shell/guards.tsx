import { useEffect, type ReactNode } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router';
import { session, useSession, type SessionState } from '@/shell/session-stub';

/** Where the guard sends the visitor instead, or null to let them through. */
type Decide = (state: SessionState) => string | null;

/**
 * Decides only on a session that could be loaded. A failed load says nothing about who the user
 * is: it goes to Indisponível, remembering the address for "Tentar de novo".
 */
function OnLoadedSession({ decide, children }: { decide: Decide; children?: ReactNode }) {
  const state = useSession();
  const location = useLocation();

  useEffect(() => {
    if (!state.loaded && !state.loadFailed) void session.load();
  }, [state.loaded, state.loadFailed]);

  if (state.loadFailed) {
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
