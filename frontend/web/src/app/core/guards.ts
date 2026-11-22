import { inject } from '@angular/core';
import { CanActivateFn, RedirectCommand, Router, UrlTree } from '@angular/router';
import { SessionService } from './session.service';

/**
 * Decides only on a session that could be loaded. A transient failure says nothing about who the
 * user is: it goes to the error screen, keeping the address for "Tentar de novo" and for a reload.
 */
const onLoadedSession =
  (decide: (session: SessionService, router: Router) => boolean | UrlTree): CanActivateFn =>
  async () => {
    const session = inject(SessionService);
    const router = inject(Router);
    await session.ensureLoaded();
    if (session.loadFailed()) {
      return new RedirectCommand(router.createUrlTree(['/indisponivel']), {
        skipLocationChange: true,
      });
    }
    return decide(session, router);
  };

/** Only signed-in users pass; everybody else goes to Entrar. */
export const authGuard = onLoadedSession((session, router) =>
  session.authenticated() ? true : router.createUrlTree(['/entrar']),
);

/** Signed-in users without a customer go to Completar cadastro. */
export const profileGuard = onLoadedSession((session, router) => {
  if (!session.authenticated()) return router.createUrlTree(['/entrar']);
  return session.customer() ? true : router.createUrlTree(['/completar-cadastro']);
});

/** Completar cadastro is for signed-in users who are not customers yet. */
export const unregisteredGuard = onLoadedSession((session, router) => {
  if (!session.authenticated()) return router.createUrlTree(['/entrar']);
  return session.customer() ? router.createUrlTree(['/inicio']) : true;
});

/** Entrar and Criar conta are for visitors; a signed-in user goes into the app. */
export const guestGuard = onLoadedSession((session, router) =>
  session.authenticated() ? router.createUrlTree(['/inicio']) : true,
);
