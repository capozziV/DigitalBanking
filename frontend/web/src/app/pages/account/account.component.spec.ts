import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { SessionService } from '../../core/session.service';
import { setInput, submit, tick } from '../../testing/dom';
import { AccountComponent } from './account.component';

const account = {
  id: 'a1',
  customerId: 'c1',
  type: 'GOLD',
  status: 'OPEN',
  balance: 0,
  overdraftLimit: 200000,
  availableBalance: 200000,
};

const me = (withAccount: boolean, phone = '+5511987654321') => ({
  customer: { name: 'Ana Souza', email: 'ana@example.com', phone },
  account: withAccount ? account : null,
});

describe('AccountComponent', () => {
  let fixture: ComponentFixture<AccountComponent>;
  let http: HttpTestingController;
  let session: SessionService;
  const el = () => fixture.nativeElement as HTMLElement;
  const button = (text: string) =>
    Array.from(el().querySelectorAll('button')).find((b) => b.textContent?.includes(text)) as
      HTMLButtonElement | undefined;

  async function settle(): Promise<void> {
    await tick();
    await fixture.whenStable();
  }

  async function create(body = me(true)): Promise<void> {
    TestBed.configureTestingModule({
      imports: [AccountComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    session = TestBed.inject(SessionService);
    session.customer.set(body.customer);
    session.account.set(body.account);
    fixture = TestBed.createComponent(AccountComponent);
    await fixture.whenStable();
  }

  afterEach(() => http.verify());

  it('shows the customer and the account data', async () => {
    await create();
    expect(el().textContent).toContain('Ana Souza');
    expect(el().textContent).toContain('ana@example.com');
    expect(el().textContent).toContain('(11) 98765-4321');
    expect(el().textContent).toContain('Gold');
    expect(el().textContent).toContain('R$ 2.000,00');
  });

  it('changes the phone sending only digits and reloads the profile', async () => {
    await create();
    setInput(fixture, 'phone', '(21) 99876-5432');
    await fixture.whenStable();
    submit(fixture);
    const req = http.expectOne('/bff/me/phone');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ phone: '21998765432' });
    req.flush({});
    await settle();
    http.expectOne('/bff/me').flush(me(true, '+5521998765432'));
    await settle();
    expect(session.customer()?.phone).toBe('+5521998765432');
    expect(el().textContent).toContain('(21) 99876-5432');
    expect(el().textContent).toContain('Telefone atualizado');
  });

  it('blocks a phone that is not a mobile number', async () => {
    await create();
    setInput(fixture, 'phone', '1187654321');
    await fixture.whenStable();
    submit(fixture);
    http.expectNone('/bff/me/phone');
  });

  it('shows the message when the phone is already used', async () => {
    await create();
    setInput(fixture, 'phone', '21998765432');
    await fixture.whenStable();
    submit(fixture);
    http
      .expectOne('/bff/me/phone')
      .flush({ detail: 'Telefone já cadastrado' }, { status: 409, statusText: 'Conflict' });
    await settle();
    expect(el().querySelector('[role="alert"]')?.textContent).toContain('Telefone já cadastrado');
  });

  it('asks for confirmation before closing the account', async () => {
    await create();
    button('Encerrar conta')?.click();
    await fixture.whenStable();
    http.expectNone('/bff/accounts/close');
    expect(el().textContent).toContain('Tem certeza');
    button('Cancelar')?.click();
    await fixture.whenStable();
    expect(el().textContent).not.toContain('Tem certeza');
    expect(button('Encerrar conta')).toBeTruthy();
  });

  it('closes the account after the confirmation and reloads the profile', async () => {
    await create();
    button('Encerrar conta')?.click();
    await fixture.whenStable();
    button('Sim, encerrar')?.click();
    const req = http.expectOne('/bff/accounts/close');
    expect(req.request.method).toBe('POST');
    req.flush({});
    await settle();
    http.expectOne('/bff/me').flush(me(false));
    await settle();
    expect(session.account()).toBeNull();
    expect(el().textContent).toContain('Você não tem conta aberta');
    expect(button('Encerrar conta')).toBeUndefined();
  });

  it('shows why the account could not be closed', async () => {
    await create();
    button('Encerrar conta')?.click();
    await fixture.whenStable();
    button('Sim, encerrar')?.click();
    http
      .expectOne('/bff/accounts/close')
      .flush(
        { detail: 'A conta precisa estar com saldo zero' },
        { status: 409, statusText: 'Conflict' },
      );
    await settle();
    expect(el().querySelector('[role="alert"]')?.textContent).toContain('saldo zero');
  });

  it('has no close button when there is no account', async () => {
    await create(me(false));
    expect(el().textContent).toContain('Você não tem conta aberta');
    expect(button('Encerrar conta')).toBeUndefined();
  });
});
