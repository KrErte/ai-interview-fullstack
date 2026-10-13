"""Stage 2: combine automation sources per task, calibrate against the hand-written professions, flag for review.

A = LLM rating (ratings_llm.csv, optional), B = GPTs-are-GPTs human label, C = AEI observed use (penetration > 0).
See section 5 / O1 of docs/superpowers/specs/2026-10-13-onet-professions-design.md.
Run: python tools/onet-import/blend.py   (uses out/ratings_llm.csv when it exists, otherwise B only)
"""
import csv
import json
import sys
from pathlib import Path

from build import CACHE, HERE, OUT

# GPTs-are-GPTs exposure labels as automation share (spec 5, O1 / 6)
B_SCORES = {'E0': 0.15, 'E1': 0.80, 'E2': 0.55}
CALIBRATION_THRESHOLD = 10.0
REVIEW_BELOW = 0.3

DEFAULT_LABELS = CACHE / 'full_labelset.tsv'
DEFAULT_PROFESSIONS = HERE.parent.parent / 'backend' / 'src' / 'main' / 'resources' / 'ai-readiness' / 'professions.json'

csv.field_size_limit(2**31 - 1)


def blend_task(llm_score, b_label):
    """50/50 mix when both sources exist, otherwise whichever exists, otherwise None."""
    b = B_SCORES.get(b_label) if b_label else None
    if llm_score is not None and b is not None:
        return 0.5 * llm_score + 0.5 * b
    return llm_score if llm_score is not None else b


def occupation_exposure(tasks):
    """Σ share_pct × automation, on the same 0..100 scale as AiReadinessScorer."""
    return sum(share * automation for share, automation in tasks)


def fit_calibration(pairs):
    """(a, b, mean_abs_diff) mapping O*NET exposure to curated exposure.

    Identity when the mean absolute difference is within CALIBRATION_THRESHOLD, else least squares.
    """
    mad = sum(abs(onet - curated) for onet, curated in pairs) / len(pairs)
    if mad <= CALIBRATION_THRESHOLD:
        return 1.0, 0.0, mad
    n = len(pairs)
    mx = sum(x for x, _ in pairs) / n
    my = sum(y for _, y in pairs) / n
    var = sum((x - mx) ** 2 for x, _ in pairs)
    a = sum((x - mx) * (y - my) for x, y in pairs) / var if var else 1.0
    return a, my - a * mx, mad


def apply_calibration(automation, a, b):
    """Task-level form of exposure' = a·exposure + b (task shares sum to 100), clamped to 0..1."""
    return min(1.0, max(0.0, a * automation + b / 100))


def _read_csv(path, delimiter=','):
    with Path(path).open(encoding='utf-8', newline='') as f:
        return list(csv.DictReader(f, delimiter=delimiter))


