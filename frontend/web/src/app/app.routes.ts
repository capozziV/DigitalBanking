import { Routes } from '@angular/router';
import { authGuard, guestGuard, profileGuard } from './core/guards';
import { CompleteProfileComponent } from './pages/complete-profile/complete-profile.component';
import { SignInComponent } from './pages/sign-in/sign-in.component';
import { SignUpComponent } from './pages/sign-up/sign-up.component';
import { AppShellComponent } from './shell/app-shell.component';

// Screens inside the shell (Início, Extrato, Transferir, Avisos, Minha conta) arrive in Task 9;
// until then each path shows the placeholder.
const placeholder = () =>
  import('./shell/placeholder.component').then((module) => module.PlaceholderComponent);

export const routes: Routes = [
  { path: 'entrar', component: SignInComponent, canActivate: [guestGuard] },
  { path: 'criar-conta', component: SignUpComponent, canActivate: [guestGuard] },
  { path: 'completar-cadastro', component: CompleteProfileComponent, canActivate: [authGuard] },
  {
    path: '',
    component: AppShellComponent,
    canActivate: [profileGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      { path: 'inicio', loadComponent: placeholder, title: 'Início' },
      { path: 'extrato', loadComponent: placeholder, title: 'Extrato' },
      { path: 'transferir', loadComponent: placeholder, title: 'Transferir' },
      { path: 'avisos', loadComponent: placeholder, title: 'Avisos' },
      { path: 'minha-conta', loadComponent: placeholder, title: 'Minha conta' },
    ],
  },
  { path: '**', redirectTo: '' },
];
