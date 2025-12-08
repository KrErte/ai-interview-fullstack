# QA Testing Strategy for AI Interview Fullstack

## 🚨 Risk Summary (Updated 2025-12-08)

### **CRITICAL BUGS FOUND**

1. **🔴 CRITICAL: Auth Token Key Mismatch**
   - **Location**: `frontend/src/app/interceptors/auth.interceptor.ts:8` vs `frontend/src/app/services/auth.service.ts:32`
   - **Issue**: Interceptor reads `localStorage.getItem('authToken')` but AuthService saves to `localStorage.setItem('token', ...)`
   - **Impact**: **Authentication will fail** - tokens won't be sent in API requests
   - **Fix Required**: Change interceptor to use `'token'` key or standardize on one key

2. **🔴 CRITICAL: JWT Secret Key Hardcoded**
   - **Location**: `backend/src/main/java/ee/kerrete/ainterview/auth/JwtService.java:15`
   - **Issue**: Hardcoded secret key in source code
   - **Impact**: Security vulnerability, tokens can be forged
   - **Fix Required**: Move to environment variable/Spring config

### **HIGH RISK AREAS**

1. **Authentication & Authorization**
   - JWT token validation failures could expose protected endpoints
   - Email resolution inconsistencies between SecurityContext and request params (potential bypass)
   - Disabled users can still authenticate if filter logic fails
   - Token expiration not handled gracefully (no refresh mechanism)
   - **Recent Change**: AuthController, AuthService, JwtAuthenticationFilter refactored

2. **Data Integrity**
   - UserProfile and AppUser fullName sync can desynchronize
   - JobMatchService skill parsing from JSON can fail silently
   - CV summary extraction may corrupt data if parsing fails
   - Transaction boundaries in multi-step operations (e.g., job match + training progress update)
   - **Recent Change**: UserProfileService, CvSummaryService, DashboardService modified

3. **Security Vulnerabilities**
   - CORS configuration allows all origins (`*`) - potential CSRF
   - Email parameter injection in controllers (bypassing SecurityContext)
   - Password validation bypass if confirmPassword is null
   - **Recent Change**: SecurityConfig, RegisterRequest modified

4. **Frontend State Management**
   - Race conditions in dashboard data loading (forkJoin)
   - Token expiration not detected until API call fails
   - Cached CV text persistence across sessions may leak data
   - **Recent Change**: Login/Register components refactored

5. **Business Logic Edge Cases**
   - JobMatchService: empty skills array causes division by zero risk (line 96: `required.isEmpty()` check exists but needs test)
   - UserProfileService: completeness calculation with null values
   - Job match scoring with missing training progress data
   - Skill parsing with malformed JSON/CSV
   - **Recent Change**: JobMatchService, DashboardService modified

---

## 📋 Unit Tests

### Backend (JUnit 5 + Spring Boot Test)

#### 1. `AuthServiceTest.java`
**Location:** `backend/src/test/java/ee/kerrete/ainterview/auth/AuthServiceTest.java`

**Test Cases:**
```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock AppUserRepository appUserRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    
    @InjectMocks AuthService authService;
    
    // Registration tests
    @Test void testRegister_Success() {
        // Given: valid request, no existing user
        // When: register()
        // Then: user saved, token generated, AuthResponse returned
    }
    
    @Test void testRegister_DuplicateEmail_ThrowsConflict() {
        // Given: email exists
        // When: register()
        // Then: ResponseStatusException with CONFLICT
    }
    
    @Test void testRegister_PasswordMismatch_ThrowsBadRequest() {
        // Given: password != confirmPassword
        // When: register()
        // Then: ResponseStatusException with BAD_REQUEST
    }
    
    @Test void testRegister_ConfirmPasswordNull_Succeeds() {
        // Given: confirmPassword is null
        // When: register()
        // Then: succeeds (optional field)
    }
    
    @Test void testRegister_DefaultRole_Candidate() {
        // Given: role not provided
        // When: register()
        // Then: user created with CANDIDATE role
    }
    
    // Login tests
    @Test void testLogin_Success() {
        // Given: valid credentials
        // When: login()
        // Then: token generated, AuthResponse returned
    }
    
    @Test void testLogin_InvalidCredentials_ThrowsUnauthorized() {
        // Given: wrong password
        // When: login()
        // Then: ResponseStatusException with UNAUTHORIZED
    }
    
    @Test void testLogin_DisabledUser_ThrowsForbidden() {
        // Given: user exists but disabled
        // When: login()
        // Then: ResponseStatusException with FORBIDDEN
    }
    
    @Test void testLogin_NonExistentUser_ThrowsUnauthorized() {
        // Given: email doesn't exist
        // When: login()
        // Then: ResponseStatusException with UNAUTHORIZED
    }
}
```

**Mock Dependencies:** `AppUserRepository`, `PasswordEncoder`, `JwtService`, `AuthenticationManager`

---

#### 2. `AuthControllerTest.java`
**Location:** `backend/src/test/java/ee/kerrete/ainterview/auth/AuthControllerTest.java`

