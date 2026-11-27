import { act, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { BffError, bff, type BffInit } from '@/lib/bff-client';
import { fakeSession } from '@/test/fake-session';
import { renderWithProviders } from '@/test/render';
import { TransferPage } from './TransferPage';

vi.mock('@/lib/session', async () => (await import('@/test/fake-session')).fakeSessionModule());
vi.mock('@/lib/bff-client', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/lib/bff-client')>()),
  bff: vi.fn(),
}));

interface Deferred<T> {
  promise: Promise<T>;
  resolve: (value: T) => void;
  reject: (error: unknown) => void;
}

function deferred<T>(): Deferred<T> {
  let resolve!: (value: T) => void;
  let reject!: (error: unknown) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

interface Post {
  body: { recipientToken: string; amount: number };
  key: string | undefined;
  reply: Deferred<unknown>;
}

const bffMock = vi.mocked(bff);
let posts: Post[];
let polls: unknown[];
let recipientToken: string;
let recipientFailure: BffError | null;
let dailyLimit: { limit: number; used: number; available: number } | BffError;
let statusCalls: string[];

const user = () => userEvent.setup({ delay: null });

beforeEach(() => {
  sessionStorage.clear();
  fakeSession.reset();
  fakeSession.customer();
  posts = [];
  polls = [];
  statusCalls = [];
  recipientToken = 'token-1';
  recipientFailure = null;
  dailyLimit = { limit: 500_000, used: 25_000, available: 475_000 };
  vi.stubGlobal('matchMedia', (query: string) => ({
    matches: query.includes('reduce'),
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
  }));
  bffMock.mockImplementation(((path: string, init?: BffInit) => {
    if (path === '/bff/home') {
      if (dailyLimit instanceof BffError) return Promise.reject(dailyLimit);
      return Promise.resolve({ dailyLimit });
    }
    if (path.startsWith('/bff/transfers/recipient')) {
      if (recipientFailure) return Promise.reject(recipientFailure);
      return Promise.resolve({ recipientName: 'Ana Souza', recipientToken });
    }
    if (path === '/bff/transfers' && init?.method === 'POST') {
      const reply = deferred<unknown>();
      posts.push({
        body: init.body as Post['body'],
        key: init.headers?.['Idempotency-Key'],
        reply,
      });
      return reply.promise;
    }
    const status = /^\/bff\/transfers\/(.+)$/.exec(path);
    if (status) {
      statusCalls.push(status[1]);
      const next = polls.shift() ?? { id: status[1], status: 'PENDING' };
      return next instanceof Error ? Promise.reject(next) : Promise.resolve(next);
    }
    return Promise.reject(new Error(`unexpected call ${path}`));
  }) as unknown as typeof bff);
});

afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  sessionStorage.clear();
});

const keypad = async (amount: string) => {
  const u = user();
  for (const digit of amount) await u.click(screen.getByRole('button', { name: digit }));
};

async function lookUp(phone = '11987654321') {
  await user().type(screen.getByLabelText('Celular do destinatário'), phone);
  await user().click(screen.getByRole('button', { name: 'Próximo' }));
  await screen.findByRole('heading', { name: 'Quanto você quer enviar?' });
}

async function enterAmount(cents = '2500') {
  await keypad(cents);
  await waitFor(() => expect(screen.getByRole('button', { name: 'Continuar' })).toBeEnabled());
  await user().click(screen.getByRole('button', { name: 'Continuar' }));
  await screen.findByRole('heading', { name: 'Confirme o envio' });
}

async function reachConfirmation(phone?: string, cents?: string) {
  renderWithProviders(<TransferPage />);
  await lookUp(phone);
  await enterAmount(cents);
}

async function fillIn(phone?: string, cents?: string) {
  await lookUp(phone);
  await enterAmount(cents);
}

async function confirm() {
  await user().click(screen.getByRole('button', { name: 'Confirmar' }));
  await waitFor(() => expect(posts.length).toBeGreaterThan(0));
  return posts.at(-1)!;
}

const answer = (post: Post, value: unknown) => act(async () => post.reply.resolve(value));
const fail = (post: Post, error: unknown = new BffError(503, null)) =>
  act(async () => post.reply.reject(error));

