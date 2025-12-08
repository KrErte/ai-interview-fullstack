import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../services/auth.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    const authSpy = jasmine.createSpyObj('AuthService', ['login']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const routeSpy = {
      snapshot: {
        queryParamMap: {
          get: jasmine.createSpy('get').and.returnValue(null)
        }
      }
    };

    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, LoginComponent],
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: routeSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  it('should validate form before submit', () => {
    component.form.patchValue({ email: '', password: '' });
    spyOn(component.form, 'markAllAsTouched');

    component.submit();

    expect(component.form.markAllAsTouched).toHaveBeenCalled();
    expect(authService.login).not.toHaveBeenCalled();
  });

  it('should call authService.login on valid form', () => {
    component.form.patchValue({
      email: 'test@example.com',
      password: 'password123'
    });
    authService.login.and.returnValue(of({
      token: 'jwt-token',
      email: 'test@example.com',
      fullName: 'Test User',
      userRole: 'CANDIDATE'
    }));

    component.submit();

    expect(authService.login).toHaveBeenCalledWith({
      email: 'test@example.com',
      password: 'password123'
    });
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
    expect(component.loading).toBeFalse();
  });

  it('should display error on login failure', () => {
    component.form.patchValue({
      email: 'test@example.com',
      password: 'wrongpassword'
    });
    authService.login.and.returnValue(
      throwError(() => ({ status: 401, error: { message: 'Invalid credentials' } }))
    );

    component.submit();

    expect(component.error).toContain('Invalid credentials');
    expect(component.loading).toBeFalse();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('should set loading state during login', () => {
    component.form.patchValue({
      email: 'test@example.com',
      password: 'password123'
    });
    authService.login.and.returnValue(
      new Promise(() => {}) as any // Never resolves
    );

    component.submit();

    expect(component.loading).toBeTrue();
  });
});
