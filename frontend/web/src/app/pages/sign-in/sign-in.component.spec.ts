import type { Mock } from 'vitest';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { setInput, submit, submitButton, tick } from '../../testing/dom';
import { SignInComponent } from './sign-in.component';

describe('SignInComponent', () => {
  let fixture: ComponentFixture<SignInComponent>;
  let http: HttpTestingController;
  let navigate: Mock;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [SignInComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    navigate = vi
      .spyOn(TestBed.inject(Router), 'navigateByUrl')
      .mockResolvedValue(true) as unknown as Mock;
    fixture = TestBed.createComponent(SignInComponent);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  it('shows Entrar as the page heading with a link to Criar conta', () => {
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('h1')?.textContent).toContain('Entrar');
    expect(el.querySelector('a[href="/criar-conta"]')).not.toBeNull();
  });

  it.each(['12345', '1234567', '12a456', ''])(
    'blocks the submit for the password "%s"',
    async (password) => {
      setInput(fixture, 'email', 'ana@example.com');
      setInput(fixture, 'password', password);
      await fixture.whenStable();
      expect(submitButton(fixture).disabled).toBe(true);
      submit(fixture);
      http.expectNone('/bff/auth/sign-in');
    },
  );

  it('signs in with six digits and goes to the app', async () => {
    setInput(fixture, 'email', 'ana@example.com');
    setInput(fixture, 'password', '123456');
    await fixture.whenStable();
    expect(submitButton(fixture).disabled).toBe(false);
    submit(fixture);
    const req = http.expectOne('/bff/auth/sign-in');
    expect(req.request.body).toEqual({ email: 'ana@example.com', password: '123456' });
    req.flush({});
    await tick();
    http.expectOne('/bff/auth/session').flush({ authenticated: true, email: 'ana@example.com' });
    await tick();
    http.expectOne('/bff/me').flush({ customer: null, account: null });
    await tick();
    expect(navigate).toHaveBeenCalledWith('/inicio');
  });

  it('shows the error when the credentials are wrong', async () => {
    setInput(fixture, 'email', 'ana@example.com');
    setInput(fixture, 'password', '123456');
    await fixture.whenStable();
    submit(fixture);
    http
      .expectOne('/bff/auth/sign-in')
      .flush({ detail: 'x' }, { status: 401, statusText: 'Unauthorized' });
    await tick();
    await fixture.whenStable();
    const alert = (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('E-mail ou senha incorretos');
    expect(navigate).not.toHaveBeenCalled();
  });
});
