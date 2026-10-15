# Night report – 2026-10-15

Asked for: fix the bugs, analyse the app, think of new features and build them, without asking questions. All work is in pull requests. Nothing is merged to `main` and nothing is deployed.

## Pull requests, in suggested review order

| PR | Base | What it does |
|----|------|--------------|
| #51 | main | Occupation-specific roadmaps for all 899 occupations. Tonight it also rewrote the 117 formula endings ("Grow toward X, or look at related roles") to say why the next role fits. The title is updated. |
| #53 | main | **Bug fix.** On `/jobs/{slug}` the summary said AI does "mostly: season and cook food" for cooks, a task that is 12% automatable. It now names a task only at 40% or more automation. |
| #52 | main | Result page as a plan: saved results are recalculated with today's catalog, roadmap steps can be ticked off with a progress count, and a "start this week" box is added. Fixes a carry-over of the AI comment choice in `restart()`. |
| #54 | #52 | "Continue your plan" card on the home page for returning visitors, and `/ai-test?view=saved`. |
| #55 | #54 | Weekly practice reminder as a calendar file (.ics), built in the browser. |

#52, #54 and #55 are stacked. Merge them in order and GitHub retargets the next one. #50 (brand) is untouched and does not conflict with these files, except the brand strings it already owns.

## Tests run

- Backend `./gradlew test`: 377 tests on the roadmap branch and 380 on #53, all passing.
- `onet-import` tests: 83 passing. `catalog.py` reports 899 whole roadmaps and 0 below the quality gate.
- Frontend `ng test` (Chrome headless): 80 at the start; 90 on #52, 92 on #54 and 96 on #55, all passing.
- Frontend `npm run build`: passing on every branch.
- CI (GitHub Actions) is green on #51, #52 and #53. The stacked #54 and #55 do not get CI runs because their base is not `main`.

## Checked in a browser (local backend and `ng serve`)

- A stale saved result, seeded with a fake old roadmap, was replaced by the current roadmap, and its AI comment stayed.
- A saved result without answers shows the retake hint.
- Ticks survive a reload, and the phase and total counts are correct.
- "Start this week": Data Entry Keyers get a task to try (72% automatable). Cooks get the first roadmap item instead.
- Phone width (390 px): the new boxes show no sideways scroll.
- The home page card opens the saved result.
- The .ics content is a correct weekly event.
- `/jobs/cooks-restaurant` renders. I found the summary bug there (#53).
- API edge cases: unknown question or option, missing fields, bad JSON and unknown profession all give 400. Search limits are clamped.

Not checked: importing the .ics into Google Calendar or Outlook. Only the RFC 5545 rules are covered by unit tests.

## Findings that need your decision (not changed)

1. **The local H2 database file is in git** (`backend/data/aiinterview-db.mv.db` and `.trace.db`). A fresh checkout fails `gradlew bootRun` with Liquibase "Table DATABASECHANGELOG already exists". Docker and production use PostgreSQL and are not affected. The fix is `git rm --cached backend/data/*.db` plus a `.gitignore` entry. That would delete the file from other people's working copies on their next pull, so I left it. Tonight I ran locally with `SPRING_DATASOURCE_URL=jdbc:h2:mem:...`.
2. **Uncommitted changes in the main checkout:** `AiReadinessCoach.java`, `AiReadinessService.java`, `OpenAiClient.java` (51 lines). They are not mine. I worked in separate worktrees (`.claude/worktrees/*`) and did not touch them.
3. **Commit `8985d77` on #51** has an invisible BOM at the start of its title. Fixing it needs a force-push of that branch.

## Backlog

- A share link from the result to the public `/jobs/{slug}` plan. It needs the slug in the API, which means touching `AiReadinessService`, which has the uncommitted changes above.
- Letting people adjust task shares when their week differs from the catalogue.
- Calling the result's "Starter plan" an occupation roadmap once #51 is merged.
- Estonian translation of the catalogue (899 roadmaps). The site is in English now, so this is a direction question for you.
