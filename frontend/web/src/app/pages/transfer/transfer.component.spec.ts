import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
  TestRequest,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { SessionService } from '../../core/session.service';
import { setInput, submit, tick } from '../../testing/dom';
import { TransferComponent } from './transfer.component';

describe('TransferComponent', () => {
  let fixture: ComponentFixture<TransferComponent>;
  let http: HttpTestingController;
  const el = () => fixture.nativeElement as HTMLElement;
  const button = (text: string) =>
    Array.from(el().querySelectorAll('button')).find((b) =>
      b.textContent?.includes(text),
    ) as HTMLButtonElement;

  async function settle(): Promise<void> {
    await tick();
    await fixture.whenStable();
  }

  async function create(email = 'ana@example.com'): Promise<void> {
    TestBed.configureTestingModule({
      imports: [TransferComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    // Only the interval timers are faked: the polling is driven by hand, `tick` keeps working.
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
    http = TestBed.inject(HttpTestingController);
    TestBed.inject(SessionService).email.set(email);
    fixture = TestBed.createComponent(TransferComponent);
    await fixture.whenStable();
  }

  async function lookUp(phone = '(11) 98765-4321', token = 'token-1'): Promise<void> {
    setInput(fixture, 'phone', phone);
    await fixture.whenStable();
    submit(fixture);
    const req = http.expectOne((r) => r.url === '/bff/transfers/recipient');
    expect(req.request.params.get('phone')).toBe(phone.replace(/\D/g, ''));
    req.flush({ recipientName: 'Ana Souza', recipientToken: token });
    await settle();
  }

  async function enterAmount(value: string): Promise<void> {
    setInput(fixture, 'amount', value);
    await fixture.whenStable();
    submit(fixture);
    await settle();
  }

  /** Goes through the three steps up to the confirmation. */
  async function reachConfirmation(): Promise<void> {
    await create();
    await lookUp();
    await enterAmount('25,00');
  }

  /** The POSTs of "Confirmar": they may leave some macrotasks after the click. */
  async function sentTransfers(count: number): Promise<TestRequest[]> {
    const sent: TestRequest[] = [];
    for (let turn = 0; turn < 500 && sent.length < count; turn++) {
      sent.push(...http.match('/bff/transfers'));
      if (sent.length < count) await tick();
    }
    expect(sent).toHaveLength(count);
    return sent;
  }

  async function confirmTransfer(): Promise<TestRequest> {
    button('Confirmar').click();
    return (await sentTransfers(1))[0];
  }

  const keyOf = (request: TestRequest) => request.request.headers.get('Idempotency-Key');

  /** Confirms and loses the answer: the transfer may or may not exist on the server. */
  async function confirmAndLoseTheAnswer(): Promise<string | null> {
    const request = await confirmTransfer();
    request.flush({}, { status: 503, statusText: 'Unavailable' });
    await settle();
    return keyOf(request);
  }

  async function fillIn(amount = '25,00', phone?: string, token?: string): Promise<void> {
    await lookUp(phone, token);
    await enterAmount(amount);
  }

  async function backToPhone(): Promise<void> {
    button('Voltar').click();
    await settle();
    button('Voltar').click();
    await settle();
  }

  /** The router destroys the page when the user leaves it, and builds another one on return. */
  async function leaveAndReturn(): Promise<void> {
    fixture.destroy();
    fixture = TestBed.createComponent(TransferComponent);
    await fixture.whenStable();
  }

  afterEach(() => {
    vi.useRealTimers();
    http.verify();
    sessionStorage.clear();
  });

  it('starts at the phone step and blocks an invalid phone', async () => {
    await create();
    expect(el().querySelector('input[name="phone"]')).not.toBeNull();
    setInput(fixture, 'phone', '1187654321');
    await fixture.whenStable();
    submit(fixture);
    http.expectNone((r) => r.url === '/bff/transfers/recipient');
  });

  it('shows the full name of the recipient to be checked, then asks the amount', async () => {
    await create();
    await lookUp();
    expect(el().textContent).toContain('Ana Souza');
    expect(el().querySelector('input[name="amount"]')).not.toBeNull();
    expect(el().querySelector('input[name="phone"]')).toBeNull();
  });

  it('confirms with the name and the amount before sending', async () => {
    await reachConfirmation();
    expect(el().textContent).toContain('Ana Souza');
    expect(el().textContent).toContain('R$ 25,00');
    expect(button('Confirmar')).toBeTruthy();
    http.expectNone('/bff/transfers');
  });

  it('refuses an amount that is not a positive value', async () => {
    await create();
    await lookUp();
    await enterAmount('0');
    expect(el().querySelector('input[name="amount"]')).not.toBeNull();
    expect(el().textContent).not.toContain('Confirmar');
  });

  it('shows the BFF message when the phone has no account to receive', async () => {
    await create();
    setInput(fixture, 'phone', '11987654321');
    await fixture.whenStable();
    submit(fixture);
    http
      .expectOne((r) => r.url === '/bff/transfers/recipient')
      .flush(
        { detail: 'Este telefone não tem conta para receber' },
        { status: 404, statusText: 'Not Found' },
      );
    await settle();
    expect(el().querySelector('[role="alert"]')?.textContent).toContain(
      'Este telefone não tem conta para receber',
    );
    expect(el().querySelector('input[name="phone"]')).not.toBeNull();
  });

  it('sends the amount in cents with the recipient token and an Idempotency-Key', async () => {
    await reachConfirmation();
    const req = await confirmTransfer();
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ recipientToken: 'token-1', amount: 2500 });
    expect(req.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/);
    req.flush({ id: 't1', status: 'PENDING' });
    await settle();
  });

  it('repeats the same Idempotency-Key on a double click on Confirmar', async () => {
    await reachConfirmation();
    const confirm = button('Confirmar');
    confirm.click();
    confirm.click();
    const requests = await sentTransfers(2);
    const key = requests[0].request.headers.get('Idempotency-Key');
    expect(key).toBeTruthy();
    expect(requests[1].request.headers.get('Idempotency-Key')).toBe(key);
    requests[0].flush({ id: 't1', status: 'PENDING' });
    requests[1].flush({ id: 't1', status: 'PENDING' });
  });

  it('keeps the same Idempotency-Key when the user retries after a failure', async () => {
    await reachConfirmation();
    const first = await confirmTransfer();
    const key = first.request.headers.get('Idempotency-Key');
    first.flush({}, { status: 503, statusText: 'Unavailable' });
    await settle();
    expect(el().querySelector('[role="alert"]')).not.toBeNull();
    const second = await confirmTransfer();
    expect(second.request.headers.get('Idempotency-Key')).toBe(key);
    second.flush({ id: 't1', status: 'COMPLETED' });
    await settle();
    http.expectOne('/bff/transfers/t1').flush({ id: 't1', status: 'COMPLETED' });
  });

  it('uses a new Idempotency-Key when the amount changes after going back', async () => {
    await reachConfirmation();
    const first = await confirmTransfer();
    const key = first.request.headers.get('Idempotency-Key');
    first.flush({}, { status: 503, statusText: 'Unavailable' });
    await settle();
    button('Voltar').click();
    await settle();
    await enterAmount('30,00');
    const second = await confirmTransfer();
    expect(second.request.body).toEqual({ recipientToken: 'token-1', amount: 3000 });
    expect(second.request.headers.get('Idempotency-Key')).not.toBe(key);
    second.flush({ id: 't1', status: 'PENDING' });
  });

  it('keeps the Idempotency-Key when going back and confirming the same amount', async () => {
    await reachConfirmation();
    const first = await confirmTransfer();
    const key = first.request.headers.get('Idempotency-Key');
    first.flush({}, { status: 503, statusText: 'Unavailable' });
    await settle();
    button('Voltar').click();
    await settle();
    await enterAmount('25,00');
    const second = await confirmTransfer();
    expect(second.request.headers.get('Idempotency-Key')).toBe(key);
    second.flush({ id: 't1', status: 'PENDING' });
  });

  it('fetches the reason right away when the POST already answers a final status', async () => {
    await reachConfirmation();
    (await confirmTransfer()).flush({ id: 't1', status: 'FAILED' });
    await settle();
    http
      .expectOne('/bff/transfers/t1')
      .flush({ id: 't1', status: 'FAILED', reason: 'Limite diário excedido' });
    await settle();
    expect(el().textContent).toContain('Recusada');
    expect(el().textContent).toContain('Limite diário excedido');
    vi.advanceTimersByTime(5000);
    http.expectNone('/bff/transfers/t1');
  });

  describe('attempt of a transfer', () => {
    it('keeps the key when the recipient is looked up again and gets another token', async () => {
      await reachConfirmation();
      const key = await confirmAndLoseTheAnswer();
      await backToPhone();
      await fillIn('25,00', '11987654321', 'token-2');
      const retry = await confirmTransfer();
      expect(retry.request.body).toEqual({ recipientToken: 'token-2', amount: 2500 });
      expect(keyOf(retry)).toBe(key);
      retry.flush({ id: 't1', status: 'PENDING' });
    });

    it('keeps the key when the recipient token expired after a lost answer', async () => {
      await reachConfirmation();
      const key = await confirmAndLoseTheAnswer();
      const expired = await confirmTransfer();
      expired.flush(
        { code: 'invalid_recipient', message: 'Destinatário expirado. Busque o telefone de novo.' },
        { status: 400, statusText: 'Bad Request' },
      );
      await settle();
      expect(el().textContent).toContain('Destinatário expirado');
      await backToPhone();
      await fillIn('25,00', '(11) 98765-4321', 'token-2');
      const retry = await confirmTransfer();
      expect(keyOf(retry)).toBe(key);
      retry.flush({ id: 't1', status: 'PENDING' });
    });

    it('uses a new key for another recipient', async () => {
      await reachConfirmation();
      const key = await confirmAndLoseTheAnswer();
      await backToPhone();
      // Same token on purpose: the phone tells the recipient, not the token.
      await fillIn('25,00', '(21) 99876-5432', 'token-1');
      const other = await confirmTransfer();
      expect(keyOf(other)).not.toBe(key);
      other.flush({ id: 't2', status: 'PENDING' });
    });

    it('finds the first key again when the user comes back to the first amount', async () => {
      await reachConfirmation();
      const key = await confirmAndLoseTheAnswer();
      button('Voltar').click();
      await settle();
      await enterAmount('30,00');
      await confirmAndLoseTheAnswer();
      button('Voltar').click();
      await settle();
      await enterAmount('25,00');
      const retry = await confirmTransfer();
      expect(keyOf(retry)).toBe(key);
      retry.flush({ id: 't1', status: 'PENDING' });
    });

    it('keeps the key when the user leaves the page after a lost answer and returns', async () => {
      await reachConfirmation();
      const key = await confirmAndLoseTheAnswer();
      await leaveAndReturn();
      await fillIn('25,00', '(11) 98765-4321', 'token-2');
      const retry = await confirmTransfer();
      expect(keyOf(retry)).toBe(key);
      retry.flush({ id: 't1', status: 'PENDING' });
    });

    it('keeps the key when the tab reloads after a lost answer', async () => {
      await reachConfirmation();
      const key = await confirmAndLoseTheAnswer();
      TestBed.resetTestingModule();
      await reachConfirmation();
      const retry = await confirmTransfer();
      expect(keyOf(retry)).toBe(key);
      retry.flush({ id: 't1', status: 'PENDING' });
    });

    it('keeps the key when the same user signs in again', async () => {
      await reachConfirmation();
      const key = await confirmAndLoseTheAnswer();
      const session = TestBed.inject(SessionService);
      session.clear();
      session.email.set('ana@example.com');
      await leaveAndReturn();
      await fillIn();
      const retry = await confirmTransfer();
      expect(keyOf(retry)).toBe(key);
      retry.flush({ id: 't1', status: 'PENDING' });
    });

    it('never gives the key to another user of the tab, and keeps it for its owner', async () => {
      await reachConfirmation();
      const key = await confirmAndLoseTheAnswer();
      const session = TestBed.inject(SessionService);
      session.clear();
      session.email.set('bia@example.com');
      await leaveAndReturn();
      await fillIn();
      expect(await confirmAndLoseTheAnswer()).not.toBe(key);
      session.clear();
      session.email.set('ana@example.com');
      await leaveAndReturn();
      await fillIn();
      const retry = await confirmTransfer();
      expect(keyOf(retry)).toBe(key);
      retry.flush({ id: 't1', status: 'PENDING' });
    });

    it('does not leave the e-mail or the phone readable in the tab storage', async () => {
      await reachConfirmation();
      await confirmAndLoseTheAnswer();
      const stored = JSON.stringify({ ...sessionStorage });
      expect(sessionStorage.length).toBe(1);
      expect(stored).not.toContain('ana@example.com');
      expect(stored).not.toContain('987654321');
    });

    it('keeps the key when the user leaves before the final status', async () => {
      await reachConfirmation();
      const first = await confirmTransfer();
      first.flush({ id: 't1', status: 'PENDING' });
      await settle();
      await leaveAndReturn();
      await fillIn();
      const retry = await confirmTransfer();
      expect(keyOf(retry)).toBe(keyOf(first));
      retry.flush({ id: 't1', status: 'PENDING' });
    });

    it('keeps the key when the final answer arrives after the user left the page', async () => {
      await reachConfirmation();
      const first = await confirmTransfer();
      await leaveAndReturn();
      first.flush({ id: 't1', status: 'COMPLETED' });
      await settle();
      // Nobody saw that result, and nobody is left to read its reason.
      http.expectNone('/bff/transfers/t1');
      await fillIn();
      const retry = await confirmTransfer();
      expect(keyOf(retry)).toBe(keyOf(first));
      retry.flush({ id: 't1', status: 'PENDING' });
    });

    it('keeps the key for "Nova transferência" while the status is still not final', async () => {
      await reachConfirmation();
      const first = await confirmTransfer();
      first.flush({ id: 't1', status: 'PENDING' });
      await settle();
      vi.advanceTimersByTime(30_000);
      await settle();
      http.match('/bff/transfers/t1');
      button('Nova transferência').click();
      await settle();
      await fillIn();
      const retry = await confirmTransfer();
      expect(keyOf(retry)).toBe(keyOf(first));
      retry.flush({ id: 't1', status: 'PENDING' });
    });

    it('a late answer of a double click does not drop the key of the next transfer', async () => {
      await reachConfirmation();
      const confirm = button('Confirmar');
      confirm.click();
      confirm.click();
      const [late, answered] = await sentTransfers(2);
      answered.flush({ id: 't1', status: 'COMPLETED' });
      await settle();
      http.expectOne('/bff/transfers/t1').flush({ id: 't1', status: 'COMPLETED' });
      await settle();
      button('Nova transferência').click();
      await settle();
      await fillIn();
      const key = await confirmAndLoseTheAnswer();
      late.flush({ id: 't1', status: 'COMPLETED' });
      await settle();
      http.expectOne('/bff/transfers/t1').flush({ id: 't1', status: 'COMPLETED' });
      await leaveAndReturn();
      await fillIn();
      const retry = await confirmTransfer();
      expect(keyOf(retry)).toBe(key);
      retry.flush({ id: 't2', status: 'PENDING' });
    });

    for (const status of ['COMPLETED', 'FAILED', 'COMPENSATED']) {
      it(`uses a new key for the same transfer after ${status} in the POST answer`, async () => {
        await reachConfirmation();
        const first = await confirmTransfer();
        first.flush({ id: 't1', status });
        await settle();
        http.expectOne('/bff/transfers/t1').flush({ id: 't1', status });
        await settle();
        button('Nova transferência').click();
        await settle();
        await fillIn();
        const next = await confirmTransfer();
        expect(keyOf(next)).not.toBe(keyOf(first));
        next.flush({ id: 't2', status: 'PENDING' });
      });

      it(`uses a new key after leaving a transfer that polling showed ${status}`, async () => {
        await reachConfirmation();
        const first = await confirmTransfer();
        first.flush({ id: 't1', status: 'PENDING' });
        await settle();
        vi.advanceTimersByTime(1000);
        http.expectOne('/bff/transfers/t1').flush({ id: 't1', status });
        await settle();
        await leaveAndReturn();
        await fillIn();
        const next = await confirmTransfer();
        expect(keyOf(next)).not.toBe(keyOf(first));
        next.flush({ id: 't2', status: 'PENDING' });
      });
    }
  });

  describe('status polling', () => {
    async function confirmAndStartPolling(): Promise<void> {
      await reachConfirmation();
      (await confirmTransfer()).flush({ id: 't1', status: 'PENDING' });
      await settle();
    }

    const poll = async (status: string, reason: string | null = null) => {
      vi.advanceTimersByTime(1000);
      http.expectOne('/bff/transfers/t1').flush({ id: 't1', status, reason });
      await settle();
    };

    it('polls every second and shows the transfer as completed, then stops', async () => {
      await confirmAndStartPolling();
      expect(el().textContent).toContain('Processando');
      http.expectNone('/bff/transfers/t1');
      await poll('PENDING');
      await poll('COMPLETED');
      expect(el().textContent).toContain('Concluída');
      vi.advanceTimersByTime(5000);
      http.expectNone('/bff/transfers/t1');
    });

    it('shows the reason of a refused transfer and stops polling', async () => {
      await confirmAndStartPolling();
      await poll('FAILED', 'Limite diário excedido');
      expect(el().textContent).toContain('Recusada');
      expect(el().textContent).toContain('Limite diário excedido');
      vi.advanceTimersByTime(5000);
      http.expectNone('/bff/transfers/t1');
    });

    it('shows the reason of a returned (compensated) transfer', async () => {
      await confirmAndStartPolling();
      await poll('COMPENSATED', 'Conta de destino encerrada');
      expect(el().textContent).toContain('Devolvida');
      expect(el().textContent).toContain('Conta de destino encerrada');
      vi.advanceTimersByTime(5000);
      http.expectNone('/bff/transfers/t1');
    });

    it('keeps polling after a failed poll', async () => {
      await confirmAndStartPolling();
      vi.advanceTimersByTime(1000);
      http.expectOne('/bff/transfers/t1').flush({}, { status: 503, statusText: 'Unavailable' });
      await settle();
      await poll('COMPLETED');
      expect(el().textContent).toContain('Concluída');
    });

    it('gives up after 30 s with "Ainda processando"', async () => {
      await confirmAndStartPolling();
      for (let second = 0; second < 29; second++) {
        await poll('PENDING');
      }
      expect(el().textContent).not.toContain('Ainda processando');
      vi.advanceTimersByTime(1000);
      await settle();
      expect(el().textContent).toContain('Ainda processando');
      http.match('/bff/transfers/t1').forEach((request) => expect(request.cancelled).toBe(true));
      vi.advanceTimersByTime(5000);
      http.expectNone('/bff/transfers/t1');
    });

    it('starts a new transfer with a new Idempotency-Key', async () => {
      await reachConfirmation();
      const first = await confirmTransfer();
      const firstKey = first.request.headers.get('Idempotency-Key');
      first.flush({ id: 't1', status: 'PENDING' });
      await settle();
      await poll('COMPLETED');
      button('Nova transferência').click();
      await settle();
      await lookUp();
      await enterAmount('1');
      const req = await confirmTransfer();
      expect(req.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/);
      expect(req.request.headers.get('Idempotency-Key')).not.toBe(firstKey);
      req.flush({ id: 't2', status: 'PENDING' });
      await settle();
    });
  });
});
