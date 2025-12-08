# Build frontend

You are the QA agent.
Work ONLY inside: ai-interview-fullstack/frontend

Steps:
1) Change directory to the frontend folder.
2) Run:
   npm install
   npm run build

3) If the build fails:
   - List the errors grouped by file.
   - For each error, show:
     - File path and line
     - Component or module name
     - The exact TypeScript/Angular/template error message
   - Identify if the issue is:
     - Type error in TS
     - Invalid template binding
     - Missing import / module
     - Build config issue

4) Suggest a short prompt for the FRONTEND agent that points to:
   - The files to check
   - The type of fix needed.
