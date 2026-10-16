"""Stage 3: write backend/src/main/resources/ai-readiness/professions-onet.json from the stage 1-2 outputs.

Each O*NET occupation becomes a Profession in the same shape as the hand-written professions.json, plus
onetCode, alternateTitles (search only) and tasks[].observedUse. Occupations already covered by a
hand-written profession (its onetCodes) are left out.
Also writes professions-aliases.json: for each hand-written profession, the titles of the O*NET
occupations it hides, so search still finds it by them, and professions-hidden.json: each hidden
occupation's title and the hand-written profession whose page its /jobs URL redirects to.
Run: python tools/onet-import/catalog.py
"""
import csv
import json
from pathlib import Path

from build import HERE, OUT
from roadmap_quality import quality_problems

RESOURCES = HERE.parent.parent / 'backend' / 'src' / 'main' / 'resources' / 'ai-readiness'
TEMPLATES = HERE / 'templates.json'
PROFESSIONS = RESOURCES / 'professions.json'
TARGET = RESOURCES / 'professions-onet.json'
ALIASES = RESOURCES / 'professions-aliases.json'
HIDDEN = RESOURCES / 'professions-hidden.json'


def _rows(path):
    with Path(path).open(encoding='utf-8', newline='') as f:
        return list(csv.DictReader(f))


def build_catalog(out_dir, templates, hidden_codes, projects=None, roadmaps=None):
    """Professions for every O*NET occupation not in hidden_codes, sorted by name.

    projects: {code: [phase 1, 2, 3 sentence]} from write_projects.py; replaces the template's
    project per phase for that occupation (spec O2). Others keep the template's project.
    roadmaps: {code: {'roadmap', 'aiTools', 'humanEdge'}} from write_roadmaps.py (spec O3); when present
    for an occupation they replace the template, the O2 projects and the task-statement humanEdge.
    Roadmaps that fail the quality gate (roadmap_quality.py) are ignored, so a thin one never replaces
    the template.
    """
    projects = projects or {}
    roadmaps = {code: r for code, r in (roadmaps or {}).items() if not quality_problems(r)}
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
        written = roadmaps.get(code)
        catalog.append({
            'id': 'onet-' + code.replace('.', '-'),
            'name': occ['title'],
            'category': occ['soc_major_title'],
            'onetCode': code,
            'tasks': task_list,
            'humanEdge': written['humanEdge'] if written else [t['task_statement'] for t in edge[:3]],
            'aiTools': written['aiTools'] if written else template['aiTools'],
            'pivotRoles': [titles[c] for c in occ['related_codes'].split(';') if c in titles][:3],
            'roadmap': written['roadmap'] if written else
            [{**phase, 'project': projects[code][i]} if code in projects else phase
             for i, phase in enumerate(template['roadmap'])],
            'alternateTitles': alternates.get(code, []),
        })
    return sorted(catalog, key=lambda p: p['name'])


def build_aliases(out_dir, curated_codes):
    """{hand-written id: titles of the O*NET occupations it hides}, so search still finds them.

    Per code: the O*NET title, then its alternate titles; duplicates dropped, order kept.
    Professions without onetCodes are left out.
    """
    out_dir = Path(out_dir)
    titles = {o['onetsoc_code']: o['title'] for o in _rows(out_dir / 'onet_occupations.csv')}
    alternates = {}
    for a in _rows(out_dir / 'onet_alternate_titles.csv'):
        alternates.setdefault(a['onetsoc_code'], []).append(a['alternate_title'])
    aliases = {}
    for pid, codes in curated_codes.items():
        names = []
        for code in codes:
            for name in [titles[code]] + alternates.get(code, []):
                if name not in names:
                    names.append(name)
        if names:
            aliases[pid] = names
    return aliases


def build_hidden(out_dir, curated_codes):
    """{O*NET title of a hidden occupation: hand-written id that hides it}, sorted by title.

    The backend redirects /jobs/<slug of that title> to the hand-written profession's page.
    """
    titles = {o['onetsoc_code']: o['title'] for o in _rows(Path(out_dir) / 'onet_occupations.csv')}
    hidden = {titles[code]: pid for pid, codes in curated_codes.items() for code in codes}
    return dict(sorted(hidden.items()))


def curated_codes_from(professions_json):
    return {p['id']: p.get('onetCodes', []) for p in json.loads(Path(professions_json).read_text(encoding='utf-8'))}


def hidden_codes_from(professions_json):
    return {c for codes in curated_codes_from(professions_json).values() for c in codes}


def _write_json(path, data):
    path.write_text(json.dumps(data, ensure_ascii=False, separators=(',', ':')) + '\n', encoding='utf-8')
    print(f'{path.name}: {len(data)} entries ({path.stat().st_size:,} bytes)')


def projects_from(path):
    """{code: [phase 1, 2, 3 project]} from write_projects.py output; empty when it does not exist."""
    projects = {}
    if Path(path).exists():
        for r in _rows(path):
            projects.setdefault(r['onetsoc_code'], ['', '', ''])[int(r['phase']) - 1] = r['project']
    return {code: p for code, p in projects.items() if all(p)}


if __name__ == '__main__':
    templates = json.loads(TEMPLATES.read_text(encoding='utf-8'))
    projects = projects_from(OUT / 'projects_llm.csv')
    roadmaps_path = OUT / 'roadmaps_llm.json'
    roadmaps = json.loads(roadmaps_path.read_text(encoding='utf-8')) if roadmaps_path.exists() else {}
    thin = sum(1 for r in roadmaps.values() if quality_problems(r))
    print(f'occupation-specific projects: {len(projects)}, whole roadmaps: {len(roadmaps) - thin}'
          f' ({thin} below the quality gate, ignored)')
    _write_json(TARGET, build_catalog(OUT, templates, hidden_codes_from(PROFESSIONS), projects, roadmaps))
    _write_json(ALIASES, build_aliases(OUT, curated_codes_from(PROFESSIONS)))
    _write_json(HIDDEN, build_hidden(OUT, curated_codes_from(PROFESSIONS)))