**Test Cases:**
```java
@WebMvcTest(AuthController.class)
class AuthControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean AuthService authService;
    
    @Test void testLogin_ValidRequest_Returns200() throws Exception {
        // POST /api/auth/login with valid body
        // Verify: 200 OK, AuthResponse JSON
    }
    
    @Test void testLogin_InvalidEmail_Returns400() throws Exception {
        // POST with invalid email format
        // Verify: 400 BAD_REQUEST, validation errors
    }
    
    @Test void testLogin_BlankPassword_Returns400() throws Exception {
        // POST with blank password
        // Verify: 400, field error for password
    }
    
    @Test void testRegister_ValidRequest_Returns200() throws Exception {
        // POST /api/auth/register with valid body
        // Verify: 200 OK, token in response
    }
    
    @Test void testRegister_ValidationErrors_Returns400() throws Exception {
        // POST with missing required fields
        // Verify: 400, formatted field errors
    }
    
    @Test void testExceptionHandler_FormatsErrors() throws Exception {
        // Trigger MethodArgumentNotValidException
        // Verify: Map<String, String> with field names and messages
    }
}
```

---

#### 3. `JwtAuthenticationFilterTest.java`
**Location:** `backend/src/test/java/ee/kerrete/ainterview/auth/JwtAuthenticationFilterTest.java`

**Test Cases:**
```java
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    @Mock JwtService jwtService;
    @Mock AppUserRepository appUserRepository;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain filterChain;
    
    @InjectMocks JwtAuthenticationFilter filter;
    
    @Test void testDoFilter_PublicEndpoint_SkipsFilter() throws Exception {
        // Given: path = "/api/auth/login"
        // When: doFilterInternal()
        // Then: filterChain.doFilter() called, no authentication set
    }
    
    @Test void testDoFilter_ValidToken_SetsSecurityContext() throws Exception {
        // Given: valid Bearer token, user exists and enabled
        // When: doFilterInternal()
        // Then: SecurityContext has authentication with email and ROLE_CANDIDATE
    }
    
    @Test void testDoFilter_InvalidToken_ContinuesChain() throws Exception {
        // Given: malformed token
        // When: doFilterInternal()
        // Then: filterChain.doFilter() called, no authentication set
    }
    
    @Test void testDoFilter_DisabledUser_SkipsAuthentication() throws Exception {
        // Given: valid token, user exists but disabled
        // When: doFilterInternal()
        // Then: filterChain.doFilter() called, no authentication set
    }
    
    @Test void testDoFilter_MissingHeader_ContinuesChain() throws Exception {
        // Given: no Authorization header
        // When: doFilterInternal()
        // Then: filterChain.doFilter() called
    }
    
    @Test void testDoFilter_MalformedHeader_ContinuesChain() throws Exception {
        // Given: Authorization header without "Bearer " prefix
        // When: doFilterInternal()
        // Then: filterChain.doFilter() called
    }
}
```

---

#### 4. `UserProfileServiceTest.java`
**Location:** `backend/src/test/java/ee/kerrete/ainterview/service/UserProfileServiceTest.java`

**Test Cases:**
```java
@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {
    @Mock UserProfileRepository userProfileRepository;
    @Mock AppUserRepository appUserRepository;
    
    @InjectMocks UserProfileService service;
    
    @Test void testGetProfile_Existing_ReturnsDto() {
        // Given: profile exists
        // When: getProfile(email)
        // Then: returns UserProfileDto with all fields
    }
    
    @Test void testGetProfile_NotExists_CreatesShellProfile() {
        // Given: profile doesn't exist, AppUser exists
        // When: getProfile(email)
        // Then: creates shell profile with email and fullName from AppUser
    }
    
    @Test void testSaveProfile_NewProfile_CreatesAndSyncsAppUser() {
        // Given: profile doesn't exist
        // When: saveProfile(email, dto)
        // Then: profile created, AppUser.fullName updated
    }
    
    @Test void testSaveProfile_UpdateExisting_UpdatesFields() {
        // Given: profile exists
        // When: saveProfile(email, dto)
        // Then: profile updated, AppUser.fullName synced
    }
    
    @Test void testCalculateCompleteness_Empty_Returns0() {
        // Given: all fields null/empty
        // When: calculateCompleteness(profile)
        // Then: returns 0
    }
    
    @Test void testCalculateCompleteness_Full_Returns100() {
        // Given: all 6 fields filled
        // When: calculateCompleteness(profile)
        // Then: returns 100
    }
    
    @Test void testCalculateCompleteness_Partial_ReturnsCorrectPercentage() {
        // Given: 3 of 6 fields filled
        // When: calculateCompleteness(profile)
        // Then: returns 50
    }
    
    @Test void testSplitSkills_CommaSeparated_ParsesCorrectly() {
        // Given: "java, spring, docker"
        // When: splitSkills(skills)
        // Then: returns ["java", "spring", "docker"]
    }
    
    @Test void testSplitSkills_NewlineSeparated_ParsesCorrectly() {
        // Given: "java\nspring\ndocker"
        // When: splitSkills(skills)
        // Then: returns ["java", "spring", "docker"]
    }
    
    @Test void testSplitSkills_Empty_ReturnsEmptyArray() {
        // Given: null or blank string
        // When: splitSkills(skills)
        // Then: returns []
    }
    
    @Test void testSplitSkills_Duplicates_RemovesDuplicates() {
        // Given: "java, Java, JAVA"
        // When: splitSkills(skills)
        // Then: returns ["java"] (case-insensitive dedupe)
    }
}
```

