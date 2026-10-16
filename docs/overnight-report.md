# Report – feature/your-week

Steps 1–4 from `docs/claude-next.md` (sections 0–3 in that file). The branch is local only: nothing is pushed, nothing is merged, and `overnight/rbai-gap` is not touched or rebased. `frontend/`, `old-ui/` and `frontend/uus/` are not touched.

## Branch and base

- Branch: `feature/your-week`, in the worktree `.claude/worktrees/your-week`.
- **Base: `origin/main` at `5a208a4`**, not the main checkout's `HEAD`.
  - The main checkout is on `feat/occupation-roadmaps` (`a3c3236`). That branch is already merged into `main`, and `main` has since gained #50 and #52–#58.
  - The result page (`ai-check.page.ts`) changed a lot in #52, #54 and #55. Building on `a3c3236` would have created the same kind of conflict that `claude-next.md` warns about for `overnight/rbai-gap`.
  - `origin/main` contains everything in that `HEAD`, so this is the safer reading of "on top of the current HEAD".
- **The main checkout has uncommitted work from another session that `claude-next.md` treats as present:**
  - `LlmRateLimiter`;
  - the search ranking test for "truck driver" / "coder";
  - a logout change in `auth.service.ts`;
  - edits in `privacy.page.ts`, `professions.json` and `application.yml`.

  None of it is in this branch, and I did not touch those files in the main checkout. Whoever commits that work will see overlaps in `auth.service.ts`, `privacy.page.ts` and `AiReadinessService.java` (see choice 2).

## Commits

| # | Commit | Step |
|---|--------|------|
| 1 | `3a3273a` Keep the test result on logout and say so in the privacy policy | 1 (section 0) |
| 2 | `656750c` Let people set their own task shares with sliders | 2 (section 1) |
| 3 | `a30685e` Let people skip tasks they already do with AI in the first step | 3 (section 2) |
| 4 | this report | 4 (section 3) |

## Tests run

- Backend `./gradlew test` (full suite): 387 tests, 0 failures. The aireadiness tests:
  - `AiReadinessScorerTest`: 12 tests, 4 of them new.
    - Missing `taskShares` gives the same result for every catalog profession.
    - A valid override changes exposure but not the adjustment or readiness.
    - Catalog shares sent as an override give an identical result.
    - Wrong total, unknown name, missing task, out-of-range value and an extra key are rejected.
  - `AiReadinessAssessWebMvcTest`: 3 tests, all new. They cover JSON through the controller: no shares, a valid override, and 400 with "Missing share" / "Unknown task".
  - `AiReadinessCatalogTest` (8), `AiReadinessSearchTest` (14) and `AiReadinessFeaturesTest` (2): unchanged, all pass.
- Frontend `ng test` (Chrome headless): 111 of 111 pass. That includes new specs for:
  - `rescaleShares`: the total stays 100 over 200 random moves, the remainder goes to the largest other share, values are clamped and rounded;
  - `previewResult`;
  - first-step marks;
  - the logout and sign-in storage rules;
  - the storage of marks.
- Frontend production build (`ng build`): passes, 1.56 MB initial.

## Checked in a browser (local backend with in-memory H2, `ng serve`)

- **One slider move.** Data Entry Keyers, main task moved from 54% to 5%:
  - the ring went from 70 to 65 at once;
  - the server answer was also 65;
  - exactly one request, with `personalize: false` and shares totalling 100;
  - the AI comment stayed;
  - the six visible rows did not change.
- **Quick drag.** Six moves within 300 ms sent one request, carrying the final shares.
- **Reload.** The custom shares came back ("Your week", score 73).
- **Reset.** "Reset to the typical week" returned the score to 70 and dropped the saved shares.
- **Marks.** "I already do this with AI" stepped through all 7 tasks at 40% or more. The score stayed at 73 throughout, the one-sentence message appeared at the end, and "Clear" brought the first task back.
- **Phone width (390 px).** Sliders fit with no sideways scroll.

## Not checked in a browser

- Keyboard-only use and screen readers on the sliders. The sliders are native `input type=range` elements with `aria-label` and `aria-valuetext`, but this was not tested.
- A real AI comment from the language model, followed by slider moves. Only a stored fake comment was used to check that the comment is kept.
- Logging in as a different person with the real login form. That rule is covered by unit tests only.
- An old saved result without answers, combined with sliders. The code previews and saves locally without a server call; there is no browser test.
- Production. Nothing is deployed.

## Choices made (the safer option, as `claude-next.md` asks)

1. **Base branch.** `origin/main`, see above.
2. **Step 1 changes code as well as text.** On `origin/main`, logout did delete the test result, so the old privacy bullet was true for the committed code. To make the new bullet true:
   - `AuthService.logout()` now removes only the CV text;
   - a sign-in with a different email removes the test result, its answers, the roadmap ticks and the AI marks.

   The other session's uncommitted `auth.service.ts` does the same for the result key only, so keep one of the two when merging. I changed only the bullet that section 0 names.
3. **First-step threshold.** The box picks the highest share × automation among tasks AI can do at least 40% of (`FIRST_STEP_MIN_AUTOMATION`, already live since #52).
   - For jobs where no task reaches 40% (for example cooks), the first roadmap item is shown instead, so nobody is told to cook with AI.
   - Section 2 does not mention a threshold. To follow it literally, set the constant to 0.
   - The "all marked" sentence appears only when every task at 40% or more is marked.
4. **Additions not asked for:**
   - A "Reset to the typical week" link. The rescaling is proportional, so dragging can push small shares to 0, and the link is the way back.
   - The set of visible tasks is fixed when the result opens, so a row does not disappear while its share is dragged down.
5. **Older saves.** A saved result without stored answers previews slider moves locally and saves that preview. The server is not asked, because the adjustment needs the answers.

## Open points for you

- **Privacy page.** "Cookies and browser storage" still lists only the last test result. The browser now also keeps the answers, task shares, roadmap ticks and AI marks. Section 0 said to change only one bullet, so this paragraph is left as it was.
- **The other session's uncommitted work.** It should be committed on its own branch before or after this one; expect conflicts in the three files named above.
- **Not started**, as section 3 asks: the team report, sharing, or anything else.
