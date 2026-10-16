import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';

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