---

#### 5. `JobMatchServiceTest.java`
**Location:** `backend/src/test/java/ee/kerrete/ainterview/service/JobMatchServiceTest.java`

**Test Cases:**
```java
@ExtendWith(MockitoExtension.class)
class JobMatchServiceTest {
    @Mock UserProfileService userProfileService;
    @Mock CvSummaryService cvSummaryService;
    @Mock TrainingProgressRepository trainingProgressRepository;
    @Mock JobAnalysisSessionRepository jobAnalysisSessionRepository;
    @Mock ObjectMapper objectMapper;
    
    @InjectMocks JobMatchService service;
    
    @Test void testMatch_WithTargetRole_FiltersToRole() {
        // Given: targetRole = "Java"
        // When: match(request)
        // Then: returns only Java-related roles
    }
    
    @Test void testMatch_WithoutTargetRole_ReturnsAllRoles() {
        // Given: targetRole = null
        // When: match(request)
        // Then: returns all 8 roles sorted by fit desc
    }
    
    @Test void testMatch_CalculatesFitScore() {
        // Given: skills overlap, progress bonus, lastFitBonus
        // When: match(request)
        // Then: fit score = (overlapRatio + progressBonus + lastFitBonus) * 100
    }
    
    @Test void testMatch_IdentifiesStrengths() {
        // Given: candidate has "java", "spring" matching role requirements
        // When: match(request)
        // Then: strengths list contains "java", "spring"
    }
    
    @Test void testMatch_IdentifiesGaps() {
        // Given: role requires "docker" but candidate doesn't have it
        // When: match(request)
        // Then: gaps list contains "docker"
    }
    
    @Test void testMatch_WithCvText_ExtractsSkills() {
        // Given: request.cvText = "I know Java and Spring Boot"
        // When: match(request)
        // Then: cvSummaryService.extractSkills() called
    }
    
    @Test void testMatch_EmptySkills_HandlesGracefully() {
        // Given: no skills in profile or CV
        // When: match(request)
        // Then: no division by zero, fit score calculated correctly
    }
    
    @Test void testMatch_UpdatesTrainingProgress() {
        // Given: match returns results
        // When: match(request)
        // Then: TrainingProgress.lastMatchScore and lastMatchSummary updated
    }
    
    @Test void testParseJsonList_ValidJson_ParsesCorrectly() throws Exception {
        // Given: '["java", "spring"]'
        // When: parseJsonList(json)
        // Then: returns Set containing "java", "spring"
    }
    
    @Test void testParseJsonList_InvalidJson_ReturnsEmptySet() {
        // Given: malformed JSON
        // When: parseJsonList(json)
        // Then: returns empty Set
    }
    
    @Test void testBuildRoadmap_WithGaps_GeneratesDayByDay() {
        // Given: gaps = ["docker", "kubernetes"]
        // When: buildRoadmap(gaps)
        // Then: returns ["Day 1: focus on docker...", "Day 2: focus on kubernetes..."]
    }
    
    @Test void testBuildRoadmap_NoGaps_ReturnsContinuationMessage() {
        // Given: gaps = empty
        // When: buildRoadmap(gaps)
        // Then: returns ["Continue deepening current strengths..."]
    }
}
```

---

#### 6. `DashboardServiceTest.java`
**Location:** `backend/src/test/java/ee/kerrete/ainterview/service/DashboardServiceTest.java`

**Test Cases:**
```java
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock UserProfileService userProfileService;
    @Mock CvSummaryRepository cvSummaryRepository;
    @Mock TrainingProgressRepository trainingProgressRepository;
    @Mock RoadmapTaskRepository roadmapTaskRepository;
    @Mock JobAnalysisSessionRepository jobAnalysisSessionRepository;
    
    @InjectMocks DashboardService service;
    
    @Test void testGet_WithAllData_ReturnsCompleteResponse() {
        // Given: profile, progress, roadmap tasks, CV summary exist
        // When: get(email)
        // Then: DashboardResponse with all fields populated
    }
    
    @Test void testGet_NoProgress_HandlesNull() {
        // Given: no TrainingProgress
        // When: get(email)
        // Then: progress fields default to 0/null, no NPE
    }
    
    @Test void testGet_CalculatesTrainingProgressPercent() {
        // Given: totalTasks=10, completedTasks=5
        // When: get(email)
        // Then: trainingProgressPercent = 50
    }
    
    @Test void testGet_NoTasks_Returns0Percent() {
        // Given: totalTasks=0
        // When: get(email)
        // Then: trainingProgressPercent = 0 (no division by zero)
    }
    
    @Test void testGet_LastActivityFromProgress() {
        // Given: progress.lastActivityAt exists
        // When: get(email)
        // Then: lastActive = progress.lastActivityAt.toString()
    }
    
    @Test void testGet_LastActivityFromJobAnalysis() {
        // Given: no progress, but JobAnalysisSession exists
        // When: get(email)
        // Then: lastActive = session.createdAt.toString()
    }
}
```

---

