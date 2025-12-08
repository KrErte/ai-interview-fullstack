# Task: Full login flow debug (backend + frontend)

## Goal
End-to-end: login UI works, calls the correct backend endpoint, backend returns correct statuses, and all login tests pass.

## Plan
1) QA agent:
   - Run backend login tests and summarize failures.
   - Run frontend build and check for auth-related template errors.

2) BACKEND agent:
   - Fix backend login behavior and exception handling so tests pass.
   - Ensure the API contract (URL, request body, response body, status codes) is clearly defined.

3) FRONTEND agent:
   - Make sure the login page:
     - Sends correct request to the backend
     - Handles all status codes appropriately (wrong password, disabled user, etc.)
     - Shows user-friendly error messages.

4) QA agent:
   - Re-run backend login tests.
   - Optionally run a manual test scenario description for logging in from the UI.

## Recommended agent prompts

### QA agent (step 1)
Run both:
  ./gradlew test --tests ""*Login*""
and
  npm run build
Then summarize backend and frontend issues separately.

### BACKEND agent (step 2)
Based on QA report, fix backend login behavior and exception handling so all login-related tests pass, without modifying test code. Then run:
  ./gradlew test --tests ""*Login*""

### FRONTEND agent (step 3)
Ensure the login UI calls the correct backend endpoint, sends the expected payload, and handles all relevant HTTP status codes by showing appropriate error messages on the form.
