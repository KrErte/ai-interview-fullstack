# Login Flow Test Plan

## 🎯 Goal
Verify that `/auth/login` works correctly between Angular frontend and Spring Boot backend.

---

## 1. Backend Test Plan

### Test File: `AuthControllerLoginTest.java`
**Location:** `backend/src/test/java/ee/kerrete/ainterview/auth/AuthControllerLoginTest.java`

```java
package ee.kerrete.ainterview.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.kerrete.ainterview.auth.dto.AuthResponse;
import ee.kerrete.ainterview.auth.dto.LoginRequest;
import ee.kerrete.ainterview.model.AppUser;
import ee.kerrete.ainterview.model.UserRole;
import ee.kerrete.ainterview.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AuthControllerLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_NAME = "Test User";

    @BeforeEach
    void setUp() {
        // Create test user before each test
        AppUser user = AppUser.builder()
                .email(TEST_EMAIL)
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .fullName(TEST_NAME)
                .role(UserRole.CANDIDATE)
                .enabled(true)
                .build();
        userRepository.save(user);
    }

    @Test
    void testLogin_ValidCredentials_Returns200WithToken() throws Exception {
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.fullName").value(TEST_NAME))
                .andExpect(jsonPath("$.userRole").value("CANDIDATE"));
    }

    @Test
    void testLogin_InvalidPassword_Returns401() throws Exception {
        LoginRequest request = new LoginRequest(TEST_EMAIL, "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("Invalid credentials")));
    }

    @Test
    void testLogin_NonExistentUser_Returns401() throws Exception {
        LoginRequest request = new LoginRequest("nonexistent@example.com", TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("Invalid credentials")));
    }

    @Test
    void testLogin_DisabledUser_Returns403() throws Exception {
        // Disable the user
        AppUser user = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
        user.setEnabled(false);
        userRepository.save(user);

        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("User is disabled")));
    }

    @Test
    void testLogin_InvalidEmailFormat_Returns400() throws Exception {
        LoginRequest request = new LoginRequest("invalid-email", TEST_PASSWORD);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void testLogin_BlankPassword_Returns400() throws Exception {
        LoginRequest request = new LoginRequest(TEST_EMAIL, "");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());
    }
}
```

### Test File: `AuthServiceLoginTest.java` (Unit Test)
**Location:** `backend/src/test/java/ee/kerrete/ainterview/auth/AuthServiceLoginTest.java`

```java
package ee.kerrete.ainterview.auth;

import ee.kerrete.ainterview.auth.dto.AuthResponse;
import ee.kerrete.ainterview.auth.dto.LoginRequest;
import ee.kerrete.ainterview.model.AppUser;
import ee.kerrete.ainterview.model.UserRole;
import ee.kerrete.ainterview.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_TOKEN = "jwt-token-123";
    private static final String TEST_NAME = "Test User";

    @Test
    void testLogin_ValidCredentials_ReturnsToken() {
        // Given
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        AppUser user = AppUser.builder()
                .email(TEST_EMAIL)
                .fullName(TEST_NAME)
                .role(UserRole.CANDIDATE)
                .enabled(true)
                .build();

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(user);
        when(jwtService.generateToken(TEST_EMAIL)).thenReturn(TEST_TOKEN);

        // When
        AuthResponse response = authService.login(request);

        // Then
        assertNotNull(response);
        assertEquals(TEST_TOKEN, response.getToken());
        assertEquals(TEST_EMAIL, response.getEmail());
        assertEquals(TEST_NAME, response.getFullName());
        assertEquals(UserRole.CANDIDATE, response.getUserRole());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken(TEST_EMAIL);
    }

    @Test
    void testLogin_InvalidPassword_ThrowsUnauthorized() {
        // Given
        LoginRequest request = new LoginRequest(TEST_EMAIL, "wrongpassword");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // When/Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(request));
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Invalid credentials"));
        verify(jwtService, never()).generateToken(anyString());
    }

    @Test
    void testLogin_DisabledUser_ThrowsForbidden() {
        // Given
        LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("User is disabled"));

        // When/Then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(request));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("User is disabled"));
        verify(jwtService, never()).generateToken(anyString());
    }
}
```