#### 7. `CvSummaryServiceTest.java`
**Location:** `backend/src/test/java/ee/kerrete/ainterview/service/CvSummaryServiceTest.java`

**Test Cases:**
```java
@ExtendWith(MockitoExtension.class)
class CvSummaryServiceTest {
    @Mock CvSummaryRepository cvSummaryRepository;
    @Mock ObjectMapper objectMapper;
    
    @InjectMocks CvSummaryService service;
    
    @Test void testSaveSummary_ExtractsHeadline() {
        // Given: rawText = "John Doe\nSoftware Engineer\n..."
        // When: saveSummary(email, rawText)
        // Then: headline = "John Doe"
    }
    
    @Test void testSaveSummary_ExtractsSkills() {
        // Given: rawText contains "Java", "Spring Boot"
        // When: saveSummary(email, rawText)
        // Then: parsedSkills contains "java", "spring boot"
    }
    
    @Test void testExtractSkills_MatchesKnownSkills() {
        // Given: text contains "java", "spring", "docker"
        // When: extractSkills(text)
        // Then: returns list with these skills
    }
    
    @Test void testExtractSkills_NoMatches_UsesFallback() {
        // Given: text with capitalized words but no known skills
        // When: extractSkills(text)
        // Then: returns capitalized tokens as potential skills
    }
    
    @Test void testFindByEmail_Existing_ReturnsDto() {
        // Given: CvSummary exists
        // When: findByEmail(email)
        // Then: returns Optional.of(CvSummaryDto)
    }
    
    @Test void testFindByEmail_NotExists_ReturnsEmpty() {
        // Given: no CvSummary
        // When: findByEmail(email)
        // Then: returns Optional.empty()
    }
}
```

---

### Frontend (Jasmine/Karma)

#### 1. `auth.service.spec.ts`
**Location:** `frontend/src/app/services/auth.service.spec.ts`

**Test Cases:**
```typescript
describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let router: jasmine.SpyObj<Router>;
  
  beforeEach(() => {
    // Setup TestBed with HttpClientTestingModule
  });
  
  it('should login with email and password', () => {
    // Given: valid credentials
    // When: login(email, password)
    // Then: POST /auth/login called, token saved to localStorage with key 'token'
  });
  
  it('should login with LoginRequest object', () => {
    // Given: LoginRequest payload
    // When: login(payload)
    // Then: POST called with correct body
  });
  
  it('should save token and user data to localStorage', () => {
    // Given: successful login response
    // When: login()
    // Then: localStorage.setItem called for 'token', 'authEmail', 'authFullName', 'authUserRole'
  });
  
  it('should register new user', () => {
    // Given: RegisterPayload
    // When: register(payload)
    // Then: POST /auth/register called, auth saved
  });
  
  it('should logout and clear storage', () => {
    // Given: user logged in
    // When: logout()
    // Then: localStorage.removeItem called for all keys, router.navigate(['/login'])
  });
  
  it('should return token from localStorage', () => {
    // Given: token stored
    // When: getToken()
    // Then: returns value from localStorage.getItem('token')
  });
  
  it('should check login status', () => {
    // Given: token exists
    // When: isLoggedIn()
    // Then: returns true
  });
  
  it('should handle login errors', () => {
    // Given: API returns 401
    // When: login()
    // Then: error observable handled correctly
  });
});
```

---

#### 2. `auth.interceptor.spec.ts` ⚠️ **CRITICAL BUG TEST**
**Location:** `frontend/src/app/interceptors/auth.interceptor.spec.ts`

**Test Cases:**
```typescript
describe('AuthInterceptor', () => {
  let interceptor: HttpInterceptorFn;
  let httpMock: HttpTestingController;
  
  beforeEach(() => {
    // Setup TestBed
  });
  
  it('should add Authorization header when token exists', () => {
    // Given: localStorage has 'token' key
    // When: HTTP request made
    // Then: Authorization header added with Bearer token
  });
  
  it('should use correct localStorage key', () => {
    // ⚠️ THIS TEST WILL FAIL - BUG DETECTION
    // Given: token stored in 'token' key
    // When: interceptor runs
    // Then: reads from 'token' (not 'authToken')
    // EXPECTED: Should read 'token'
    // ACTUAL: Reads 'authToken' (BUG)
  });
  
  it('should not override existing Authorization header', () => {
    // Given: request already has Authorization header
    // When: interceptor runs
    // Then: header not modified
  });
  
  it('should not add header when no token', () => {
    // Given: no token in localStorage
    // When: HTTP request made
    // Then: request unchanged
  });
});
```

---

#### 3. `auth.guard.spec.ts`
**Location:** `frontend/src/app/guards/auth.guard.spec.ts`

**Test Cases:**
```typescript
describe('AuthGuard', () => {
  let guard: CanActivateFn;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;
  
  it('should allow access when logged in', () => {
    // Given: isLoggedIn() returns true
    // When: guard activated
    // Then: returns true
  });
  
  it('should redirect to login when not logged in', () => {
    // Given: isLoggedIn() returns false
    // When: guard activated
    // Then: router.navigate(['/login']) called, returns false
  });
  
  it('loginRedirectGuard should redirect logged-in users', () => {
    // Given: user logged in
    // When: loginRedirectGuard activated
    // Then: router.navigate(['/dashboard']) called, returns false
  });
  
  it('loginRedirectGuard should allow access when logged out', () => {
    // Given: user not logged in
    // When: loginRedirectGuard activated
    // Then: returns true
  });
});
```

