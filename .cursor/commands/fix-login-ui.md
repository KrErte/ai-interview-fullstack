# Fix login UI (dark glass auth)

You are the FRONTEND agent.
Work ONLY inside: ai-interview-fullstack/frontend

Goal:
Create a modern dark, glassmorphism-style login experience that matches the existing dark dashboard theme, without breaking forms or routing.

Steps:
1) Inspect the current auth-related files:
   - Login component TS/HTML/SCSS
   - Auth layout / shell
   - Any shared auth UI pieces

2) Implement:
   - Full-screen dark gradient background
   - Centered auth card with glass/blur effect
   - Clean layout with Tailwind utilities for spacing and alignment
   - Rounded corners, subtle shadows, hover/focus transitions

3) Form behavior:
   - Keep existing reactive form structure (FormGroup / FormControl names).
   - Keep validators and error messages.
   - Ensure submit handler still works and calls the right service.

4) Styling rules:
   - Use Tailwind for layout, spacing, colors and typography.
   - Use SCSS for more complex styling if necessary (e.g. glass effect).
   - Do NOT modify angular.json or Tailwind/PostCSS config files.

5) After changes:
   - Ensure the app compiles (no template errors).
   - Briefly describe:
     - Which components/files were changed
     - What the UI looks like now
     - Any follow-up backend needs (if any).
