# Project: ai-interview-fullstack
# Claude Prompt for Development Assistance

This prompt describes the entire project context so I can ask for fixes, improvements, refactors, UI design, bug analysis or code generation at any time.

-----------------------------------------------------
PROJECT OVERVIEW
-----------------------------------------------------

Full-stack web application (AI Interview Mentor) consisting of:

Backend:
- Spring Boot 3, Java 17
- H2 database (dev)
- Liquibase migrations
- JWT authentication (login/register)
- Services: CV upload, job analysis, interview training, skill evaluation

Frontend:
- Angular + TypeScript + Tailwind
- Standalone components
- New dark login/register UI

Development tools:
- Cursor (4-agent workflow: Architect, Backend, Frontend, QA)
- Claude Desktop for Windows for external reasoning or large code-generation tasks

-----------------------------------------------------
RULES FOR CLAUDE RESPONSES
-----------------------------------------------------
1. Output FULL FILES when I ask for code changes.
2. Use simple, step-by-step guidance.
3. Assume I am often tired, high or cognitively overloaded — instructions must be LOW FRICTION.
4. When I paste errors (Java, Angular, Liquibase), analyze them and give:
   - exact cause
   - exact fix
   - file paths
   - full replacements if needed
5. For UI work, use Tailwind, clean modern look, dark mode default.
6. For backend, create new Liquibase files instead of modifying old ones.
7. For Angular, ensure template + component class always match exactly.
8. Avoid long theory — focus on practical fixes.

-----------------------------------------------------
WHEN I SAY:
"fix this"
"make it clean"
"use full files"
"refactor this"
"give me UI"
"help debug"

→ You ALWAYS respond with precise code, steps and instructions.

-----------------------------------------------------
MY CURRENT STATE
-----------------------------------------------------
- Backend runs on port 8080 and works.
- Frontend runs on 4200 and login UI loads.
- Auth components may still need fixes (template mismatches, submit handlers, validators).
- Next major goals:
  1) Fully working login/register flow
  2) Minimal dashboard page
  3) Job analysis + training UI

-----------------------------------------------------
STARTUP COMMANDS (so Claude can reference them)

Backend:
  cd backend
  ./gradlew bootRun

Frontend:
  cd frontend
  npm install
  npm start

-----------------------------------------------------
END OF PROMPT
-----------------------------------------------------