const backToPhone = async () => {
  await user().click(screen.getByRole('button', { name: 'Voltar para Destinatário' }));
  await screen.findByLabelText('Celular do destinatário');
};

const tickPolls = (times = 1) =>
  act(async () => {
    await vi.advanceTimersByTimeAsync(1000 * times);
  });

describe('RecipientStep', () => {
  it('starts at the phone step and blocks an invalid phone', async () => {
    renderWithProviders(<TransferPage />);
    await user().type(screen.getByLabelText('Celular do destinatário'), '1187654321');
    expect(screen.getByRole('button', { name: 'Próximo' })).toBeDisabled();
    await user().tab();
    expect(screen.getByText(/Informe um celular com DDD/)).toBeVisible();
    await user().type(screen.getByLabelText('Celular do destinatário'), '{Enter}');
    expect(bffMock).not.toHaveBeenCalledWith(
      expect.stringContaining('/bff/transfers/recipient'),
      expect.anything(),
    );
  });

  it('masks the phone while typing and looks up the digits only', async () => {
    renderWithProviders(<TransferPage />);
    await user().type(screen.getByLabelText('Celular do destinatário'), '11987654321');
    expect(screen.getByLabelText('Celular do destinatário')).toHaveValue('(11) 98765-4321');
    await user().click(screen.getByRole('button', { name: 'Próximo' }));
    await screen.findByRole('heading', { name: 'Quanto você quer enviar?' });
    expect(bffMock).toHaveBeenCalledWith('/bff/transfers/recipient?phone=11987654321');
  });

  it('shows the full name of the recipient to be checked, then asks the amount', async () => {
    renderWithProviders(<TransferPage />);
    await lookUp();
    expect(screen.getByText('Ana Souza')).toBeVisible();
    expect(screen.queryByLabelText('Celular do destinatário')).toBeNull();
  });

  it('shows the BFF message when the phone has no account to receive', async () => {
    recipientFailure = new BffError(404, { detail: 'Este telefone não tem conta para receber' });
    renderWithProviders(<TransferPage />);
    await user().type(screen.getByLabelText('Celular do destinatário'), '11987654321');
    await user().click(screen.getByRole('button', { name: 'Próximo' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Este telefone não tem conta para receber',
    );
    expect(screen.getByLabelText('Celular do destinatário')).toBeVisible();
  });
});

describe('ConfirmStep', () => {
  it('confirms with the name and the amount before sending', async () => {
    await reachConfirmation();
    expect(screen.getByText('Ana Souza')).toBeVisible();
    expect(screen.getByText('Você vai enviar').nextElementSibling).toHaveTextContent('R$ 25,00');
    expect(screen.getByRole('button', { name: 'Confirmar' })).toBeEnabled();
    expect(posts).toHaveLength(0);
  });

  it('sends the amount in cents with the recipient token and an Idempotency-Key', async () => {
    await reachConfirmation();
    const post = await confirm();
    expect(post.body).toEqual({ recipientToken: 'token-1', amount: 2500 });
    expect(post.key).toMatch(/^[0-9a-f-]{36}$/);
    expect(bffMock).toHaveBeenCalledWith(
      '/bff/transfers',
      expect.objectContaining({ method: 'POST' }),
    );
  });

  it('two fast clicks send one POST with one Idempotency-Key', async () => {
    await reachConfirmation();
    const confirmButton = screen.getByRole('button', { name: 'Confirmar' });
    // Both clicks land before React re-renders the button as disabled.
    act(() => {
      confirmButton.click();
      confirmButton.click();
    });
    await waitFor(() => expect(posts).toHaveLength(1));
    await act(async () => undefined);
    expect(posts).toHaveLength(1);
    expect(Object.keys(sessionStorage)).toHaveLength(1);
    await answer(posts[0], { id: 't1', status: 'PENDING' });
    expect(screen.queryByTestId('resumed-notice')).toBeNull();
  });

  it('keeps Voltar disabled until the POST ends', async () => {
    await reachConfirmation();
    const post = await confirm();
    expect(screen.getByRole('button', { name: 'Voltar' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Voltar para Valor' })).toBeDisabled();
    await fail(post);
    expect(screen.getByRole('button', { name: 'Voltar' })).toBeEnabled();
    await user().click(screen.getByRole('button', { name: 'Voltar' }));
    expect(await screen.findByRole('heading', { name: 'Quanto você quer enviar?' })).toBeVisible();
  });

  it('says the send could not be confirmed and that trying here is safe', async () => {
    await reachConfirmation();
    await fail(await confirm(), new BffError(0, null));
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Não foi possível confirmar o envio. Tente de novo aqui ou confira o extrato.',
    );
  });

  describe('storage failures close the attempt', () => {
    it('sends nothing when reading the storage throws', async () => {
      await reachConfirmation();
      vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
        throw new Error('blocked');
      });
      await user().click(screen.getByRole('button', { name: 'Confirmar' }));
      expect(await screen.findByRole('alert')).toBeVisible();
      expect(posts).toHaveLength(0);
    });

    it('sends nothing when writing the storage throws', async () => {
      await reachConfirmation();
      vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
        throw new Error('quota');
      });
      await user().click(screen.getByRole('button', { name: 'Confirmar' }));
      expect(await screen.findByRole('alert')).toBeVisible();
      expect(posts).toHaveLength(0);
    });

    it('sends nothing when crypto.subtle is missing', async () => {
      await reachConfirmation();
      vi.spyOn(crypto, 'subtle', 'get').mockReturnValue(undefined as unknown as SubtleCrypto);
      await user().click(screen.getByRole('button', { name: 'Confirmar' }));
      expect(await screen.findByRole('alert')).toBeVisible();
      expect(posts).toHaveLength(0);
    });
  });
});

