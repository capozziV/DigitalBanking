import type { Mock } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { setInput, submit, submitButton, tick } from '../../testing/dom';
import { SignUpComponent } from './sign-up.component';

describe('SignUpComponent', () => {
  let fixture: ComponentFixture<SignUpComponent>;
  let http: HttpTestingController;
  let navigate: Mock;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [SignUpComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    navigate = vi
      .spyOn(TestBed.inject(Router), 'navigateByUrl')
      .mockResolvedValue(true) as unknown as Mock;
    fixture = TestBed.createComponent(SignUpComponent);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  async function fill(password: string, confirmation: string) {
    setInput(fixture, 'email', 'ana@example.com');
    setInput(fixture, 'password', password);
    setInput(fixture, 'confirmation', confirmation);
    await fixture.whenStable();
  }

  async function finishSignIn() {
    await tick();
    http.expectOne('/bff/auth/sign-in').flush({});
    await tick();
    http.expectOne('/bff/auth/session').flush({ authenticated: true, email: 'ana@example.com' });
    await tick();
    http.expectOne('/bff/me').flush({ customer: null, account: null });
    await tick();
  }

  it.each([
    ['12345', '12345'],
    ['1234567', '1234567'],
    ['12a456', '12a456'],
  ])('blocks the submit for the password "%s"', async (password, confirmation) => {
    await fill(password, confirmation);
    expect(submitButton(fixture).disabled).toBe(true);
    submit(fixture);
    http.expectNone('/bff/auth/sign-up');
  });

  it('blocks the submit when the confirmation differs', async () => {
    await fill('123456', '654321');
    expect(submitButton(fixture).disabled).toBe(true);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('As senhas não conferem');
    submit(fixture);
    http.expectNone('/bff/auth/sign-up');
  });

  it('asks for the e-mail code only when the BFF requires it', async () => {
    await fill('123456', '123456');
    submit(fixture);
    const req = http.expectOne('/bff/auth/sign-up');
    expect(req.request.body).toEqual({ email: 'ana@example.com', password: '123456' });
    req.flush({ confirmationRequired: true });
    await tick();
    await fixture.whenStable();
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('input[name="code"]'),
    ).not.toBeNull();

    setInput(fixture, 'code', '987654');
    await fixture.whenStable();
    submit(fixture);
    const confirm = http.expectOne('/bff/auth/confirm');
    expect(confirm.request.body).toEqual({ email: 'ana@example.com', code: '987654' });
    confirm.flush({});
    await finishSignIn();
    expect(navigate).toHaveBeenCalledWith('/inicio');
  });

  it('skips the code and signs in when no confirmation is required', async () => {
    await fill('123456', '123456');
    submit(fixture);
    http.expectOne('/bff/auth/sign-up').flush({ confirmationRequired: false });
    await finishSignIn();
    expect((fixture.nativeElement as HTMLElement).querySelector('input[name="code"]')).toBeNull();
    expect(navigate).toHaveBeenCalledWith('/inicio');
  });

  it('shows the problem detail when the e-mail is already taken', async () => {
    await fill('123456', '123456');
    submit(fixture);
    http
      .expectOne('/bff/auth/sign-up')
      .flush({ detail: 'Este e-mail já está cadastrado' }, { status: 409, statusText: 'Conflict' });
    await tick();
    await fixture.whenStable();
    const alert = (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('Este e-mail já está cadastrado');
  });
});
