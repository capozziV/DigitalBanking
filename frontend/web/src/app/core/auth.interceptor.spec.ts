import type { Mock } from 'vitest';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { SessionService } from './session.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let navigate: Mock;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
    navigate = vi
      .spyOn(TestBed.inject(Router), 'navigateByUrl')
      .mockResolvedValue(true) as unknown as Mock;
    document.cookie = 'bank_csrf=token-123; path=/';
  });

  afterEach(() => {
    document.cookie = 'bank_csrf=; path=/; max-age=0';
    controller.verify();
  });

  it('sends the CSRF header on non-GET requests', () => {
    http.post('/bff/transfers', {}).subscribe();
    const req = controller.expectOne('/bff/transfers');
    expect(req.request.headers.get('X-CSRF-Token')).toBe('token-123');
    req.flush({});
  });

  it('does not send the CSRF header on GET', () => {
    http.get('/bff/home').subscribe();
    const req = controller.expectOne('/bff/home');
    expect(req.request.headers.has('X-CSRF-Token')).toBe(false);
    req.flush({});
  });

  it('does not send the CSRF header to other origins', () => {
    http.post('https://other.example/x', {}).subscribe();
    const req = controller.expectOne('https://other.example/x');
    expect(req.request.headers.has('X-CSRF-Token')).toBe(false);
    req.flush({});
  });

  it('clears the session and goes to Entrar on 401', () => {
    const session = TestBed.inject(SessionService);
    http.get('/bff/home').subscribe({ error: () => undefined });
    controller.expectOne('/bff/home').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(session.authenticated()).toBe(false);
    expect(session.loaded()).toBe(true);
    expect(navigate).toHaveBeenCalledWith('/entrar');
  });

  it('lets a wrong password on sign in reach the screen', () => {
    let status = 0;
    http.post('/bff/auth/sign-in', {}).subscribe({ error: (e) => (status = e.status) });
    controller
      .expectOne('/bff/auth/sign-in')
      .flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(status).toBe(401);
    expect(navigate).not.toHaveBeenCalled();
  });
});
