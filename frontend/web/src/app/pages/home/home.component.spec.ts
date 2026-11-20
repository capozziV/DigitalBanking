import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { SessionService } from '../../core/session.service';
import { tick } from '../../testing/dom';
import { HomeComponent } from './home.component';

const account = {
  id: 'a1',
  customerId: 'c1',
  type: 'GOLD',
  status: 'OPEN',
  balance: 482015,
  overdraftLimit: 200000,
  availableBalance: 682015,
};

const entry = (id: string, kind: 'CREDIT' | 'DEBIT', amount: number, description: string) => ({
  id,
  kind,
  amount,
  description,
  balanceAfter: 1000,
  createdAt: '2026-11-19T12:00:00Z',
});

describe('HomeComponent', () => {
  let fixture: ComponentFixture<HomeComponent>;
  let http: HttpTestingController;
  const el = () => fixture.nativeElement as HTMLElement;

  async function create(withAccount = true): Promise<void> {
    TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    TestBed.inject(SessionService).customer.set({
      name: 'Vitor Capozzi',
      email: 'v@example.com',
      phone: '+5511987654321',
    });
    TestBed.inject(SessionService).account.set(withAccount ? account : null);
    fixture = TestBed.createComponent(HomeComponent);
    await fixture.whenStable();
  }

  async function flushHome(body: object): Promise<void> {
    http.expectOne('/bff/home').flush(body);
    await tick();
    await fixture.whenStable();
  }

  afterEach(() => http.verify());

  it('shows the balance card, the daily limit and the latest entries', async () => {
    await create();
    await flushHome({
      account,
      dailyLimit: { limit: 500000, used: 120000, available: 380000 },
      latestEntries: [
        entry('1', 'CREDIT', 100000, 'Transferência recebida'),
        entry('2', 'DEBIT', 30000, 'Transferência enviada'),
      ],
    });
    expect(el().textContent).toContain('Olá, Vitor');
    expect(el().querySelector('.balance')?.textContent).toContain('R$ 6.820,15');
    expect(el().textContent).toContain('Conta Gold');
    expect(el().textContent).toContain('cheque especial R$ 2.000,00');
    expect(el().textContent).toContain('R$ 1.200,00 de R$ 5.000,00');
    expect(el().querySelectorAll('li')).toHaveLength(2);
    const links = Array.from(el().querySelectorAll('a')).map((a) => a.getAttribute('href'));
    expect(links).toContain('/transferir');
    expect(links).toContain('/extrato');
  });

  it('fills the limit bar in proportion to what was used', async () => {
    await create();
    await flushHome({
      account,
      dailyLimit: { limit: 500000, used: 125000, available: 375000 },
      latestEntries: [],
    });
    const fill = el().querySelector('.bar-fill') as HTMLElement;
    expect(fill.style.width).toBe('25%');
  });

  it('offers "Abrir conta" without calling /bff/home when the session has no account, and opens it', async () => {
    await create(false);
    await tick();
    await fixture.whenStable();
    const open = Array.from(el().querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Abrir conta'),
    ) as HTMLButtonElement;
    expect(open).toBeTruthy();
    expect(el().querySelector('.balance')).toBeNull();
    http.expectNone('/bff/home');
    open.click();
    const req = http.expectOne('/bff/accounts');
    expect(req.request.method).toBe('POST');
    req.flush({});
    await tick();
    http.expectOne('/bff/me').flush({
      customer: { name: 'Vitor Capozzi', email: 'v@example.com', phone: '+5511987654321' },
      account,
    });
    await tick();
    await flushHome({
      account: { ...account, balance: 10000, availableBalance: 10000, overdraftLimit: 0 },
      dailyLimit: { limit: 500000, used: 0, available: 500000 },
      latestEntries: [entry('1', 'CREDIT', 10000, 'Bônus de boas-vindas')],
    });
    expect(el().textContent).toContain('Bônus de boas-vindas');
    expect(el().querySelector('.balance')?.textContent).toContain('R$ 100,00');
  });

  it('falls back to "Abrir conta" when /bff/home answers 404', async () => {
    await create();
    http.expectOne('/bff/home').flush({}, { status: 404, statusText: 'Not Found' });
    await tick();
    await fixture.whenStable();
    expect(el().textContent).toContain('Você ainda não tem conta');
    expect(el().textContent).not.toContain('Não foi possível carregar');
  });

  it('shows the message of a failed account opening', async () => {
    await create(false);
    await tick();
    await fixture.whenStable();
    (
      Array.from(el().querySelectorAll('button')).find((b) =>
        b.textContent?.includes('Abrir conta'),
      ) as HTMLButtonElement
    ).click();
    http
      .expectOne('/bff/accounts')
      .flush({ detail: 'Cliente bloqueado' }, { status: 422, statusText: 'Unprocessable' });
    await tick();
    await fixture.whenStable();
    expect(el().querySelector('[role="alert"]')?.textContent).toContain('Cliente bloqueado');
  });

  it('shows a discreet notice with a retry when the load fails', async () => {
    await create();
    http.expectOne('/bff/home').flush({}, { status: 503, statusText: 'Unavailable' });
    await tick();
    await fixture.whenStable();
    expect(el().textContent).toContain('Não foi possível carregar. Tente de novo.');
    (
      Array.from(el().querySelectorAll('button')).find((b) =>
        b.textContent?.includes('Tentar de novo'),
      ) as HTMLButtonElement
    ).click();
    await flushHome({ account: null, dailyLimit: null, latestEntries: [] });
    expect(el().textContent).not.toContain('Não foi possível carregar');
  });
});
