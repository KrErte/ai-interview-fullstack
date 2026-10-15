# O\*NET import

Builds occupation and task data for the AI replaceability test from O\*NET and the Anthropic Economic Index.
Design: `docs/superpowers/specs/2026-10-13-onet-professions-design.md`. This is stage 1: occupations, tasks and time shares. Automation scores come in stage 2.

## Run

```bash
python tools/onet-import/fetch.py   # downloads pinned inputs into .cache/ (gitignored), ~115 MB
python tools/onet-import/build.py   # writes out/*.csv and prints counts
python -m unittest discover -s tools/onet-import -p "test_*.py"
```

Expected output of `build.py` with the pinned inputs:

```
occupations: 923
tasks: 18796
penetration_unmatched: 96
```

Only the standard library is needed (Python 3.10+). The `out/` files are committed so changes show up as diffs in review.

## Inputs (pinned in `fetch.py`)

| File | Source | License |
|---|---|---|
| `db_29_3_text.zip` | [O\*NET 29.3 database](https://www.onetcenter.org/database.html): Occupation Data, Task Statements, Alternate Titles, Related Occupations | CC BY 4.0, U.S. Department of Labor / ETA |
| `robot_exposure_tasks.csv` | [Anthropic Economic Index](https://huggingface.co/datasets/Anthropic/EconomicIndex) `robot_exposure/` at revision `4e5f69f…`: per-task `time_share`, `physical` | CC BY |
| `task_penetration.csv` | Same, `labor_market_impacts/`: observed AI use per task (`penetration`) | CC BY |

O\*NET 29.3 is used, not the latest release, because the AEI task data is based on 29.3 and the task IDs and texts must match.

## Outputs (`out/`)

**`onet_occupations.csv`**: one row per O\*NET occupation that has task data (923 of the 1016 titles; the rest are "All Other" groups without tasks).

| Column | Meaning |
|---|---|
| `onetsoc_code` | e.g. `15-1252.00` |
| `title` | O\*NET title |
| `soc_major`, `soc_major_title` | SOC 2018 major group, e.g. `15`, `Computer and Mathematical` |
| `task_count` | number of tasks |
| `related_codes` | the "Primary-Short" related occupations in O\*NET rank order, `;`-separated |

**`onet_tasks.csv`**: one row per task.

| Column | Meaning |
|---|---|
| `onetsoc_code`, `task_id`, `task_statement`, `task_type` | from O\*NET Task Statements (`task_type`: Core / Supplemental) |
| `time_share` | AEI share of working time, 0..1, sums to 1 per occupation |
| `share_pct` | `time_share` as whole percent, exactly 100 per occupation (largest remainder). Can be 0 for very small tasks (560 tasks). |
| `physical` | AEI `Yes` / `No` |
| `penetration` | AEI observed AI use, 0..1; empty when the task text has no match (96 tasks) |

**`onet_alternate_titles.csv`**: `onetsoc_code`, `alternate_title`, `short_title` (empty when O\*NET has `n/a`). Used for search ("coder" → Software Developers).

## Stage 2: automation scores

Section 5 / O1 of the design doc. Every task gets `automation` (0..1) from three sources:

| Source | File | Role |
|---|---|---|
| A: Claude rating | `out/ratings_llm.csv` from `rate_tasks.py` (`claude-opus-5`, effort `medium`, rubric `rubric.md`, Message Batches API) | Base score |
| B: GPTs-are-GPTs human label | `.cache/full_labelset.tsv`, column `human_exposure_agg` (E0/E2/E1 → 0.15/0.55/0.80) | 50/50 anchor where present (99.8% of tasks) |
| C: AEI `penetration` | `out/onet_tasks.csv` | `observed_use = penetration > 0`, and the review check. Not part of the score. |

`blend.py` mixes A and B, then compares the resulting exposure with the 19 hand-written professions (their `onetCodes` in `professions.json`). If the mean absolute difference is over 10 points, it applies one linear correction to all tasks.

```bash
pip install -r tools/onet-import/requirements.txt
# ANTHROPIC_API_KEY must be set in the environment (never commit it)
python tools/onet-import/rate_tasks.py submit --pilot    # 24 calibration occupations
python tools/onet-import/rate_tasks.py collect --pilot   # writes .cache/pilot/ratings_llm.csv + usage.json
python tools/onet-import/blend.py --preview              # B (+ pilot A), fills gaps, writes .cache/preview/
python tools/onet-import/rate_tasks.py submit            # all 923 occupations
python tools/onet-import/rate_tasks.py collect           # writes out/ratings_llm.csv + out/usage.json
python tools/onet-import/blend.py                        # writes out/onet_automation.csv, review.csv, calibration.md
```

Outputs:
- `onet_automation.csv`: `onetsoc_code, task_id, llm_score, b_label, blended, automation, observed_use`
- `review.csv`: tasks with observed AI use but `automation < 0.3`, for manual review
- `calibration.md`: hand-written vs O\*NET exposure per profession, and the correction applied

Additional source: [GPTs-are-GPTs](https://github.com/openai/GPTs-are-GPTs) `data/full_labelset.tsv` at commit `0471612…`, MIT license. Cite as Eloundou, Manning, Mishkin, Rock (2024), *Science* 384(6702).

## Stage 3: catalog for the backend

`catalog.py` writes `backend/src/main/resources/ai-readiness/professions-onet.json`: one profession per O\*NET occupation (899), in the same shape as the hand-written `professions.json`, plus `onetCode`, `alternateTitles` (search only) and `tasks[].observedUse`.

```bash
python tools/onet-import/catalog.py
```

- **Hidden duplicates:** occupations listed in a hand-written profession's `onetCodes` are left out (24 codes), so "Software developer" exists only once.
- **`humanEdge`:** the 3 tasks with the largest `share × (1 − automation)`.
- **`pivotRoles`:** the first 3 O\*NET "Primary-Short" related occupations.
- **`aiTools` and `roadmap`:** from `templates.json`, one template per SOC major group (22). Each phase's `project` is replaced by the occupation-specific sentence from `out/projects_llm.csv` (see below).

## Occupation-specific projects (spec O2)

`write_projects.py` asks Claude for one practice project per roadmap phase, based on the occupation's 5 largest tasks and the group template's phase. Each answer must have exactly one sentence per phase, 1–220 characters, or it is rejected.

```bash
python tools/onet-import/write_projects.py submit && python tools/onet-import/write_projects.py collect   # Batches API, half price, up to 24 h
python tools/onet-import/write_projects.py direct --model claude-sonnet-5 --max-usd 4                    # parallel, list price, resumable, stops at the budget
python tools/onet-import/catalog.py                                                                       # picks up out/projects_llm.csv
```

**What was run on 2026-10-13:**
- An Opus 5 batch sat for about 95 minutes with nothing processed and was then cancelled. While it was cancelling, 898 of 899 requests still completed and were billed (about $3.6).
- A direct run with Sonnet 5 wrote all 899 occupations in 6 minutes for $3.49.
- `out/projects_llm.csv` combines them: Opus 5 for 853 occupations, and Sonnet 5 for the 46 whose Opus answer was over the 220-character limit or cancelled. The `model` column records which one wrote each sentence.

Lesson: cancelling a batch does not stop requests that are already being processed. Check `request_counts` after the cancel completes.

The backend loads both files. `GET /api/ai-readiness/professions` still returns only the hand-written professions. `GET /api/ai-readiness/professions/search?q=&limit=` searches all of them by name and alternate title.

## Whole occupation-specific roadmaps (spec O3)

O2 left the phase titles and items on the 22 group templates, so an actuary got the IT template ("Pair programming with AI") and `humanEdge` was three raw task statements. `write_roadmaps.py` asks Claude for the whole thing from the occupation's 10 largest tasks (share and automation) and its related occupations: three phases (start where AI already helps, make it repeatable, grow what AI cannot do), each with a title, three items and a project, plus three `aiTools` and three `humanEdge` strengths. Answers with the wrong count or an over-long field are rejected and retried.

```bash
python tools/onet-import/write_roadmaps.py direct --pilot --max-usd 4   # the 30 PILOT occupations
python tools/onet-import/catalog.py                                     # picks up out/roadmaps_llm.json
```

The 30 pilot roadmaps (2026-10-15) were written in a Claude Code session (Opus 5.5) from the same prompts and `GUIDE`, and checked with `parse_roadmap`; their `model` field says so. The rest can come from the API run above or the same way.

`catalog.py` uses `out/roadmaps_llm.json` where it has an occupation and falls back to the template plus the O2 project elsewhere. The file is resumable: a later run without `--pilot` writes only the missing occupations.

## Attribution

Products using these files must show:
> Occupation and task data: O\*NET 29.3, U.S. Department of Labor / ETA, CC BY 4.0. Task time shares and observed AI use: Anthropic Economic Index, CC BY.
