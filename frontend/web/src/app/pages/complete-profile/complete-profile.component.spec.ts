import type { Mock } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { SessionService } from '../../core/session.service';
import { setInput, submit, submitButton, tick } from '../../testing/dom';
import { CompleteProfileComponent } from './complete-profile.component';

describe('CompleteProfileComponent', () => {
  let fixture: ComponentFixture<CompleteProfileComponent>;
  let http: HttpTestingController;
  let navigate: Mock;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [CompleteProfileComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    navigate = vi
      .spyOn(TestBed.inject(Router), 'navigateByUrl')
      .mockResolvedValue(true) as unknown as Mock;
    fixture = TestBed.createComponent(CompleteProfileComponent);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  const phoneInput = () =>
    (fixture.nativeElement as HTMLElement).querySelector('input[name="phone"]') as HTMLInputElement;

  it('masks the phone while typing', async () => {
    setInput(fixture, 'phone', '11987654321');
    await fixture.whenStable();
    expect(phoneInput().value).toBe('(11) 98765-4321');
  });

  it('blocks the submit without a valid mobile phone', async () => {
    setInput(fixture, 'name', 'Ana Souza');
    setInput(fixture, 'phone', '1187654321');
    await fixture.whenStable();
    expect(submitButton(fixture).disabled).toBe(true);
    submit(fixture);
    http.expectNone('/bff/customers');
  });

  it('blocks the submit without a name', async () => {
    setInput(fixture, 'phone', '11987654321');
    await fixture.whenStable();
    expect(submitButton(fixture).disabled).toBe(true);
  });

  it('registers the customer and goes to the app', async () => {
    setInput(fixture, 'name', ' Ana Souza ');
    setInput(fixture, 'phone', '11987654321');
    await fixture.whenStable();
    submit(fixture);
    const req = http.expectOne('/bff/customers');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'Ana Souza', phone: '11987654321' });
    req.flush({});
    await tick();
    http.expectOne('/bff/auth/session').flush({ authenticated: true, email: 'ana@example.com' });
    await tick();
    http.expectOne('/bff/me').flush({
      customer: { name: 'Ana Souza', email: 'ana@example.com', phone: '+5511987654321' },
      account: null,
    });
    await tick();
    expect(navigate).toHaveBeenCalledWith('/inicio');
  });

  it('leaves the registration when it worked and only the profile load failed', async () => {
    setInput(fixture, 'name', 'Ana Souza');
    setInput(fixture, 'phone', '11987654321');
    await fixture.whenStable();
    submit(fixture);
    http.expectOne('/bff/customers').flush({});
    await tick();
    http.expectOne('/bff/auth/session').flush({ authenticated: true, email: 'ana@example.com' });
    await tick();
    http.expectOne('/bff/me').flush({}, { status: 503, statusText: 'Service Unavailable' });
    await tick();
    // The guard of Início then shows the error screen, instead of asking for the registration again.
    expect(TestBed.inject(SessionService).loadFailed()).toBe(true);
    expect(navigate).toHaveBeenCalledWith('/inicio');
  });

  it('shows the BFF message when the customer already exists', async () => {
    setInput(fixture, 'name', 'Ana Souza');
    setInput(fixture, 'phone', '11987654321');
    await fixture.whenStable();
    submit(fixture);
    http
      .expectOne('/bff/customers')
      .flush(
        { code: 'user_exists', message: 'Já existe uma conta com este e-mail' },
        { status: 409, statusText: 'Conflict' },
      );
    await tick();
    await fixture.whenStable();
    const alert = (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('Já existe uma conta com este e-mail');
  });

  it('shows the problem detail when the phone is already used', async () => {
    setInput(fixture, 'name', 'Ana Souza');
    setInput(fixture, 'phone', '11987654321');
    await fixture.whenStable();
    submit(fixture);
    http
      .expectOne('/bff/customers')
      .flush({ detail: 'Telefone já cadastrado' }, { status: 409, statusText: 'Conflict' });
    await tick();
    await fixture.whenStable();
    const alert = (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('Telefone já cadastrado');
    expect(navigate).not.toHaveBeenCalled();
  });
});
