import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { routes } from '@/routes';

/** Renders the app's real route table at `path` and returns the router to inspect navigation. */
export function renderRoutes(path: string, onNavigate?: (pathname: string) => void) {
  const router = createMemoryRouter(routes, { initialEntries: [path] });
  if (onNavigate) router.subscribe((state) => onNavigate(state.location.pathname));
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const view = render(
    <QueryClientProvider client={client}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
  return { router, ...view };
}
