# QA Notes – Soft Skill Merger v1.0

Route: `/soft-skill-merger` (redirect from `/soft-skills/merger`), requires login.

Manual sanity steps:
- Paste sample JSON on the page (use the built-in “Paste example” button).
- Submit while logged in; expect a green success banner and merged profile card.
- Verify the UI shows a non-empty summary, at least one strength pill, a risks list (may be empty but rendered), and at least one dimension card with score and confidence chip.
- Toggle “Persist merged profile” on/off to confirm both flows succeed.
- Error path: clear JSON textarea and submit → shows validation message without crashing.

Backend endpoint expectation:
- `POST /api/soft-skills/merge` (falls back to `/api/soft-skill/merge` on 404/405).
- Response must include `mergedProfile.summary`, `strengths[]`, `risks[]`, `dimensionScoresMerged[]` with `mergedScore` 1–5 and `confidence` 0–1, plus optional `meta.overallConfidence`.

