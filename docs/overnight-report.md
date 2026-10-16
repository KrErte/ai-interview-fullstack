# Report – feature/your-week, feature/team-report, feature/share-and-measure

All three branches are local only.

- **Part 1** covers steps 1–4 from `docs/claude-next.md`.
- **Part 2** covers steps 5–10: the earning model and `docs/superpowers/specs/2026-10-14-team-mode-design.md`.

# Part 1 – feature/your-week (steps 1–4)

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

- **Privacy page.** "Cookies and browser storage" listed only the last test result. Fixed in step 6: the list now names everything the browser keeps.
- **The other session's uncommitted work.** It should be committed on its own branch before or after this one; expect conflicts in the three files named above.
- **Team report and sharing.** These were left out of part 1, as section 3 asks. They are done in part 2.

# Part 2 – feature/team-report and feature/share-and-measure (steps 5–10)

Nothing is pushed or merged. `overnight/rbai-gap`, `frontend/`, `old-ui/` and `frontend/uus/` are not touched. There is no payment or Stripe code. The personal test stays free and needs no account.

## Branches and commits

The branches are stacked: `feature/your-week` → `feature/team-report` → `feature/share-and-measure`. Merging the last one brings in all three.

| # | Branch | Commit | Step |
|---|--------|--------|------|
| 5a | `feature/team-report` | `f5a3020` Add the anonymous team report backend | 5 |
| 5b | `feature/team-report` | `36887d1` Add the team report pages and invite flow | 5 |
| 6 | `feature/team-report` | `eb253a6` Add the team report to the privacy policy and a DPA template | 6 |
| 7 | `feature/share-and-measure` | `35d594f` Add a shareable result link without an account | 7 |
| 8 | `feature/share-and-measure` | `f29eb34` Add FAQPage and BreadcrumbList JSON-LD to job pages | 8 |
| 9 | `feature/share-and-measure` | `671b517` Track search gaps, printing, the AI comment, sliders and team steps | 9 |
| 10 | `feature/share-and-measure` | this update | 10 |

Step 5 is two commits, backend first and then the pages, so each diff can be reviewed on its own.

## What was built

### Step 5 – team report

- **Database.** A new Liquibase changeset, `2026-10-14-create-team-tables.xml`, adds two tables:
  - `team`: invite code, SHA-256 of the admin token, name, owner email and `max_members` (default 50);
  - `team_submission`: profession, scores, band, answers, tasks and `submitted_on` (a date with no time). It stores no name, email, IP address or browser ID.
- **API** (`/api/teams`, public):
  - `POST /api/teams` creates a team and returns the invite link and the secret report link.
  - `GET /join/{code}` returns the team name for the invite banner.
  - `GET` and `DELETE /report/{token}` read and delete the report. A wrong token gives 404.
- **Storing answers.** `POST /api/ai-readiness/assess` takes an optional `teamCode` and `teamEditToken`.
  - The answer is stored after scoring.
  - The AI comment gets no team information.
  - A slider move or a retake from the same browser updates the same anonymous answer and keeps the first date.
  - If the team is full, the response says `"full"` and the person still gets their result.
- **Report.** It opens only at 5 answers; below 5 the page shows only "N of 5 answers".
  - Contents: bands, the top 10 tasks, AI use, readiness levels, roadmap stages and a rule-based focus.
  - A task is counted only if at least 5 respondents have it.
  - The profession breakdown is dropped when the remaining group has 1–4 people or no profession reaches 5. This stops anyone finding a small group by subtraction.
- **Pages:**
  - `/teams`: Article 4 framing, steps, anonymity rules, a sample report, prices, the create form and "Book a call";
  - `/teams/report#token`: the report, print, and delete with an inline confirmation;
  - an invite banner on `/ai-test`;
  - a "For teams" link in the footer.
- **Prices on the page.**
  - Pilot: 490 € for up to 50 people.
  - Team: 1 490 € for up to 200 people.
  - Annual: 6 € per person, shown as "coming later".
  - There is no checkout. "Book a call" opens an email.

### Step 6 – privacy and DPA

- **`privacy.page.ts`**, dated 16 October 2026, now has:
  - a new "Team reports" section: we are the processor for the answers and the controller for the creator's contact data, plus retention and deletion;
  - the share link;
  - the full list of what the browser stores;
  - the analytics event list.
- **`docs/legal/dpa-template.md`**: a GDPR Article 28 template.
  - It has sections 1–12, Annex 1 (technical and organisational measures) and Annex 2 (Contabo GmbH).
  - **It is a template, not legal advice.** The fields in brackets must be filled in.

### Step 7 – share link

- **The link.** "Share your result" on the result page copies `/ai-test#share=p:<profession>;a:<question>.<option>,…;s:<shares>`.
- **What it carries.** Only IDs and numbers, never free text. The part after `#` (the fragment) is never sent to the server, so it stays out of logs and analytics.
- **Opening it.** The page runs the normal assessment again (`personalize: false`) and shows a banner.
- **Nothing is saved**, on the server or in the viewer's browser. The viewer's own saved result stays as it was.
- **Not shared:** the AI comment.

