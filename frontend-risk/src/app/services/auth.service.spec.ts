import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  function login(email: string): void {
    auth.login({ email, password: 'secret' }).subscribe();
    const req = http.expectOne(r => r.url.endsWith('/auth/login'));
    req.flush({ token: 't-' + email, email });
  }

  it('keeps the test result on logout and drops the CV', () => {
    localStorage.setItem('ai_readiness_last_result', '{"professionId":"cook"}');
    localStorage.setItem('risk_cv_text', 'secret cv');
    login('a@example.com');

    auth.logout();

    expect(localStorage.getItem('risk_token')).toBeNull();
    expect(localStorage.getItem('risk_cv_text')).toBeNull();
    expect(localStorage.getItem('ai_readiness_last_result')).toBe('{"professionId":"cook"}');
  });

  it('drops the test result when a different person signs in', () => {
    login('a@example.com');
    localStorage.setItem('ai_readiness_last_result', '{"professionId":"cook"}');

    login('b@example.com');

    expect(localStorage.getItem('ai_readiness_last_result')).toBeNull();
    expect(auth.getCurrentUserEmail()).toBe('b@example.com');
  });
});