---

#### 4. `dashboard.component.spec.ts`
**Location:** `frontend/src/app/pages/dashboard/dashboard.component.spec.ts`

**Test Cases:**
```typescript
describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let aiService: jasmine.SpyObj<AiService>;
  let trainingService: jasmine.SpyObj<TrainingService>;
  let authService: jasmine.SpyObj<AuthService>;
  
  it('should load dashboard data on init', () => {
    // Given: component initialized
    // When: ngOnInit()
    // Then: forkJoin calls getDashboard() and getProgress()
  });
  
  it('should display stats and progress', () => {
    // Given: API returns data
    // When: data loaded
    // Then: stats and progress displayed in template
  });
  
  it('should handle loading state', () => {
    // Given: API call in progress
    // When: component renders
    // Then: loading indicator shown
  });
  
  it('should handle errors', () => {
    // Given: API returns error
    // When: error occurs
    // Then: error message displayed
  });
  
  it('should format dates correctly', () => {
    // Given: date string
    // When: formatDate(value)
    // Then: returns locale string
  });
  
  it('should handle null dates', () => {
    // Given: null or undefined date
    // When: formatDate(value)
    // Then: returns 'N/A'
  });
});
```

---

#### 5. `login.component.spec.ts`
**Location:** `frontend/src/app/pages/auth/login.component.spec.ts`

**Test Cases:**
```typescript
describe('LoginComponent', () => {
  let component: LoginComponent;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;
  
  it('should validate form before submit', () => {
    // Given: invalid form
    // When: submit()
    // Then: form.markAllAsTouched() called, no API call
  });
  
  it('should call authService.login on valid form', () => {
    // Given: valid form
    // When: submit()
    // Then: authService.login() called with form values
  });
  
  it('should navigate to dashboard on success', () => {
    // Given: login succeeds
    // When: submit()
    // Then: router.navigate(['/dashboard']) called
  });
  
  it('should display error on failure', () => {
    // Given: login fails
    // When: submit()
    // Then: error message displayed
  });
  
  it('should show session expired message', () => {
    // Given: query param reason='session-expired'
    // When: component initialized
    // Then: sessionExpired flag set, error message shown
  });
});
```

---

## 🔗 Integration Tests

### Backend Integration Tests

#### 1. `AuthIntegrationTest.java`
**Location:** `backend/src/test/java/ee/kerrete/ainterview/integration/AuthIntegrationTest.java`

**Setup:**
```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AuthIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired AppUserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    
    @Test void testRegisterAndLoginFlow() throws Exception {
        // 1. Register new user
        // 2. Verify user saved in DB
        // 3. Login with credentials
        // 4. Verify token returned
        // 5. Use token to access protected endpoint
    }
    
    @Test void testLoginWithWrongPassword_Returns401() throws Exception {
        // Given: user exists
        // When: POST /api/auth/login with wrong password
        // Then: 401 UNAUTHORIZED
    }
    
    @Test void testRegisterDuplicateEmail_Returns409() throws Exception {
        // Given: user exists
        // When: POST /api/auth/register with same email
        // Then: 409 CONFLICT
    }
    
    @Test void testJwtTokenValidation() throws Exception {
        // Given: valid token from login
        // When: GET /api/dashboard with Authorization header
        // Then: 200 OK
    }
    
    @Test void testDisabledUserCannotLogin_Returns403() throws Exception {
        // Given: user exists but disabled
        // When: POST /api/auth/login
        // Then: 403 FORBIDDEN
    }
}
```

**How to Run:**
```bash
cd backend
./gradlew test --tests AuthIntegrationTest
```

---

#### 2. `UserProfileIntegrationTest.java`
**Location:** `backend/src/test/java/ee/kerrete/ainterview/integration/UserProfileIntegrationTest.java`

**Test Cases:**
```java
@SpringBootTest
@Transactional
class UserProfileIntegrationTest {
    @Autowired UserProfileService service;
    @Autowired UserProfileRepository profileRepository;
    @Autowired AppUserRepository userRepository;
    
    @Test void testCreateAndGetProfile() {
        // Given: AppUser exists
        // When: getProfile(email)
        // Then: shell profile created, retrieved successfully
    }
    
    @Test void testUpdateProfileSyncsAppUser() {
        // Given: profile exists
        // When: saveProfile(email, dto with new fullName)
        // Then: UserProfile.fullName updated, AppUser.fullName synced
    }
    
    @Test void testProfileCompletenessCalculation() {
        // Given: profile with 3 of 6 fields
        // When: getProfile(email)
        // Then: completeness = 50%
    }
}
```

---

#### 3. `JobMatchIntegrationTest.java`
**Location:** `backend/src/test/java/ee/kerrete/ainterview/integration/JobMatchIntegrationTest.java`