def _write_csv(path, header, rows):
    with Path(path).open('w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f, lineterminator='\n')
        writer.writerow(header)
        writer.writerows(rows)


def run(out_dir, ratings_csv, labels_tsv, professions_json, fill_missing=False, tasks_csv=None):
    """Write onet_automation.csv, review.csv and calibration.md into out_dir.

    fill_missing (preview/pilot only): a task with no score gets its occupation's mean instead of raising.
    """
    out_dir = Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    tasks = _read_csv(tasks_csv or out_dir / 'onet_tasks.csv')
    labels = {str(int(float(r['Task ID']))): r['human_exposure_agg'] for r in _read_csv(labels_tsv, '\t')}
    llm = {r['task_id']: float(r['llm_score']) for r in _read_csv(ratings_csv)} if ratings_csv else {}

    blended, missing = {}, []
    for t in tasks:
        value = blend_task(llm.get(t['task_id']), labels.get(t['task_id']))
        if value is None:
            if not fill_missing:
                raise ValueError(f"Task {t['task_id']} has neither an LLM rating nor a human label")
            missing.append(t)
            continue
        blended[t['task_id']] = value
    for t in missing:
        known = [blended[o['task_id']] for o in tasks
                 if o['onetsoc_code'] == t['onetsoc_code'] and o['task_id'] in blended]
        blended[t['task_id']] = sum(known) / len(known) if known else sum(blended.values()) / len(blended)

    by_occ = {}
    for t in tasks:
        by_occ.setdefault(t['onetsoc_code'], []).append((int(t['share_pct']), blended[t['task_id']]))
    exposure = {code: occupation_exposure(items) for code, items in by_occ.items()}

    # hand-written professions carry their O*NET counterparts in onetCodes
    report_rows, pairs = [], []
    for p in json.loads(Path(professions_json).read_text(encoding='utf-8')):
        pid, codes = p['id'], p.get('onetCodes')
        if not codes:
            continue
        onet = sum(exposure[c] for c in codes) / len(codes)
        curated = occupation_exposure((t['share'], t['automation']) for t in p['tasks'])
        pairs.append((onet, curated))
        report_rows.append((pid, codes, curated, onet))
    a, b, mad = fit_calibration(pairs)

    auto_rows, review_rows = [], []
    for t in tasks:
        automation = apply_calibration(blended[t['task_id']], a, b)
        observed = (t['penetration'] or '0') != '0' and float(t['penetration']) > 0
        llm_score = llm.get(t['task_id'])
        auto_rows.append([t['onetsoc_code'], t['task_id'], '' if llm_score is None else llm_score,
                          labels.get(t['task_id'], ''), round(blended[t['task_id']], 4), round(automation, 4),
                          'true' if observed else 'false'])
        if observed and automation < REVIEW_BELOW:
            review_rows.append([t['onetsoc_code'], t['task_id'], t['task_statement'], round(automation, 4),
                                t['penetration']])

    _write_csv(out_dir / 'onet_automation.csv',
               ['onetsoc_code', 'task_id', 'llm_score', 'b_label', 'blended', 'automation', 'observed_use'],
               auto_rows)
    _write_csv(out_dir / 'review.csv',
               ['onetsoc_code', 'task_id', 'task_statement', 'automation', 'penetration'], review_rows)
    _write_report(out_dir / 'calibration.md', report_rows, a, b, mad, bool(llm))
    return {'tasks': len(auto_rows), 'llm_rated': len(llm), 'calibration': (round(a, 4), round(b, 4)),
            'mean_abs_diff': round(mad, 1), 'review': len(review_rows), 'filled': len(missing)}


def _write_report(path, rows, a, b, mad, has_llm):
    lines = ['# Calibration report', '',
             f"Sources: {'A (LLM) + B (human labels)' if has_llm else 'B (human labels) only'}. "
             f'Mean absolute difference before calibration: **{mad:.1f}** points '
             f'(threshold {CALIBRATION_THRESHOLD:.0f}).',
             f'Calibration applied: exposure\' = {a:.3f} × exposure + {b:.2f}', '',
             '| Curated profession | O*NET codes | Curated | O*NET (raw) | O*NET (calibrated) |',
             '|---|---|---|---|---|']
    for pid, codes, curated, onet in rows:
        lines.append(f"| {pid} | {', '.join(codes)} | {curated:.0f} | {onet:.0f} | {a * onet + b:.0f} |")
    Path(path).write_text('\n'.join(lines) + '\n', encoding='utf-8')


if __name__ == '__main__':
    if not DEFAULT_LABELS.exists():
        sys.exit('Missing full_labelset.tsv. Run: python tools/onet-import/fetch.py')
    preview = '--preview' in sys.argv
    ratings = (CACHE / 'pilot' / 'ratings_llm.csv') if preview else (OUT / 'ratings_llm.csv')
    target = CACHE / 'preview' if preview else OUT
    stats = run(target, ratings if ratings.exists() else None, DEFAULT_LABELS, DEFAULT_PROFESSIONS,
                fill_missing=preview, tasks_csv=OUT / 'onet_tasks.csv')
    print(f'written to {target}')
    for key, value in stats.items():
        print(f'{key}: {value}')
