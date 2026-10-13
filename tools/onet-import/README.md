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

## Attribution

Products using these files must show:
> Occupation and task data: O\*NET 29.3, U.S. Department of Labor / ETA, CC BY 4.0. Task time shares and observed AI use: Anthropic Economic Index, CC BY.