**Test Cases:**
```java
@SpringBootTest
@Transactional
class JobMatchIntegrationTest {
    @Autowired JobMatchService service;
    @Autowired UserProfileRepository profileRepository;
    @Autowired CvSummaryRepository cvRepository;
    @Autowired TrainingProgressRepository progressRepository;
    
    @Test void testFullJobMatchFlow() {
        // 1. Create UserProfile with skills
        // 2. Create CvSummary
        // 3. Call match(request)
        // 4. Verify: results returned, TrainingProgress updated
    }
    
    @Test void testJobMatchWithTargetRole() {
        // Given: targetRole = "Java"
        // When: match(request)
        // Then: only Java roles returned
    }
    
    @Test void testJobMatchUpdatesTrainingProgress() {
        // Given: match returns results
        // When: match(request)
        // Then: TrainingProgress.lastMatchScore and lastMatchSummary persisted
    }
}
```

---

#### 4. `SecurityIntegrationTest.java`
**Location:** `backend/src/test/java/ee/kerrete/ainterview/integration/SecurityIntegrationTest.java`

**Test Cases:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    @Autowired MockMvc mockMvc;
    
    @Test void testPublicEndpointsAccessible() throws Exception {
        // GET /api/auth/** without token
        // Verify: 200 OK (or appropriate response)
    }
    
    @Test void testProtectedEndpointsRequireAuth() throws Exception {
        // GET /api/dashboard without token
        // Verify: 401 UNAUTHORIZED
    }
    
    @Test void testCorsHeaders() throws Exception {
        // OPTIONS /api/dashboard
        // Verify: CORS headers present
    }
    
    @Test void testJwtFilterChain() throws Exception {
        // Given: valid token
        // When: GET /api/dashboard with Authorization header
        // Then: 200 OK
    }
}
```

---

### Frontend E2E Tests (Cypress Recommended)

#### Setup Required:
```bash
cd frontend
npm install --save-dev cypress
npx cypress open  # Initialize Cypress
```

#### 1. `auth.cy.ts`
**Location:** `frontend/cypress/e2e/auth.cy.ts`

**Test Cases:**
```typescript
describe('Authentication Flow', () => {
  it('should register new user', () => {
    cy.visit('/register');
    cy.get('[data-cy=email]').type('test@example.com');
    cy.get('[data-cy=password]').type('password123');
    cy.get('[data-cy=fullName]').type('Test User');
    cy.get('[data-cy=submit]').click();
    cy.url().should('include', '/dashboard');
    cy.window().its('localStorage').should('have.property', 'token');
  });
  
  it('should login existing user', () => {
    cy.visit('/login');
    cy.get('[data-cy=email]').type('test@example.com');
    cy.get('[data-cy=password]').type('password123');
    cy.get('[data-cy=submit]').click();
    cy.url().should('include', '/dashboard');
  });
  
  it('should show validation errors', () => {
    cy.visit('/login');
    cy.get('[data-cy=submit]').click();
    cy.get('[data-cy=email-error]').should('be.visible');
  });
  
  it('should logout', () => {
    // Login first
    cy.login('test@example.com', 'password123');
    cy.get('[data-cy=logout]').click();
    cy.url().should('include', '/login');
    cy.window().its('localStorage').should('not.have.property', 'token');
  });
  
  it('should protect routes', () => {
    cy.visit('/dashboard');
    cy.url().should('include', '/login');
  });
});
```

**How to Run:**
```bash
cd frontend
npm run cypress:open   # Interactive
npm run cypress:run    # Headless CI mode
```

---

#### 2. `dashboard.cy.ts`
**Location:** `frontend/cypress/e2e/dashboard.cy.ts`

**Test Cases:**
```typescript
describe('Dashboard', () => {
  beforeEach(() => {
    cy.login('test@example.com', 'password123');
  });
  
  it('should load dashboard data', () => {
    cy.visit('/dashboard');
    cy.get('[data-cy=stats]').should('be.visible');
    cy.get('[data-cy=progress]').should('be.visible');
  });
  
  it('should handle API errors', () => {
    cy.intercept('GET', '**/api/dashboard', { statusCode: 500 });
    cy.visit('/dashboard');
    cy.get('[data-cy=error]').should('be.visible');
  });
  
  it('should navigate to other pages', () => {
    cy.visit('/dashboard');
    cy.get('[data-cy=nav-profile]').click();
    cy.url().should('include', '/profile');
  });
});
```

---

#### 3. `job-match.cy.ts`
**Location:** `frontend/cypress/e2e/job-match.cy.ts`

**Test Cases:**
```typescript
describe('Job Match', () => {
  beforeEach(() => {
    cy.login('test@example.com', 'password123');
  });
  
  it('should match jobs', () => {
    cy.visit('/job-match');
    cy.get('[data-cy=job-description]').type('Looking for Java developer');
    cy.get('[data-cy=submit]').click();
    cy.get('[data-cy=matches]').should('be.visible');
  });
  
  it('should filter by score', () => {
    cy.visit('/job-match');
    // Perform match first
    cy.get('[data-cy=min-score-slider]').setValue(50);
    cy.get('[data-cy=matches]').children().should('have.length.greaterThan', 0);
  });
});
```

---

## 📝 Manual Test Checklist

### Authentication Flow

#### Registration
- [ ] **TC-AUTH-001**: Register with valid email, password, fullName → Success, redirected to dashboard
- [ ] **TC-AUTH-002**: Register with duplicate email → Error message "User with this email already exists"
- [ ] **TC-AUTH-003**: Register with mismatched passwords → Error "Passwords do not match"
- [ ] **TC-AUTH-004**: Register with invalid email format → Validation error
- [ ] **TC-AUTH-005**: Register with blank fields → Field-level validation errors
- [ ] **TC-AUTH-006**: Register without confirmPassword → Still succeeds (optional field)

#### Login
- [ ] **TC-AUTH-007**: Login with valid credentials → Success, token stored in `localStorage` with key `token`, redirected to dashboard
- [ ] **TC-AUTH-008**: Login with wrong password → Error "Invalid credentials"
- [ ] **TC-AUTH-009**: Login with non-existent email → Error "Invalid credentials"
- [ ] **TC-AUTH-010**: Login with disabled user → Error "User is disabled"
- [ ] **TC-AUTH-011**: Login with invalid email format → Validation error
- [ ] **TC-AUTH-012**: ⚠️ **CRITICAL**: Verify token is sent in API requests after login (check Network tab, Authorization header should have Bearer token)

#### Logout & Session
- [ ] **TC-AUTH-013**: Click logout → Token cleared, redirected to login
- [ ] **TC-AUTH-014**: Close browser → Token persists (localStorage)
- [ ] **TC-AUTH-015**: Open new tab → Still logged in (token shared)
- [ ] **TC-AUTH-016**: Expired token → API returns 401, redirects to login

#### Route Protection
- [ ] **TC-AUTH-017**: Access /dashboard without login → Redirected to /login
- [ ] **TC-AUTH-018**: Access /login when logged in → Redirected to /dashboard
- [ ] **TC-AUTH-019**: Access /register when logged in → Allowed (no guard)

---

### User Profile

#### View Profile
- [ ] **TC-PROFILE-001**: View profile after registration → Shows shell profile with email/name
- [ ] **TC-PROFILE-002**: View profile completeness → Shows 0% initially
- [ ] **TC-PROFILE-003**: View profile for existing user → Shows saved data

#### Update Profile
- [ ] **TC-PROFILE-004**: Update fullName → Saved, syncs with AppUser (verify in DB or via API)
- [ ] **TC-PROFILE-005**: Update currentRole → Saved
- [ ] **TC-PROFILE-006**: Update targetRole → Saved
- [ ] **TC-PROFILE-007**: Update skills (comma-separated) → Parsed correctly
- [ ] **TC-PROFILE-008**: Update skills (newline-separated) → Parsed correctly
- [ ] **TC-PROFILE-009**: Update yearsOfExperience → Saved as integer
- [ ] **TC-PROFILE-010**: Update bio → Saved as text
- [ ] **TC-PROFILE-011**: Update all fields → Completeness shows 100%
- [ ] **TC-PROFILE-012**: Update partial fields → Completeness calculates correctly (e.g., 3/6 = 50%)

---

### Job Matching

#### Job Match Analysis
- [ ] **TC-JOB-001**: Match without targetRole → Returns all roles sorted by fit
- [ ] **TC-JOB-002**: Match with targetRole "Java" → Returns only Java-related roles
- [ ] **TC-JOB-003**: Match with CV text → Extracts skills from CV
- [ ] **TC-JOB-004**: Match with user profile skills → Includes profile skills
- [ ] **TC-JOB-005**: Match with job analysis strengths → Includes previous analysis
- [ ] **TC-JOB-006**: Match displays fit score → Shows percentage (0-100)
- [ ] **TC-JOB-007**: Match displays strengths → Lists matching skills
- [ ] **TC-JOB-008**: Match displays gaps → Lists missing required skills
- [ ] **TC-JOB-009**: Match displays roadmap → Shows day-by-day plan
- [ ] **TC-JOB-010**: Match updates training progress → Last match score saved (verify via API)

#### Job Match UI
- [ ] **TC-JOB-011**: Enter job description → Form validates required field
- [ ] **TC-JOB-012**: Submit without job description → Shows validation error
- [ ] **TC-JOB-013**: Filter by minScore → Only shows matches above threshold
- [ ] **TC-JOB-014**: Select match card → Shows detailed view
- [ ] **TC-JOB-015**: CV text cached → Persists after page refresh

---

### Dashboard

#### Dashboard Data
- [ ] **TC-DASH-001**: Load dashboard after login → Shows stats and progress
- [ ] **TC-DASH-002**: Dashboard shows user email → Displays current user
- [ ] **TC-DASH-003**: Dashboard shows user name → Displays fullName
- [ ] **TC-DASH-004**: Dashboard handles loading state → Shows spinner
- [ ] **TC-DASH-005**: Dashboard handles API error → Shows error message
- [ ] **TC-DASH-006**: Dashboard formats dates → Displays readable date/time
- [ ] **TC-DASH-007**: Dashboard calculates training progress → Shows correct percentage
- [ ] **TC-DASH-008**: Dashboard shows CV uploaded status → Correctly indicates if CV exists

#### Dashboard Navigation
- [ ] **TC-DASH-009**: Click "Upload CV" → Navigates to /upload-cv
- [ ] **TC-DASH-010**: Click "Job Match" → Navigates to /job-match
- [ ] **TC-DASH-011**: Click "Profile" → Navigates to /profile

---

### CV Upload

#### CV Processing
- [ ] **TC-CV-001**: Upload PDF CV → Extracts text and skills
- [ ] **TC-CV-002**: Upload invalid file → Shows error
- [ ] **TC-CV-003**: Upload large file → Handles gracefully or shows size limit
- [ ] **TC-CV-004**: CV summary saved → Appears in dashboard/job match

---

### Security & Edge Cases

#### Security
- [ ] **TC-SEC-001**: Access protected endpoint without token → Returns 401
- [ ] **TC-SEC-002**: Access protected endpoint with invalid token → Returns 401
- [ ] **TC-SEC-003**: Access protected endpoint with expired token → Returns 401
- [ ] **TC-SEC-004**: CORS preflight (OPTIONS) → Returns CORS headers
- [ ] **TC-SEC-005**: Email parameter injection → Cannot bypass SecurityContext (verify manually by trying ?email=other@example.com)

#### Edge Cases
- [ ] **TC-EDGE-001**: Empty skills array → No division by zero errors
- [ ] **TC-EDGE-002**: Very long text fields → Handles gracefully
- [ ] **TC-EDGE-003**: Special characters in skills → Parsed correctly
- [ ] **TC-EDGE-004**: Concurrent profile updates → No data loss (test with 2 browser tabs)
- [ ] **TC-EDGE-005**: Network timeout → Shows error message
- [ ] **TC-EDGE-006**: Server error (500) → Shows user-friendly error
- [ ] **TC-EDGE-007**: Null/empty email in requests → Handles gracefully

---

## 🚀 Running Tests

### Backend Tests
```bash
cd backend

# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests AuthServiceTest

# Run integration tests only
./gradlew test --tests "*IntegrationTest"

# Run with coverage (requires JaCoCo plugin - add to build.gradle)
./gradlew test jacocoTestReport
# Coverage report: build/reports/jacoco/test/html/index.html
```

### Frontend Unit Tests
```bash
cd frontend

# Run tests (watch mode)
npm test

# Run tests once
npm test -- --watch=false

# Run with coverage
npm test -- --code-coverage
# Coverage report: coverage/index.html
```

### Frontend E2E Tests (after setup)
```bash
cd frontend

# Cypress
npm run cypress:open   # Interactive
npm run cypress:run    # Headless CI mode

# Add to package.json scripts:
# "cypress:open": "cypress open",
# "cypress:run": "cypress run"
```

---

## 📊 Test Coverage Goals

- **Backend Unit Tests**: 80%+ coverage for services and controllers
- **Backend Integration Tests**: 70%+ coverage for critical flows
- **Frontend Unit Tests**: 70%+ coverage for services and components
- **E2E Tests**: Cover all critical user journeys

---

## 🔍 Regression Test Priorities

When backend/frontend agents make changes, prioritize testing:

1. **Authentication changes** → Run full auth test suite + manual TC-AUTH-001 to TC-AUTH-019
2. **Security config changes** → Run SecurityIntegrationTest + manual security checklist (TC-SEC-001 to TC-SEC-005)
3. **Service logic changes** → Run unit + integration tests for that service
4. **Controller changes** → Run controller unit tests + integration tests
5. **Frontend service changes** → Run service unit tests + affected component tests
6. **Route/guard changes** → Run guard tests + E2E navigation tests
7. **Database schema changes** → Run integration tests + verify Liquibase migrations

---

## 🐛 Known Issues to Test Around

1. **🔴 CRITICAL: Auth Interceptor Bug**: Uses `authToken` key but AuthService uses `token` → **Fix required before production**
2. **Email Resolution**: Controllers accept email param that can bypass SecurityContext → Verify manually (TC-SEC-005)
3. **CORS Wildcard**: `@CrossOrigin(origins = "*")` → Test CSRF scenarios manually
4. **Password Confirmation**: Optional in backend but validated if present → Test both cases (TC-AUTH-003, TC-AUTH-006)
5. **JWT Secret Hardcoded**: Security risk → Move to environment variable

---

## 📝 Notes

- All backend tests should use H2 in-memory database
- Use `@Transactional` for integration tests to avoid DB pollution
- Mock external services (OpenAI API) in tests
- Frontend tests should mock HTTP calls
- E2E tests should use test database/backend instance
- Consider adding test data fixtures for consistent testing
- Add `@DataJpaTest` for repository tests if needed
- Use `@MockBean` for Spring context tests, `@Mock` for unit tests

---

## 🔧 Quick Fixes Needed

### 1. Fix Auth Interceptor Token Key (CRITICAL)
```typescript
// frontend/src/app/interceptors/auth.interceptor.ts:8
// CHANGE FROM:
const token = localStorage.getItem('authToken');
// TO:
const token = localStorage.getItem('token');
```

### 2. Move JWT Secret to Environment Variable
```java
// backend/src/main/java/ee/kerrete/ainterview/auth/JwtService.java
@Value("${jwt.secret:super-secret-key-change-me-please-1234567890}")
private String secretKey;

private Key signingKey = Keys.hmacShaKeyFor(secretKey.getBytes());
```

---

**Last Updated**: 2025-12-08
**Next Review**: After auth interceptor fix and JWT secret migration
