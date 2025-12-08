# Task: Fix backend login flow and tests

## Goal
Ensure all backend login-related tests pass and the login API behaves as expected for all edge cases (invalid email format, disabled user, wrong password, blank fields, non-existent user, etc.).

## Plan
1) Ask the QA agent to run focused backend tests:
   - Command: run-backend-tests (or manually ./gradlew test --tests ""*Login*"")

2) Use the QA summary to understand:
   - Which login tests are failing
   - Root causes and stack traces

3) Ask the BACKEND agent to:
   - Read AuthControllerLoginTest, AuthController, AuthService and related classes.
   - Fix status codes and exception handling to match the tests.
   - Possibly add/update @ControllerAdvice for authentication errors.
   - Run ./gradlew test --tests ""*Login*"" and show diffs.

4) If new failures appear, iterate with QA + BACKEND again until all login tests pass.

## Recommended agent prompts

### QA agent (step 1)
Run:
  Command: run-backend-tests

### BACKEND agent (step 3)
Fix all failing login tests by aligning AuthController/AuthService behavior with AuthControllerLoginTest expectations. Do not change test code. Use proper exceptions and HTTP statuses, and then run:
  ./gradlew test --tests ""*Login*""
and show file diffs.
