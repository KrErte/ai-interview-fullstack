# QA Checklist – Soft Skill Merger (Merged Profile v0.1)

## Manual acceptance steps

1) Login  
   - User: `admin@local.test` (or available demo admin).  
   - Ensure JWT auth works (existing flow).

2) Seed evaluations  
   - Navigate to “Soft Skill Merger” → “New Evaluation”.  
   - Create at least two evaluations for `candidate@example.com` / job context “Senior Backend Engineer”:  
     - HR: communication=80, ownership=90.  
     - Tech Lead: communication=60, ownership=70, stress_management=70.  
   - Save each evaluation; confirm 201 responses in Network tab.

3) View merged profile  
   - Open “Merged Profile” tab.  
   - Enter email `candidate@example.com`, click “Load profile”.  
   - Expected: dimensions listed; averages:  
     - communication → avg 70 (2 ratings)  
     - ownership → avg 80 (2 ratings)  
     - stress_management → avg 70 (1 rating)  
   - Dimensions sorted alphabetically by key/label.

4) Empty state  
   - Query `no-data@example.com` → should show empty dimensions, no errors, status 200.

5) Validation  
   - Call endpoint without `email` → expect 400 with message about missing email.

6) Regression spot-check  
   - Smoke other soft-skill endpoints (`/api/soft-skill/evaluations`, `/api/soft-skills/profile`) still behave as before.  
   - Optional: frontend build `npm run build` to ensure UI still compiles.

## Notes
- Backend endpoint under test: `GET /api/soft-skill/merged-profile?email=...` (requires auth in normal operation).  
- Test data from above can be re-used for automated assertions.  
- Counts/averages are computed live from stored evaluations; no pre-existing merged record is required.

