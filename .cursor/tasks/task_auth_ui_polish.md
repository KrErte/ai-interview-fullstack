# Task: Polish auth UI (login & register)

## Goal
Make login and register pages visually consistent with the dark dashboard, using a modern glassmorphism style, while preserving all existing auth functionality.

## Plan
1) Ask the FRONTEND agent to:
   - Inspect login and register components (TS/HTML/SCSS).
   - Introduce a shared auth layout if useful.
   - Implement:
     - Dark gradient background
     - Centered glass card
     - Tailwind-based spacing and typography

2) Ensure forms:
   - Keep existing FormGroup structures and validators.
   - Display clear validation errors.
   - Maintain navigation and routing.

3) Optionally ask QA agent to:
   - Run npm run build in the frontend folder
   - Report any template/TS errors.

## Recommended agent prompts

### FRONTEND agent (main)
Apply a modern dark glassmorphism layout to login and register pages using Tailwind + SCSS, without changing routing or breaking reactive forms. Ensure templates compile.

### QA agent (optional)
In the frontend folder, run:
  npm install
  npm run build
and report any template or TypeScript errors related to auth pages.
