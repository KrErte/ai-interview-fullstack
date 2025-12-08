# Login Tests - Quick Start Guide

## 🚀 Quick Commands

### Backend Tests
```bash
cd backend

# Run all login tests
./gradlew test --tests "*Login*"

# Run specific test
./gradlew test --tests "AuthControllerLoginTest"
./gradlew test --tests "AuthServiceLoginTest"
```

### Frontend Tests
```bash
cd frontend

# Run login-related tests
npm test -- --include='**/auth.service.spec.ts' --include='**/login.component.spec.ts'

# Run once (CI mode)
npm test -- --watch=false --include='**/auth.service.spec.ts'
```

---

## 📋 Test Files Created

### Backend
- ✅ `backend/src/test/java/ee/kerrete/ainterview/auth/AuthControllerLoginTest.java` (6 tests)
- ✅ `backend/src/test/java/ee/kerrete/ainterview/auth/AuthServiceLoginTest.java` (3 tests)

### Frontend
- ✅ `frontend/src/app/services/auth.service.spec.ts` (5 tests)
- ✅ `frontend/src/app/pages/auth/login.component.spec.ts` (4 tests)

---

## ✅ What Gets Tested

### Backend Integration Tests (`AuthControllerLoginTest`)
1. ✅ Valid credentials → 200 + token
2. ✅ Invalid password → 401
3. ✅ Non-existent user → 401
4. ✅ Disabled user → 403
5. ✅ Invalid email format → 400
6. ✅ Blank password → 400

### Backend Unit Tests (`AuthServiceLoginTest`)
1. ✅ Valid credentials → Returns token
2. ✅ Invalid password → Throws 401
3. ✅ Disabled user → Throws 403

### Frontend Service Tests (`auth.service.spec.ts`)
1. ✅ Login saves token to localStorage
2. ✅ Supports email/password overload
3. ✅ Handles 401 errors
4. ✅ Handles 403 errors
5. ✅ Doesn't save token if missing from response

### Frontend Component Tests (`login.component.spec.ts`)
1. ✅ Validates form before submit
2. ✅ Calls authService.login on valid form
3. ✅ Displays error on failure
4. ✅ Sets loading state

---

## 🐛 Troubleshooting

### Backend Tests Fail

**Issue**: `@ActiveProfiles("test")` but no test profile config
- **Fix**: Tests will use default H2 in-memory DB. If you need custom config, create:
  - `backend/src/test/resources/application-test.yml`

**Issue**: Database connection errors
- **Fix**: Ensure H2 dependency is in `build.gradle` (already present)

**Issue**: User already exists error
- **Fix**: Tests use `@Transactional` - should auto-rollback. If not, check test isolation.

### Frontend Tests Fail

**Issue**: `environment.apiBaseUrl` is undefined
- **Fix**: Check `frontend/src/environments/environment.ts` has `apiBaseUrl` defined

**Issue**: Router/ActivatedRoute mocks not working
- **Fix**: Ensure all dependencies are properly mocked in `beforeEach`

---

## 📊 Expected Results

### Backend
```
AuthControllerLoginTest > testLogin_ValidCredentials_Returns200WithToken PASSED
AuthControllerLoginTest > testLogin_InvalidPassword_Returns401 PASSED
AuthControllerLoginTest > testLogin_NonExistentUser_Returns401 PASSED
AuthControllerLoginTest > testLogin_DisabledUser_Returns403 PASSED
AuthControllerLoginTest > testLogin_InvalidEmailFormat_Returns400 PASSED
AuthControllerLoginTest > testLogin_BlankPassword_Returns400 PASSED

AuthServiceLoginTest > testLogin_ValidCredentials_ReturnsToken PASSED
AuthServiceLoginTest > testLogin_InvalidPassword_ThrowsUnauthorized PASSED
AuthServiceLoginTest > testLogin_DisabledUser_ThrowsForbidden PASSED
```

### Frontend
```
AuthService - Login
  ✓ should login with valid credentials and save token
  ✓ should login with email and password overload
  ✓ should handle login error (401)
  ✓ should handle login error (403 - disabled user)
  ✓ should not save token if response has no token

LoginComponent
  ✓ should validate form before submit
  ✓ should call authService.login on valid form
  ✓ should display error on login failure
  ✓ should set loading state during login
```

---

## 🔍 Manual Test Checklist (Quick)

1. **Happy Path**
   - [ ] Go to `/login`
   - [ ] Enter `test@example.com` / `password123`
   - [ ] Click Login
   - [ ] ✅ Redirects to `/dashboard`
   - [ ] ✅ localStorage has `token` key

2. **Error Cases**
   - [ ] Wrong password → Shows error, no redirect
   - [ ] Invalid email → Shows validation error
   - [ ] Blank fields → Shows validation errors

3. **Security**
   - [ ] Check Network tab → Authorization header present after login
   - [ ] Check localStorage → Token stored correctly

---

## 📝 Next Steps

1. Run backend tests: `cd backend && ./gradlew test --tests "*Login*"`
2. Run frontend tests: `cd frontend && npm test`
3. Follow manual checklist in browser
4. Fix any failures
5. Commit tests ✅

---

**Total Test Coverage**: 18 automated tests + manual checklist
