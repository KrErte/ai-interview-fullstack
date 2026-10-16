import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
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
    client.post('/api/billing/checkout', {}).subscribe();
    client.get('/api/ai-readiness/professions').subscribe();
    client.get('/api/ai-readiness/questions').subscribe();
    client.get('/api/ai-readiness/features').subscribe();
    expect(http.expectOne('/api/billing/entitlement').request.headers.get('Authorization')).toBe('Bearer tkn');
    expect(http.expectOne('/api/ai-readiness/roadmap').request.headers.get('Authorization')).toBe('Bearer tkn');
    expect(http.expectOne('/api/ai-readiness/assess').request.headers.has('Authorization')).toBeFalse();
    expect(http.expectOne('/api/billing/checkout').request.headers.get('Authorization')).toBe('Bearer tkn');
    for (const anon of ['/api/ai-readiness/professions', '/api/ai-readiness/questions', '/api/ai-readiness/features']) {
      expect(http.expectOne(anon).request.headers.has('Authorization')).withContext(anon).toBeFalse();
    }
    http.verify();
  });

  it('401 from checkout or entitlement: login with reason=session-expired and returnUrl with resume=paywall', () => {
    const logout = jasmine.createSpy('logout');
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { getToken: () => 'tkn', logout } }
      ]
    });
    const router = TestBed.inject(Router);
    spyOnProperty(router, 'url', 'get').and.returnValue('/ai-test?team=ABC');
    const nav = spyOn(router, 'navigate').and.resolveTo(true);
    const client = TestBed.inject(HttpClient);
    const http = TestBed.inject(HttpTestingController);
    for (const url of ['/api/billing/checkout', '/api/billing/entitlement']) {
      nav.calls.reset();
      client.get(url).subscribe({ error: () => {} });
      http.expectOne(url).flush(null, { status: 401, statusText: 'Unauthorized' });
      expect(nav).toHaveBeenCalledWith(['/login'], {
        queryParams: { reason: 'session-expired', returnUrl: '/ai-test?team=ABC&resume=paywall' }
      });
    }
    expect(logout).toHaveBeenCalled();
  });
});
