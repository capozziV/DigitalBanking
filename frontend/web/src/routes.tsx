import { Navigate, type RouteObject } from 'react-router';
import { CompleteProfilePage } from '@/pages/complete-profile/CompleteProfilePage';
import { SignInPage } from '@/pages/sign-in/SignInPage';
import { SignUpPage } from '@/pages/sign-up/SignUpPage';
import { UnavailablePage } from '@/pages/unavailable/UnavailablePage';
import { AppShell } from '@/shell/AppShell';
import { CustomerOnly, GuestOnly, UnregisteredOnly } from '@/shell/guards';
import { RootLayout, type RouteHandle } from '@/shell/RootLayout';

const titled = (title: string): RouteHandle => ({ title });

/** The paths are the same as the Angular app's: nginx and the e2e tests depend on them. */
export const routes: RouteObject[] = [
  {
    element: <RootLayout />,
    children: [
      { path: 'indisponivel', element: <UnavailablePage />, handle: titled('Indisponível') },
      {
        element: <GuestOnly />,
        children: [
          { path: 'entrar', element: <SignInPage />, handle: titled('Entrar') },
          { path: 'criar-conta', element: <SignUpPage />, handle: titled('Criar conta') },
        ],
      },
      {
        path: 'completar-cadastro',
        element: (
          <UnregisteredOnly>
            <CompleteProfilePage />
          </UnregisteredOnly>
        ),
        handle: titled('Completar cadastro'),
      },
      {
        path: '/',
        element: (
          <CustomerOnly>
            <AppShell />
          </CustomerOnly>
        ),
        children: [
          { index: true, element: <Navigate to="/inicio" replace /> },
          {
            path: 'inicio',
            handle: titled('Início'),
            lazy: async () => ({ Component: (await import('@/pages/home/HomePage')).HomePage }),
          },
          {
            path: 'extrato',
            handle: titled('Extrato'),
            lazy: async () => ({
              Component: (await import('@/pages/statement/StatementPage')).StatementPage,
            }),
          },
          {
            path: 'transferir',
            handle: titled('Transferir'),
            lazy: async () => ({
              Component: (await import('@/pages/transfer/TransferPage')).TransferPage,
            }),
          },
          { path: 'avisos', element: <Navigate to="/notificacoes" replace /> },
          {
            path: 'notificacoes',
            handle: titled('Notificações'),
            lazy: async () => ({
              Component: (await import('@/pages/notifications/NotificationsPage'))
                .NotificationsPage,
            }),
          },
          {
            path: 'minha-conta',
            handle: titled('Minha conta'),
            lazy: async () => ({
              Component: (await import('@/pages/account/AccountPage')).AccountPage,
            }),
          },
        ],
      },
      { path: '*', element: <Navigate to="/" replace /> },
    ],
  },
];
