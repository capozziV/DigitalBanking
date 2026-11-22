import { Routes } from '@angular/router';
import { guestGuard, profileGuard, unregisteredGuard } from './core/guards';
import { CompleteProfileComponent } from './pages/complete-profile/complete-profile.component';
import { SignInComponent } from './pages/sign-in/sign-in.component';
import { SignUpComponent } from './pages/sign-up/sign-up.component';
import { UnavailableComponent } from './pages/unavailable/unavailable.component';
import { AppShellComponent } from './shell/app-shell.component';

export const routes: Routes = [
  { path: 'indisponivel', component: UnavailableComponent },
  { path: 'entrar', component: SignInComponent, canActivate: [guestGuard] },
  { path: 'criar-conta', component: SignUpComponent, canActivate: [guestGuard] },
  {
    path: 'completar-cadastro',
    component: CompleteProfileComponent,
    canActivate: [unregisteredGuard],
  },
  {
    path: '',
    component: AppShellComponent,
    canActivate: [profileGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      {
        path: 'inicio',
        loadComponent: () =>
          import('./pages/home/home.component').then((module) => module.HomeComponent),
        title: 'Início',
      },
      {
        path: 'extrato',
        loadComponent: () =>
          import('./pages/statement/statement.component').then(
            (module) => module.StatementComponent,
          ),
        title: 'Extrato',
      },
      {
        path: 'transferir',
        loadComponent: () =>
          import('./pages/transfer/transfer.component').then((module) => module.TransferComponent),
        title: 'Transferir',
      },
      {
        path: 'avisos',
        redirectTo: 'notificacoes',
      },
      {
        path: 'notificacoes',
        loadComponent: () =>
          import('./pages/notifications/notifications.component').then(
            (module) => module.NotificationsComponent,
          ),
        title: 'Notificações',
      },
      {
        path: 'minha-conta',
        loadComponent: () =>
          import('./pages/account/account.component').then((module) => module.AccountComponent),
        title: 'Minha conta',
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
