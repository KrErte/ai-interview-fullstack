# Run backend tests

You are the QA agent.
Work ONLY inside: ai-interview-fullstack/backend

Steps:
1) Change directory to the backend folder.
2) Run:
   ./gradlew clean test
3) Wait for the build to finish.
4) Summarize:
   - Overall result (SUCCESS or FAILED)
   - Number of failed tests
   - For each failure:
     - Test class and method
     - Exception type and main message
     - The most relevant stack trace location (class + line)
5) Identify if the failure is most likely caused by:
   - Database / Liquibase
   - Service / business logic
   - Controller / HTTP status / validation
   - Configuration (Security, profiles, etc.)
6) Suggest which agent should fix it next:
   - BACKEND agent (for code/DB issues)
   - FRONTEND agent (if the test is actually an API usage mismatch)
7) Propose a very short, copy-pasteable prompt the user can send to that agent.