describe('steps bar', () => {
  it('goes back keeping the phone and the amount, and never forward', async () => {
    await reachConfirmation();
    expect(screen.queryByRole('button', { name: 'Voltar para Confirmar' })).toBeNull();
    await user().click(screen.getByRole('button', { name: 'Voltar para Valor' }));
    expect(screen.getByRole('status', { name: 'Valor' })).toHaveTextContent('R$ 25,00');
    await user().click(screen.getByRole('button', { name: 'Voltar para Destinatário' }));
    expect(screen.getByLabelText('Celular do destinatário')).toHaveValue('(11) 98765-4321');
  });
});

describe('Idempotency-Key of one intention', () => {
  it('keeps the same key when the user retries after a failure', async () => {
    await reachConfirmation();
    const first = await confirm();
    await fail(first);
    expect(await screen.findByRole('alert')).toBeVisible();
    const second = await confirm();
    expect(second.key).toBe(first.key);
    await answer(second, { id: 't1', status: 'PENDING' });
  });

  it('uses a new key when the amount changes after going back', async () => {
    await reachConfirmation();
    const first = await confirm();
    await fail(first);
    await user().click(screen.getByRole('button', { name: 'Voltar' }));
    await user().click(screen.getByRole('button', { name: 'Apagar' }));
    await keypad('3');
    await user().click(screen.getByRole('button', { name: 'Continuar' }));
    const second = await confirm();
    expect(second.body.amount).toBe(2503);
    expect(second.key).not.toBe(first.key);
  });

  it('keeps the key when going back and confirming the same amount', async () => {
    await reachConfirmation();
    const first = await confirm();
    await fail(first);
    await user().click(screen.getByRole('button', { name: 'Voltar' }));
    await user().click(screen.getByRole('button', { name: 'Continuar' }));
    const second = await confirm();
    expect(second.key).toBe(first.key);
  });

  it('keeps the key when the recipient is looked up again and gets another token', async () => {
    await reachConfirmation();
    const first = await confirm();
    await fail(first);
    await backToPhone();
    recipientToken = 'token-2';
    await user().click(screen.getByRole('button', { name: 'Próximo' }));
    await screen.findByRole('heading', { name: 'Quanto você quer enviar?' });
    await user().click(screen.getByRole('button', { name: 'Continuar' }));
    const retry = await confirm();
    expect(retry.body).toEqual({ recipientToken: 'token-2', amount: 2500 });
    expect(retry.key).toBe(first.key);
  });

  it('keeps the key when the recipient token expired after a lost answer', async () => {
    await reachConfirmation();
    const first = await confirm();
    await fail(first);
    const expired = await confirm();
    await fail(
      expired,
      new BffError(400, {
        message: 'Destinatário expirado. Busque o telefone de novo.',
      } as never),
    );
    expect(await screen.findByRole('alert')).toHaveTextContent('Destinatário expirado');
    await backToPhone();
    recipientToken = 'token-2';
    await user().click(screen.getByRole('button', { name: 'Próximo' }));
    await screen.findByRole('heading', { name: 'Quanto você quer enviar?' });
    await user().click(screen.getByRole('button', { name: 'Continuar' }));
    expect((await confirm()).key).toBe(first.key);
  });

  it('uses a new key for another recipient', async () => {
    await reachConfirmation();
    const first = await confirm();
    await fail(first);
    await backToPhone();
    await user().clear(screen.getByLabelText('Celular do destinatário'));
    // Same token on purpose: the phone tells the recipient, not the token.
    await user().type(screen.getByLabelText('Celular do destinatário'), '21998765432');
    await user().click(screen.getByRole('button', { name: 'Próximo' }));
    await screen.findByRole('heading', { name: 'Quanto você quer enviar?' });
    await user().click(screen.getByRole('button', { name: 'Continuar' }));
    expect((await confirm()).key).not.toBe(first.key);
  });

  it('keeps the key when the user leaves the page after a lost answer and returns', async () => {
    const first = await (async () => {
      const view = renderWithProviders(<TransferPage />);
      await fillIn();
      const post = await confirm();
      await fail(post);
      view.unmount();
      return post;
    })();
    renderWithProviders(<TransferPage />);
    await fillIn();
    expect((await confirm()).key).toBe(first.key);
  });

  it('never gives the key to another user of the tab, and keeps it for its owner', async () => {
    const view = renderWithProviders(<TransferPage />);
    await fillIn();
    const first = await confirm();
    await fail(first);
    view.unmount();

    fakeSession.customer({ email: 'bia@example.test' });
    const other = renderWithProviders(<TransferPage />);
    await fillIn();
    const second = await confirm();
    expect(second.key).not.toBe(first.key);
    await fail(second);
    other.unmount();

    fakeSession.customer();
    renderWithProviders(<TransferPage />);
    await fillIn();
    expect((await confirm()).key).toBe(first.key);
  });

  it('keeps the key when the user leaves before the final status', async () => {
    const view = renderWithProviders(<TransferPage />);
    await fillIn();
    const first = await confirm();
    await answer(first, { id: 't1', status: 'PENDING' });
    expect(await screen.findByText('Processando…')).toBeVisible();
    view.unmount();
    renderWithProviders(<TransferPage />);
    await fillIn();
    expect((await confirm()).key).toBe(first.key);
  });

  it('keeps the key when the final answer arrives after the user left the page', async () => {
    const view = renderWithProviders(<TransferPage />);
    await fillIn();
    const first = await confirm();
    view.unmount();
    await answer(first, { id: 't1', status: 'COMPLETED' });
    // Nobody saw that result, and nobody is left to read its reason.
    expect(statusCalls).toEqual([]);
    expect(Object.keys(sessionStorage)).toHaveLength(1);
    renderWithProviders(<TransferPage />);
    await fillIn();
    expect((await confirm()).key).toBe(first.key);
  });

  describe('reload on a pending attempt', () => {
    it('resumes with the same key and shows the status', async () => {
      const view = renderWithProviders(<TransferPage />);
      await fillIn();
      const first = await confirm();
      await answer(first, { id: 't1', status: 'PENDING' });
      await screen.findByText('Processando…');
      view.unmount();

      renderWithProviders(<TransferPage />);
      await fillIn();
      const second = await confirm();
      expect(second.key).toBe(first.key);
      polls.push({ id: 't1', status: 'COMPLETED' });
      await answer(second, { id: 't1', status: 'COMPLETED' });
      expect(await screen.findByTestId('resumed-notice')).toHaveTextContent(
        'Você já tinha confirmado uma transferência igual sem ver o resultado. Este é o resultado dela: não houve um segundo envio. Para enviar mais uma, use Nova transferência.',
      );
      expect(screen.getByTestId('resumed-notice')).toHaveAttribute('role', 'status');
      expect(await screen.findByText('Concluída')).toBeVisible();
    });
  });
});

