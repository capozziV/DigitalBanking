import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessionService } from './session.service';

/** Only signed-in users pass; everybody else goes to Entrar. */
export const authGuard: CanActivateFn = async () => {
  const session = inject(SessionService);
  const router = inject(Router);
  await session.ensureLoaded();
  return session.authenticated() ? true : router.createUrlTree(['/entrar']);
};

/** Signed-in users without a customer go to Completar cadastro. */
export const profileGuard: CanActivateFn = async () => {
  const session = inject(SessionService);
  const router = inject(Router);
  await session.ensureLoaded();
  if (!session.authenticated()) return router.createUrlTree(['/entrar']);
  return session.customer() ? true : router.createUrlTree(['/completar-cadastro']);
};

/** Entrar and Criar conta are for visitors; a signed-in user goes into the app. */
export const guestGuard: CanActivateFn = async () => {
  const session = inject(SessionService);
  const router = inject(Router);
  await session.ensureLoaded();
  return session.authenticated() ? router.createUrlTree(['/inicio']) : true;
};