### Step 8 – JSON-LD

- **What is added.** Every `/jobs/{slug}` page has one `<script type="application/ld+json">` with:
  - a `FAQPage` ("Will AI replace {occupation}?");
  - a `BreadcrumbList` (Jobs → occupation).
- **The FAQ answer** reuses sentences already visible on the page, so the structured data never claims more than the page does.
- **Escaping.** `<`, `>` and `&` are escaped, so an occupation name cannot close the script tag.

### Step 9 – analytics (Umami)

| Event | When | Data |
|---|---|---|
| `search-empty` | a search finds nothing (once per query) | typed text up to 60 characters, category |
| `print` | print button | `page`: result or team-report |
| `ai-comment-toggle` | AI comment checkbox | on/off |
| `shares-first-move` | first slider move on a result | none |
| `team-created` | team created | none |
| `team-joined` | invite link opened | `full` |
| `team-submitted` | answer saved to a team | `status`, `band` |
| `team-report-viewed` | report page opened | `ready` |

No token, invite code or answer is sent.

## Tests run (on `671b517`)

- **Backend** `./gradlew test`: 409 tests, 0 failures. The new or changed test classes:
  - `TeamServiceIntegrationTest` (14 tests, H2 with Liquibase) covers:
    - the thresholds;
    - hiding small task and profession groups;
    - no single answers in the report;
    - the edit token;
    - a full team;
    - deletion.
  - `TeamControllerWebMvcTest` (5).
  - `AiReadinessAssessWebMvcTest` (3) and `AiReadinessScorerTest` (12).
  - `JobPagesTest` (13). The 3 new tests cover the FAQ answer, the JSON-LD structure and the escaping.
  - `JobPageWebMvcTest` (7).
- **Frontend** `ng test`: 120 of 120 pass. The new specs are `share-link.spec.ts` and `team.service.spec.ts`.
- **Production build** `ng build`: passes, 1.56 MB initial.

## Checked in a browser (local backend with in-memory H2, `ng serve`)

- **Team flow:**
  - created a team and copied both links;
  - opened the invite: banner, test, "added" notice;
  - a slider move updated the same answer and did not add a row;
  - the report page showed "1 of 5", then the full report at 5;
  - after the 6th answer, the profession breakdown stayed hidden;
  - the report JSON had no single answers;
  - checked the print view;
  - deleted the team; after that, both the report link and the invite link give 404;
  - a broken invite link shows a message and the test still works.
- **Share link:**
  - In a fresh browser it reproduced the score of 44, showed the banner and saved nothing.
  - Moving from one share link to another on the same page loads the new shared result.
- **Analytics.** Every event above fired once with the data listed, except `ai-comment-toggle` (see below).

## Not checked

- `ai-comment-toggle`: the checkbox appears only when a language model key is set, so this was checked in the code only.
- Google's Rich Results Test on the JSON-LD. Only the structure is unit-tested.
- Keyboard-only use and screen readers on the new pages.
- The `max_members` lock under real PostgreSQL load. It is tested on H2 only.
- A real sale: an invoice, an email to the customer, a signed DPA.
- Production. Nothing is deployed.

## Choices made

1. **The report token is in the URL fragment** (`/teams/report#token`) instead of the path the spec shows. The fragment does not reach server logs, proxies or analytics.
2. **Edit token.** Slider moves and retakes from the same browser update the same anonymous answer and keep the first date. One person cannot fill the team alone by trying again.
3. **Small groups.** A task needs 5 respondents. The profession breakdown is dropped when the remaining group has 1–4 people or no profession reaches 5. The spec says "minimum 5"; this is the stricter reading.
4. **Row lock.** `SELECT … FOR UPDATE` from JPA failed on H2 in Postgres mode, so the lock is an `UPDATE` on the team row that changes nothing.
5. **Team size.** New teams get 50. Raising a team to 200 after a Team purchase is a manual database change.
6. **No rate limit on `POST /api/teams`.** Creating a team makes no LLM call. Add a limit if spam shows up.
7. **Prices** are shown without a VAT statement.
8. **The share link** carries only IDs and shares. The AI comment is left out because it is free text from the language model.
9. **`search-empty`** sends the typed text (up to 60 characters), which shows which jobs are missing.
   - People could type something personal there.
   - If that is a concern, send only the category.

## Open points for you

- **Have a lawyer read the DPA** before you sign it with a customer, and fill in the fields in brackets (address, notice times, court).
- **Decide how to sell.** There is no checkout, so a pilot means an email, an invoice and a manual `max_members` change.
- **Merge order.** Merge `feature/share-and-measure`; it contains the other two branches.
  - The main checkout's uncommitted work in `AiReadinessService.java`, `auth.service.ts` and `privacy.page.ts` will conflict.
  - `AiReadinessService` now also has the constructor that takes `TeamService`.
- **After deploying:**
  - run Google's Rich Results Test on one `/jobs/...` page;
  - check in Umami that the new events arrive.
