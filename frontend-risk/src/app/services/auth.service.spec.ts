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

const TEST_KEYS = ['ai_readiness_last_result', 'ai_readiness_last_answers', 'ai_readiness_roadmap_progress',
  'ai_readiness_ai_marks', 'ai_readiness_teams'];

describe('AuthService browser data', () => {
  let auth: AuthService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    auth = TestBed.inject(AuthService);
    auth.persistSession({ token: 't1', email: 'anna@example.org' });
    localStorage.setItem('risk_cv_text', 'cv');
    TEST_KEYS.forEach(key => localStorage.setItem(key, '{}'));
  });

  afterEach(() => localStorage.clear());

  it('keeps the test result on logout, as the privacy policy says', () => {
    auth.logout();
    expect(auth.isLoggedIn()).toBeFalse();
    expect(localStorage.getItem('risk_cv_text')).toBeNull();
    TEST_KEYS.forEach(key => expect(localStorage.getItem(key)).withContext(key).not.toBeNull());
  });

  it('keeps the test result when the same person logs in again', () => {
    auth.persistSession({ token: 't2', email: 'ANNA@example.org' });
    TEST_KEYS.forEach(key => expect(localStorage.getItem(key)).withContext(key).not.toBeNull());
  });

  it('removes the test result when a different person logs in', () => {
    auth.persistSession({ token: 't3', email: 'ben@example.org' });
    expect(localStorage.getItem('risk_cv_text')).toBeNull();
    TEST_KEYS.forEach(key => expect(localStorage.getItem(key)).withContext(key).toBeNull());
  });
});
