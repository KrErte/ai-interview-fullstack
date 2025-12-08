# Focused Login Tests Verification Report
**Date**: 2025-12-08  
**Change Verified**: GlobalExceptionHandler.java update

---

## Focused Login Tests Summary

### Test Command
```bash
./gradlew clean test --tests "*AuthControllerLoginTest*"
```

### Results
- **Total**: 6 tests
- **Passed**: 3 (50%)
- **Failed**: 3 (50%)

---

## Failing Tests

### Failure #1: `testLogin_InvalidPassword_Returns401()`
- **Test Class**: `ee.kerrete.ainterview.auth.AuthControllerLoginTest`
- **Line**: 81
- **Expected**:
  - Status: `401 UNAUTHORIZED`
  - Body: `{"message": "Invalid credentials"}` (or contains "Invalid credentials")
- **Actual**:
  - Status: `401 UNAUTHORIZED` ✅
  - Body: `(empty)` ❌
  - Content-Type: `null` ❌
- **Error Message**: `AssertionError: No value at JSON path "$.message"`
- **Root Cause**: GlobalExceptionHandler not being invoked. ResponseStatusException is resolved but body remains empty.

### Failure #2: `testLogin_NonExistentUser_Returns401()`
- **Test Class**: `ee.kerrete.ainterview.auth.AuthControllerLoginTest`
- **Line**: 92
- **Expected**:
  - Status: `401 UNAUTHORIZED`
  - Body: `{"message": "Invalid credentials"}` (or contains "Invalid credentials")
- **Actual**:
  - Status: `401 UNAUTHORIZED` ✅
  - Body: `(empty)` ❌
  - Content-Type: `null` ❌
- **Error Message**: `AssertionError: No value at JSON path "$.message"`
- **Root Cause**: Same as above - handler not invoked, empty response body.

### Failure #3: `testLogin_DisabledUser_Returns403()`
- **Test Class**: `ee.kerrete.ainterview.auth.AuthControllerLoginTest`
- **Line**: 108
- **Expected**:
  - Status: `403 FORBIDDEN`
  - Body: `{"message": "User is disabled"}` (or contains "User is disabled")
- **Actual**:
  - Status: `403 FORBIDDEN` ✅
  - Body: `(empty)` ❌
  - Content-Type: `null` ❌
- **Error Message**: `AssertionError: No value at JSON path "$.message"`
- **Root Cause**: Same as above - handler not invoked, empty response body.

---

## MockMvc Response Evidence

### Test: `testLogin_DisabledUser_Returns403()`
```
MockHttpServletResponse:
    Status = 403
    Error message = User is disabled
    Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY"]
    Content type = null
    Body = (empty)
```

### Test: `testLogin_NonExistentUser_Returns401()`
```
MockHttpServletResponse:
    Status = 401
    Error message = Invalid credentials
    Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY"]
    Content type = null
    Body = (empty)
```

### Test: `testLogin_InvalidPassword_Returns401()`
```
MockHttpServletResponse:
    Status = 401
    Error message = Invalid credentials
    Headers = [Vary:"Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers", X-Content-Type-Options:"nosniff", X-XSS-Protection:"0", Cache-Control:"no-cache, no-store, max-age=0, must-revalidate", Pragma:"no-cache", Expires:"0", X-Frame-Options:"DENY"]
    Content type = null
    Body = (empty)
```

---

## Root Cause Analysis

### Handler Not Invoked

**Evidence**:
1. ✅ `ResponseStatusException` is correctly thrown and resolved (test logs show "Resolved Exception: Type = org.springframework.web.server.ResponseStatusException")
2. ✅ HTTP status codes are correct (401, 403)
3. ❌ Response body is empty (`Body = `)
4. ❌ Content-Type is null (should be `application/json`)
5. ❌ GlobalExceptionHandler handler method not being called

**Handler Implementation** (verified exists):
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException ex) {
        // ... implementation exists
    }
}
```

**Suspected Issues**:
1. **Security Filter Chain**: Spring Security filters may be intercepting exceptions before they reach `@RestControllerAdvice`
2. **Test Context**: Handler may not be loaded/registered in `@SpringBootTest` context
3. **Exception Handling Order**: Spring's default `ResponseStatusException` handling may take precedence over custom handler
4. **Component Scan**: Handler may not be discovered (though package structure suggests it should be)

**Mismatch Identified**: 
- **Handler not invoked** - The `@RestControllerAdvice` handler exists but is not being called when `ResponseStatusException` is thrown
- **Missing JSON fields** - No JSON body is returned (should have `{"message": "..."}`)
- **Wrong Content-Type** - Content-Type is null instead of `application/json`

---

## Final Result

### Login Flow Status: ❌ **NOT GREEN**

- **Status**: 3 of 6 tests failing
- **Issue**: GlobalExceptionHandler not being invoked
- **Impact**: Error responses return empty bodies, frontend cannot display error messages

### Backend Ready for Regression: ❌ **NOT READY**

- **Status**: Login tests must pass before running full regression suite
- **Blocking Issue**: Exception handler not working in test context

---

## Prompt for BACKEND Agent

```
The GlobalExceptionHandler exists at `backend/src/main/java/ee/kerrete/ainterview/config/GlobalExceptionHandler.java` but is not being invoked during tests. ResponseStatusException is correctly thrown (status codes 401/403 are correct) but response bodies are empty.

Test Evidence:
- MockMvc shows: `Resolved Exception: Type = org.springframework.web.server.ResponseStatusException`
- But response: `Body = (empty)`, `Content type = null`
- Tests expect: `{"message": "Invalid credentials"}` or `{"message": "User is disabled"}`

Root Cause: Handler not invoked - @RestControllerAdvice handler exists but Spring is not calling it.

Investigation Steps:
1. Add logging to GlobalExceptionHandler.handleResponseStatusException() to verify if it's called
2. Check if SecurityConfig or security filters are intercepting exceptions before @RestControllerAdvice
3. Verify @RestControllerAdvice is discovered in @SpringBootTest context (check component scan)
4. Consider if Spring Security's exception handling takes precedence
5. Check if there's a conflict with AuthController's @ExceptionHandler for MethodArgumentNotValidException

Possible Fixes:
- Ensure GlobalExceptionHandler is in a package scanned by @SpringBootApplication
- Check if security filter chain needs configuration to allow exception handler
- Verify @RestControllerAdvice vs @ControllerAdvice (current uses @RestControllerAdvice which should work)
- Consider adding explicit @Order annotation to ensure handler precedence

Test Verification:
- Run: ./gradlew clean test --tests "*AuthControllerLoginTest*"
- Expected: All 6 tests pass
- Verify: Response body contains JSON with "message" field, Content-Type is application/json
```

---

**Report Generated**: 2025-12-08  
**Next Action**: BACKEND agent must investigate why GlobalExceptionHandler is not being invoked
