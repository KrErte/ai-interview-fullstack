"""Stage 3: write backend/src/main/resources/ai-readiness/professions-onet.json from the stage 1-2 outputs.

Each O*NET occupation becomes a Profession in the same shape as the hand-written professions.json, plus
onetCode, alternateTitles (search only) and tasks[].observedUse. Occupations already covered by a
hand-written profession (its onetCodes) are left out.
Run: python tools/onet-import/catalog.py
"""
import csv
import json
from pathlib import Path

from build import HERE, OUT

RESOURCES = HERE.parent.parent / 'backend' / 'src' / 'main' / 'resources' / 'ai-readiness'
TEMPLATES = HERE / 'templates.json'
PROFESSIONS = RESOURCES / 'professions.json'
TARGET = RESOURCES / 'professions-onet.json'


def _rows(path):
    with Path(path).open(encoding='utf-8', newline='') as f:
        return list(csv.DictReader(f))


def build_catalog(out_dir, templates, hidden_codes):
    """Professions for every O*NET occupation not in hidden_codes, sorted by name."""
    out_dir = Path(out_dir)
    occupations = _rows(out_dir / 'onet_occupations.csv')
    titles = {o['onetsoc_code']: o['title'] for o in occupations}
    automation = {a['task_id']: a for a in _rows(out_dir / 'onet_automation.csv')}
    tasks, alternates = {}, {}
    for t in _rows(out_dir / 'onet_tasks.csv'):
        tasks.setdefault(t['onetsoc_code'], []).append(t)
    for a in _rows(out_dir / 'onet_alternate_titles.csv'):
        alternates.setdefault(a['onetsoc_code'], []).append(a['alternate_title'])

    catalog = []
    for occ in occupations:
        code = occ['onetsoc_code']
        if code in hidden_codes:
            continue
        task_list = [{'name': t['task_statement'], 'share': int(t['share_pct']),
                      'automation': round(float(automation[t['task_id']]['automation']), 2),
                      'observedUse': automation[t['task_id']]['observed_use'] == 'true'}
                     for t in tasks[code]]
        edge = sorted(tasks[code], key=lambda t: -int(t['share_pct']) * (1 - float(automation[t['task_id']]['automation'])))
        template = templates[occ['soc_major']]
        catalog.append({
            'id': 'onet-' + code.replace('.', '-'),
            'name': occ['title'],
            'category': occ['soc_major_title'],
            'onetCode': code,
            'tasks': task_list,
            'humanEdge': [t['task_statement'] for t in edge[:3]],
            'aiTools': template['aiTools'],
            'pivotRoles': [titles[c] for c in occ['related_codes'].split(';') if c in titles][:3],
            'roadmap': template['roadmap'],
            'alternateTitles': alternates.get(code, []),
        })
    return sorted(catalog, key=lambda p: p['name'])


def hidden_codes_from(professions_json):
    return {c for p in json.loads(Path(professions_json).read_text(encoding='utf-8')) for c in p.get('onetCodes', [])}


if __name__ == '__main__':
    templates = json.loads(TEMPLATES.read_text(encoding='utf-8'))
    catalog = build_catalog(OUT, templates, hidden_codes_from(PROFESSIONS))
    TARGET.write_text(json.dumps(catalog, ensure_ascii=False, separators=(',', ':')) + '\n', encoding='utf-8')
    print(f'{len(catalog)} professions -> {TARGET} ({TARGET.stat().st_size:,} bytes)')
