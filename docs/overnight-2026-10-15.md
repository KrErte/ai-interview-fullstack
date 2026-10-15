# Night work plan – 2026-10-15

The user asked for this work while they sleep: fix the bugs, analyse the app, and build new features without asking questions. The work follows the agreed product direction: careerrisk.ee / Futureproof is the AI learning roadmap for your job, for individuals. The test is the way in and the roadmap is the product. There is no B2B or team mode.

## Ground rules for tonight

- Every change goes on its own branch with its own PR. Nothing is merged to `main` and nothing is deployed.
- No new database tables, accounts, paid steps or email walls. Saved state stays in the browser.
- The old `frontend/` is not touched. Brand strings are left alone because PR #50 changes them.
- Some uncommitted changes in the main checkout (`AiReadinessCoach`, `AiReadinessService`, `OpenAiClient`) are not mine. Tonight's work happens in a separate worktree and does not touch those changes.
- Before each PR: backend tests (`./gradlew test`), the `onet-import` tests and the `frontend-risk` production build.

## Order

1. **Content (done).** All 899 occupations have their own roadmap, and the 117 formula endings are rewritten (PR #51).
2. **Health check.** Run the full backend test suite and the frontend build, read the test flow and fix every real bug found. Each fix gets its own commit with a test where one is practical.
3. **Saved results show today's roadmap.** A result saved before the content update still shows the old group template. Store the answers with the saved result and refresh it from the server when it is opened. This uses no language-model call and needs no account.
4. **Roadmap progress.** Add a tick box to each roadmap item and project, plus a progress line for each phase, saved in the browser for each profession. This turns the plan into something people come back to (in the spirit of roadmap.sh).
5. **"Start this week" box.** Above the roadmaps, name the single task with the highest share × automation. Suggest doing it once this week with the first AI tool, and show the first roadmap project under it. This answers "where do I start?" in one step.
6. **Report.** Write `docs/overnight-report-2026-10-15.md`: branches, PRs, tests and results, what could not be checked in a browser, and the backlog.

## Backlog (not tonight)

- Weekly reminder (calendar `.ics` file) for the hours chosen in the test.
- A shareable link to the public `/jobs/{slug}` roadmap from the result page.
- Letting the user adjust task shares when their week differs from the catalogue.
- Estonian translation of the catalogue (899 roadmaps).
