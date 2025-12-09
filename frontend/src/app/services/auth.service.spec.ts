import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService, LoginRequest } from './auth.service';
import { environment } from '../../environments/environment';

describe('AuthService - Login', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        { provide: Router, useValue: routerSpy }
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    
    // Clear localStorage before each test
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should login with valid credentials and save token', () => {
    const loginRequest: LoginRequest = {
      email: 'test@example.com',
      password: 'password123'
    };

    const mockResponse = {
      token: 'jwt-token-123',
      email: 'test@example.com',
      fullName: 'Test User',
      role: 'CANDIDATE'
    };

    service.login(loginRequest).subscribe(response => {
      expect(response.token).toBe('jwt-token-123');
      expect(response.email).toBe('test@example.com');
      expect(localStorage.getItem('auth_token')).toBe('jwt-token-123');
      expect(localStorage.getItem('auth_email')).toBe('test@example.com');
      expect(localStorage.getItem('auth_fullName')).toBe('Test User');
      expect(localStorage.getItem('auth_role')).toBe('CANDIDATE');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(loginRequest);
    req.flush(mockResponse);
  });

  it('should handle login error (401)', () => {
    const loginRequest: LoginRequest = {
      email: 'test@example.com',
      password: 'wrongpassword'
    };

    service.login(loginRequest).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        expect(error.status).toBe(401);
        expect(localStorage.getItem('auth_token')).toBeNull();
      }
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    req.flush(
      { message: 'Invalid credentials' },
      { status: 401, statusText: 'Unauthorized' }
    );
  });

  it('should handle login error (403 - disabled user)', () => {
    const loginRequest: LoginRequest = {
      email: 'test@example.com',
      password: 'password123'
    };

    service.login(loginRequest).subscribe({
      next: () => fail('should have failed'),
      error: (error) => {
        expect(error.status).toBe(403);
        expect(localStorage.getItem('auth_token')).toBeNull();
      }
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    req.flush(
      { message: 'User is disabled' },
      { status: 403, statusText: 'Forbidden' }
    );
  });

  it('should not save token if response has no token', () => {
    const loginRequest: LoginRequest = {
      email: 'test@example.com',
      password: 'password123'
    };

    const mockResponse = {
      email: 'test@example.com',
      fullName: 'Test User',
      role: 'CANDIDATE'
      // token is missing
    };

    service.login(loginRequest).subscribe(() => {
      expect(localStorage.getItem('auth_token')).toBeNull();
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    req.flush(mockResponse);
  });
});