describe('TransferResult', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
  });

  async function confirmAndGetPending() {
    await reachConfirmation();
    await answer(await confirm(), { id: 't1', status: 'PENDING' });
    await screen.findByText('Processando…');
  }

  it('polls every second, shows the transfer as completed, then stops', async () => {
    await confirmAndGetPending();
    expect(statusCalls).toEqual([]);
    await tickPolls();
    expect(statusCalls).toEqual(['t1']);
    polls.push({ id: 't1', status: 'COMPLETED' });
    await tickPolls();
    expect(await screen.findByText('Transferência concluída')).toBeVisible();
    const total = statusCalls.length;
    await tickPolls(5);
    expect(statusCalls).toHaveLength(total);
  });

  it('shows the amount and the recipient on the result', async () => {
    await confirmAndGetPending();
    expect(screen.getByRole('region', { name: 'Resultado da transferência' })).toHaveTextContent(
      'R$ 25,00',
    );
    expect(screen.getByRole('region', { name: 'Resultado da transferência' })).toHaveTextContent(
      'para Ana Souza',
    );
  });

  it('shows the reason of a refused transfer and stops polling', async () => {
    await confirmAndGetPending();
    polls.push({ id: 't1', status: 'FAILED', reason: 'Limite diário excedido' });
    await tickPolls();
    const result = await screen.findByRole('region', { name: 'Resultado da transferência' });
    expect(within(result).getByText('Transferência recusada')).toBeVisible();
    expect(result).toHaveTextContent('Limite diário excedido');
    const total = statusCalls.length;
    await tickPolls(5);
    expect(statusCalls).toHaveLength(total);
  });

  it('shows the reason of a returned (compensated) transfer', async () => {
    await confirmAndGetPending();
    polls.push({ id: 't1', status: 'COMPENSATED', reason: 'Conta de destino encerrada' });
    await tickPolls();
    expect(await screen.findByText('Transferência devolvida')).toBeVisible();
    expect(screen.getByText(/Conta de destino encerrada/)).toBeVisible();
  });

  it('keeps polling after a failed poll', async () => {
    await confirmAndGetPending();
    polls.push(new BffError(503, null));
    await tickPolls();
    polls.push({ id: 't1', status: 'COMPLETED' });
    await tickPolls();
    expect(await screen.findByText('Transferência concluída')).toBeVisible();
  });

  it('gives up after 30 s with "Ainda processando"', async () => {
    await confirmAndGetPending();
    await tickPolls(29);
    expect(screen.queryByText(/Ainda processando/)).toBeNull();
    await tickPolls();
    expect(screen.getByText(/Ainda processando/)).toBeVisible();
    const total = statusCalls.length;
    await tickPolls(5);
    expect(statusCalls).toHaveLength(total);
    expect(screen.getByRole('button', { name: 'Nova transferência' })).toBeVisible();
  });

  it('fetches the reason right away when the POST already answers a final status', async () => {
    await reachConfirmation();
    polls.push({ id: 't1', status: 'FAILED', reason: 'Limite diário excedido' });
    await answer(await confirm(), { id: 't1', status: 'FAILED' });
    expect(await screen.findByText('Transferência recusada')).toBeVisible();
    expect(await screen.findByText(/Limite diário excedido/)).toBeVisible();
    expect(statusCalls).toEqual(['t1']);
    await tickPolls(5);
    expect(statusCalls).toEqual(['t1']);
  });

  it('keeps the key for "Nova transferência" while the status is still not final', async () => {
    await confirmAndGetPending();
    const firstKey = posts[0].key;
    await tickPolls(30);
    await user().click(screen.getByRole('button', { name: 'Nova transferência' }));
    await fillIn();
    expect((await confirm()).key).toBe(firstKey);
  });

  describe.each(['COMPLETED', 'FAILED', 'COMPENSATED'])('final status %s', (status) => {
    it('settles the attempt: a new key for the same transfer after the POST answer', async () => {
      await reachConfirmation();
      polls.push({ id: 't1', status });
      const first = await confirm();
      await answer(first, { id: 't1', status });
      await user().click(await screen.findByRole('button', { name: 'Nova transferência' }));
      await fillIn();
      expect((await confirm()).key).not.toBe(first.key);
    });

    it('settles the attempt: a new key after polling showed it and the user left', async () => {
      const view = renderWithProviders(<TransferPage />);
      await fillIn();
      const first = await confirm();
      await answer(first, { id: 't1', status: 'PENDING' });
      await screen.findByText('Processando…');
      polls.push({ id: 't1', status });
      await tickPolls();
      await screen.findByRole('button', { name: 'Nova transferência' });
      view.unmount();
      renderWithProviders(<TransferPage />);
      await fillIn();
      expect((await confirm()).key).not.toBe(first.key);
    });
  });

  it('pending does not call settle: the key stays in the storage', async () => {
    await confirmAndGetPending();
    expect(Object.keys(sessionStorage)).toHaveLength(1);
  });

  describe('resumed attempt notice', () => {
    it('warns when "Nova transferência" after "Ainda processando" sends the same transfer', async () => {
      await confirmAndGetPending();
      await tickPolls(30);
      expect(screen.queryByTestId('resumed-notice')).toBeNull();
      await user().click(screen.getByRole('button', { name: 'Nova transferência' }));
      await fillIn();
      polls.push({ id: 't1', status: 'COMPLETED' });
      await answer(await confirm(), { id: 't1', status: 'COMPLETED' });
      expect(await screen.findByTestId('resumed-notice')).toBeVisible();
    });

    it('does not warn on the first transfer', async () => {
      await reachConfirmation();
      polls.push({ id: 't1', status: 'COMPLETED' });
      await answer(await confirm(), { id: 't1', status: 'COMPLETED' });
      await screen.findByText('Transferência concluída');
      expect(screen.queryByTestId('resumed-notice')).toBeNull();
    });

    it('does not warn on an equal transfer after a final status was seen', async () => {
      await reachConfirmation();
      polls.push({ id: 't1', status: 'COMPLETED' });
      await answer(await confirm(), { id: 't1', status: 'COMPLETED' });
      await user().click(await screen.findByRole('button', { name: 'Nova transferência' }));
      await fillIn();
      polls.push({ id: 't2', status: 'COMPLETED' });
      await answer(await confirm(), { id: 't2', status: 'COMPLETED' });
      await screen.findByText('Transferência concluída');
      expect(screen.queryByTestId('resumed-notice')).toBeNull();
    });
  });

  it('starts a new transfer with a new Idempotency-Key', async () => {
    await reachConfirmation();
    polls.push({ id: 't1', status: 'COMPLETED' });
    const first = await confirm();
    await answer(first, { id: 't1', status: 'COMPLETED' });
    await user().click(await screen.findByRole('button', { name: 'Nova transferência' }));
    await lookUp();
    await enterAmount('100');
    const next = await confirm();
    expect(next.key).toMatch(/^[0-9a-f-]{36}$/);
    expect(next.key).not.toBe(first.key);
  });

  it('links to the statement and to the start', async () => {
    await reachConfirmation();
    polls.push({ id: 't1', status: 'COMPLETED' });
    await answer(await confirm(), { id: 't1', status: 'COMPLETED' });
    expect(await screen.findByRole('link', { name: 'Ver extrato' })).toHaveAttribute(
      'href',
      '/extrato',
    );
    expect(screen.getByRole('link', { name: 'Voltar ao início' })).toHaveAttribute(
      'href',
      '/inicio',
    );
  });
});

describe('daily limit', () => {
  it('reads the available amount from the home and blocks an amount above it', async () => {
    dailyLimit = { limit: 500_000, used: 490_000, available: 10_000 };
    renderWithProviders(<TransferPage />);
    await lookUp();
    expect(await screen.findByText(/Disponível hoje: R\$\s100,00/)).toBeVisible();
    await keypad('10001');
    expect(screen.getByRole('button', { name: 'Continuar' })).toBeDisabled();
  });

  it('does not block the transfer when the limit cannot be read', async () => {
    dailyLimit = new BffError(503, null);
    renderWithProviders(<TransferPage />);
    await lookUp();
    expect(await screen.findByText(/Não foi possível ler o limite de hoje/)).toBeVisible();
    await keypad('5');
    expect(screen.getByRole('button', { name: 'Continuar' })).toBeEnabled();
  });
});
