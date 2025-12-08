# Fix backend login tests

You are the BACKEND agent.
Work ONLY inside: ai-interview-fullstack/backend

Goal:
Make all login-related backend tests pass WITHOUT changing the test code.

Steps:
1) Read these files carefully:
   - AuthControllerLoginTest.java
   - AuthController.java
   - AuthService.java
   - Any related exception/DTO/security classes used by login.

2) Understand the test expectations:
   - Which HTTP statuses are expected for:
     - valid login
     - invalid email format
     - non-existent user
     - disabled user
     - wrong password
     - blank email/password
   - What JSON structure (fields) is expected in the response, if any.

3) Find why the current implementation fails:
   - Is the controller throwing IllegalArgumentException or another wrong exception?
   - Is the HTTP status incorrect?
   - Is response body structure wrong?

4) Implement the minimal fix:
   - Prefer using a dedicated exception type or result object.
   - If appropriate, add or update a global exception handler with @ControllerAdvice.
   - Map login failures to the correct HTTP statuses and messages required by the tests.
   - Do NOT change the test code.

5) After changes, run:
   ./gradlew test --tests "*Login*"

6) Summarize:
   - Which files were modified
   - What the behavioral change is
   - Test results (passed/failed)
   - Show a clear diff of all modified files.
