import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
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

  async function create(): Promise<void> {
    TestBed.configureTestingModule({
      imports: [TransferComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    // Only the interval timers are faked: the polling is driven by hand, `tick` keeps working.
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TransferComponent);
    await fixture.whenStable();
  }

  async function lookUp(phone = '(11) 98765-4321'): Promise<void> {
    setInput(fixture, 'phone', phone);
    await fixture.whenStable();
    submit(fixture);
    const req = http.expectOne((r) => r.url === '/bff/transfers/recipient');
    expect(req.request.params.get('phone')).toBe('11987654321');
    req.flush({ recipientName: 'Ana Souza', recipientToken: 'token-1' });
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

  afterEach(() => {
    vi.useRealTimers();
    http.verify();
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
    button('Confirmar').click();
    const req = http.expectOne('/bff/transfers');
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
    const requests = http.match('/bff/transfers');
    expect(requests).toHaveLength(2);
    const key = requests[0].request.headers.get('Idempotency-Key');
    expect(key).toBeTruthy();
    expect(requests[1].request.headers.get('Idempotency-Key')).toBe(key);
    requests[0].flush({ id: 't1', status: 'PENDING' });
    requests[1].flush({ id: 't1', status: 'PENDING' });
  });

  it('keeps the same Idempotency-Key when the user retries after a failure', async () => {
    await reachConfirmation();
    button('Confirmar').click();
    const first = http.expectOne('/bff/transfers');
    const key = first.request.headers.get('Idempotency-Key');
    first.flush({}, { status: 503, statusText: 'Unavailable' });
    await settle();
    expect(el().querySelector('[role="alert"]')).not.toBeNull();
    button('Confirmar').click();
    const second = http.expectOne('/bff/transfers');
    expect(second.request.headers.get('Idempotency-Key')).toBe(key);
    second.flush({ id: 't1', status: 'COMPLETED' });
    await settle();
    http.expectOne('/bff/transfers/t1').flush({ id: 't1', status: 'COMPLETED' });
  });

  it('uses a new Idempotency-Key when the amount changes after going back', async () => {
    await reachConfirmation();
    button('Confirmar').click();
    const first = http.expectOne('/bff/transfers');
    const key = first.request.headers.get('Idempotency-Key');
    first.flush({}, { status: 503, statusText: 'Unavailable' });
    await settle();
    button('Voltar').click();
    await settle();
    await enterAmount('30,00');
    button('Confirmar').click();
    const second = http.expectOne('/bff/transfers');
    expect(second.request.body).toEqual({ recipientToken: 'token-1', amount: 3000 });
    expect(second.request.headers.get('Idempotency-Key')).not.toBe(key);
    second.flush({ id: 't1', status: 'PENDING' });
  });

  it('keeps the Idempotency-Key when going back and confirming the same amount', async () => {
    await reachConfirmation();
    button('Confirmar').click();
    const first = http.expectOne('/bff/transfers');
    const key = first.request.headers.get('Idempotency-Key');
    first.flush({}, { status: 503, statusText: 'Unavailable' });
    await settle();
    button('Voltar').click();
    await settle();
    await enterAmount('25,00');
    button('Confirmar').click();
    const second = http.expectOne('/bff/transfers');
    expect(second.request.headers.get('Idempotency-Key')).toBe(key);
    second.flush({ id: 't1', status: 'PENDING' });
  });

  it('fetches the reason right away when the POST already answers a final status', async () => {
    await reachConfirmation();
    button('Confirmar').click();
    http.expectOne('/bff/transfers').flush({ id: 't1', status: 'FAILED' });
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

  describe('status polling', () => {
    async function confirmAndStartPolling(): Promise<void> {
      await reachConfirmation();
      button('Confirmar').click();
      http.expectOne('/bff/transfers').flush({ id: 't1', status: 'PENDING' });
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
      button('Confirmar').click();
      const first = http.expectOne('/bff/transfers');
      const firstKey = first.request.headers.get('Idempotency-Key');
      first.flush({ id: 't1', status: 'PENDING' });
      await settle();
      await poll('COMPLETED');
      button('Nova transferência').click();
      await settle();
      await lookUp();
      await enterAmount('1');
      button('Confirmar').click();
      const req = http.expectOne('/bff/transfers');
      expect(req.request.headers.get('Idempotency-Key')).toMatch(/^[0-9a-f-]{36}$/);
      expect(req.request.headers.get('Idempotency-Key')).not.toBe(firstKey);
      req.flush({ id: 't2', status: 'PENDING' });
      await settle();
    });
  });
});