---

## 2. Frontend Test Plan

### Test File: `auth.service.spec.ts`
**Location:** `frontend/src/app/services/auth.service.spec.ts`

```typescript
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
      userRole: 'CANDIDATE'
    };

    service.login(loginRequest).subscribe(response => {
      expect(response.token).toBe('jwt-token-123');
      expect(response.email).toBe('test@example.com');
      expect(localStorage.getItem('token')).toBe('jwt-token-123');
      expect(localStorage.getItem('authEmail')).toBe('test@example.com');
      expect(localStorage.getItem('authFullName')).toBe('Test User');
      expect(localStorage.getItem('authUserRole')).toBe('CANDIDATE');
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(loginRequest);
    req.flush(mockResponse);
  });

  it('should login with email and password overload', () => {
    const mockResponse = {
      token: 'jwt-token-123',
      email: 'test@example.com',
      fullName: 'Test User',
      userRole: 'CANDIDATE'
    };

    service.login('test@example.com', 'password123').subscribe();

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`);
    expect(req.request.body).toEqual({
      email: 'test@example.com',
      password: 'password123'
    });
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
        expect(localStorage.getItem('token')).toBeNull();
      }
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`);
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
        expect(localStorage.getItem('token')).toBeNull();
      }
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`);
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
      userRole: 'CANDIDATE'
      // token is missing
    };

    service.login(loginRequest).subscribe(() => {
      expect(localStorage.getItem('token')).toBeNull();
    });

    const req = httpMock.expectOne(`${environment.apiBaseUrl}/auth/login`);
    req.flush(mockResponse);
  });
});
```

### Test File: `login.component.spec.ts`
**Location:** `frontend/src/app/pages/auth/login.component.spec.ts`

```typescript
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
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

    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, LoginComponent],
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: Router, useValue: routerSpy }
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
```

---

## 3. Manual Test Checklist

### ✅ Login Flow - Happy Path
- [ ] Navigate to `/login` page
- [ ] Enter valid email: `test@example.com`
- [ ] Enter valid password: `password123`
- [ ] Click "Login" button
- [ ] **Verify**: Redirected to `/dashboard`
- [ ] **Verify**: Check browser DevTools → Application → Local Storage
  - [ ] `token` key exists with JWT token value
  - [ ] `authEmail` key exists with email
  - [ ] `authFullName` key exists with user name
- [ ] **Verify**: Check Network tab → Request to `/api/auth/login`
  - [ ] Status: 200 OK
  - [ ] Response contains `token`, `email`, `fullName`, `userRole`
  - [ ] Subsequent API requests include `Authorization: Bearer <token>` header

### ✅ Login Flow - Error Cases
- [ ] **Invalid Password**
  - [ ] Enter valid email, wrong password
  - [ ] Click "Login"
  - [ ] **Verify**: Error message displayed (e.g., "Invalid credentials")
  - [ ] **Verify**: No redirect, stays on login page
  - [ ] **Verify**: No token saved in localStorage

- [ ] **Non-existent User**
  - [ ] Enter email that doesn't exist: `nonexistent@example.com`
  - [ ] Enter any password
  - [ ] Click "Login"
  - [ ] **Verify**: Error message displayed
  - [ ] **Verify**: Network tab shows 401 Unauthorized

- [ ] **Disabled User** (requires backend setup)
  - [ ] Create user in DB with `enabled = false`
  - [ ] Try to login with that user's credentials
  - [ ] **Verify**: Error message "User is disabled" or 403 Forbidden
  - [ ] **Verify**: Network tab shows 403 Forbidden

- [ ] **Validation Errors**
  - [ ] Leave email blank → Click "Login"
    - [ ] **Verify**: Email field shows validation error
  - [ ] Enter invalid email format (e.g., "notanemail") → Click "Login"
    - [ ] **Verify**: Email validation error shown
  - [ ] Leave password blank → Click "Login"
    - [ ] **Verify**: Password field shows validation error
  - [ ] Enter password < 4 characters → Click "Login"
    - [ ] **Verify**: Password min length validation error

### ✅ UI/UX Checks
- [ ] **Loading State**
  - [ ] Click "Login" → **Verify**: Button shows loading state (disabled/spinner)
  - [ ] **Verify**: Form fields disabled during loading
  - [ ] **Verify**: Loading state clears after success/error

- [ ] **Session Expired Message**
  - [ ] Navigate to `/login?reason=session-expired`
  - [ ] **Verify**: Session expired message displayed in Estonian: "Sessioon aegus. Palun logi uuesti sisse."

- [ ] **Form Reset**
  - [ ] Enter credentials, get error
  - [ ] **Verify**: Error clears when user starts typing again
  - [ ] **Verify**: Form values persist (not cleared on error)

### ✅ Security Checks
- [ ] **Token Storage**
  - [ ] After successful login, check localStorage
  - [ ] **Verify**: Token is stored (not in sessionStorage)
  - [ ] **Verify**: Token is a valid JWT format (starts with `eyJ`)

- [ ] **Token Usage**
  - [ ] After login, navigate to dashboard
  - [ ] **Verify**: Network tab shows `Authorization: Bearer <token>` header on API calls
  - [ ] **Verify**: Protected endpoints work (e.g., `/api/dashboard`)

- [ ] **Route Protection**
  - [ ] Logout (or clear localStorage)
  - [ ] Try to navigate directly to `/dashboard`
  - [ ] **Verify**: Redirected to `/login`

---

## 4. Commands to Run Tests

### Backend Tests

```bash
# Navigate to backend directory
cd backend

