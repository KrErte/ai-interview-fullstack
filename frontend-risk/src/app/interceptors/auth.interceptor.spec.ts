import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { authInterceptor, needsToken } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('authInterceptor', () => {
  it('needsToken: billing and the paid roadmap yes; auth and the public test no', () => {
    expect(needsToken('http://localhost:8080/api/billing/entitlement')).toBeTrue();
    expect(needsToken('http://localhost:8080/api/billing/checkout')).toBeTrue();
    expect(needsToken('http://localhost:8080/api/ai-readiness/roadmap')).toBeTrue();
    expect(needsToken('http://localhost:8080/api/ai-readiness/assess')).toBeFalse();
    expect(needsToken('http://localhost:8080/api/ai-readiness/professions/search?q=roadmap')).toBeFalse();
    expect(needsToken('http://localhost:8080/api/auth/login')).toBeFalse();
    expect(needsToken('http://localhost:8080/api/job-analysis')).toBeTrue();
  });

  it('attaches the bearer token to billing and roadmap requests only', () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { getToken: () => 'tkn', logout: () => {} } }
      ]
    });
    const client = TestBed.inject(HttpClient);
    const http = TestBed.inject(HttpTestingController);
    client.get('/api/billing/entitlement').subscribe();
    client.post('/api/ai-readiness/roadmap', {}).subscribe();
    client.post('/api/ai-readiness/assess', {}).subscribe();
    expect(http.expectOne('/api/billing/entitlement').request.headers.get('Authorization')).toBe('Bearer tkn');
    expect(http.expectOne('/api/ai-readiness/roadmap').request.headers.get('Authorization')).toBe('Bearer tkn');
    expect(http.expectOne('/api/ai-readiness/assess').request.headers.has('Authorization')).toBeFalse();
    http.verify();
  });
});