# Run all login-related tests
./gradlew test --tests "*Login*"

# Run specific test class
./gradlew test --tests "AuthControllerLoginTest"
./gradlew test --tests "AuthServiceLoginTest"

# Run with verbose output
./gradlew test --tests "*Login*" --info

# Run and generate coverage report (if JaCoCo configured)
./gradlew test jacocoTestReport
# View report: backend/build/reports/jacoco/test/html/index.html
```

### Frontend Tests

```bash
# Navigate to frontend directory
cd frontend

# Run all tests (watch mode)
npm test

# Run tests once (CI mode)
npm test -- --watch=false

# Run specific test file
npm test -- --include='**/auth.service.spec.ts'
npm test -- --include='**/login.component.spec.ts'

# Run with coverage
npm test -- --code-coverage

# Run in headless mode (CI)
npm test -- --watch=false --browsers=ChromeHeadless
```

### Manual Testing

```bash
# Start backend
cd backend
./gradlew bootRun
# Backend runs on http://localhost:8080

# Start frontend (in another terminal)
cd frontend
npm start
# Frontend runs on http://localhost:4200

# Open browser
# Navigate to http://localhost:4200/login
# Follow manual test checklist above
```

### Quick Test Script

```bash
#!/bin/bash
# quick-login-test.sh

echo "🧪 Running Backend Login Tests..."
cd backend
./gradlew test --tests "*Login*" --quiet
BACKEND_RESULT=$?

echo "🧪 Running Frontend Login Tests..."
cd ../frontend
npm test -- --watch=false --browsers=ChromeHeadless --include='**/auth.service.spec.ts' --include='**/login.component.spec.ts'
FRONTEND_RESULT=$?

if [ $BACKEND_RESULT -eq 0 ] && [ $FRONTEND_RESULT -eq 0 ]; then
    echo "✅ All login tests passed!"
    exit 0
else
    echo "❌ Some tests failed"
    exit 1
fi
```

---

## 📊 Test Coverage Summary

| Test Type | Test Cases | Priority |
|-----------|------------|----------|
| **Backend Integration** | 6 tests | High |
| **Backend Unit** | 3 tests | High |
| **Frontend Service** | 5 tests | High |
| **Frontend Component** | 4 tests | Medium |
| **Manual Tests** | 15+ scenarios | High |

**Total**: ~33 test scenarios covering the login flow end-to-end.

---

## 🐛 Known Issues to Verify

1. **Token Key Mismatch** (if not fixed):
   - Interceptor uses `authToken` but service saves `token`
   - **Manual Check**: After login, verify API requests include Authorization header

2. **Error Message Display**:
   - Verify error messages are user-friendly
   - Check both network errors and validation errors display correctly

---

**Last Updated**: 2025-12-08
