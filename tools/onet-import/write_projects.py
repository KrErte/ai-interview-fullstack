"""Spec O2: one occupation-specific practice project per roadmap phase, written by Claude (Batches API).

The phases (title + items) come from the occupation's SOC group template; Claude only writes the
one-sentence `project` for each phase, from the occupation's largest tasks. catalog.py prefers
these sentences over the template's project. Only occupations that are not hidden behind a
hand-written profession are written.

    python tools/onet-import/write_projects.py submit --pilot   # 10 occupations -> .cache/projects-pilot/
    python tools/onet-import/write_projects.py collect --pilot
    python tools/onet-import/write_projects.py submit           # all visible occupations -> out/
    python tools/onet-import/write_projects.py collect          # writes out/projects_llm.csv + projects_usage.json
    python tools/onet-import/write_projects.py retry            # re-writes occupations missing from the CSV
"""
import csv
import json
import sys
import time

from build import CACHE, HERE, OUT
from catalog import PROFESSIONS, TEMPLATES, hidden_codes_from
from rate_tasks import EFFORT, MODEL, batch_cost

MAX_PROJECT_CHARS = 220
TOP_TASKS = 5

GUIDE = """You write practice projects for an AI learning plan. The plan has three phases for one occupation.
For each phase, write ONE project the worker can do alone in 1-4 weeks with today's AI tools.

Rules:
- One sentence, at most 200 characters, imperative mood ("Automate...", "Build...", "Use AI to...").
- Specific to this occupation's real tasks (given below), not generic office work.
- It must practise the phase's focus (given as title and items).
- Measurable where possible: a number, a time saved, a before/after comparison.
- Realistic and legal for the occupation; no patient, client or personal data in public AI tools.
- No product names unless they appear in the phase items.

Examples of the style (other occupations):
- Take an old project of yours and have an AI agent fix 5 bugs or add 1 feature – time it.
- Automate one client's month-end close and measure the hours saved.
- Prepare a full unit of lessons with AI and note the hours saved."""

SCHEMA = {
    'type': 'object',
    'properties': {
        'projects': {
            'type': 'array',
            'items': {
                'type': 'object',
                'properties': {'phase': {'type': 'integer'}, 'project': {'type': 'string'}},
                'required': ['phase', 'project'],
                'additionalProperties': False,
            },
        },
    },
    'required': ['projects'],
    'additionalProperties': False,
}


def build_params(title, top_tasks, phases, guide):
    lines = [f'Occupation: {title}', '', 'Largest tasks:'] + [f'- {t}' for t in top_tasks] + ['', 'Phases:']
    lines += [f"Phase {i}: {ph['title']} ({'; '.join(ph['items'])})" for i, ph in enumerate(phases, start=1)]
    return {
        'model': MODEL,
        'max_tokens': 16000,
        'system': [{'type': 'text', 'text': guide, 'cache_control': {'type': 'ephemeral'}}],
        'output_config': {'effort': EFFORT, 'format': {'type': 'json_schema', 'schema': SCHEMA}},
        'messages': [{'role': 'user', 'content': '\n'.join(lines)}],
    }


def parse_projects(text):
    """[phase 1, phase 2, phase 3] sentences; raises ValueError unless each phase appears once with valid length."""
    items = json.loads(text)['projects']
    by_phase = {}
    for item in items:
        by_phase.setdefault(int(item['phase']), []).append(str(item['project']).strip())
    if sorted(by_phase) != [1, 2, 3] or any(len(v) != 1 for v in by_phase.values()):
        raise ValueError(f'expected phases 1, 2, 3 once each, got {sorted(by_phase)}')
    projects = [by_phase[i][0] for i in (1, 2, 3)]
    for p in projects:
        if not p or len(p) > MAX_PROJECT_CHARS:
            raise ValueError(f'project length {len(p)} outside 1..{MAX_PROJECT_CHARS}')
    return projects


def _rows(path):
    with path.open(encoding='utf-8', newline='') as f:
        return list(csv.DictReader(f))


def _occupations(pilot):
    """[(code, title, top task statements, phases)] for every occupation that is not hidden."""
    hidden = hidden_codes_from(PROFESSIONS)
    templates = json.loads(TEMPLATES.read_text(encoding='utf-8'))
    tasks = {}
    for t in _rows(OUT / 'onet_tasks.csv'):
        tasks.setdefault(t['onetsoc_code'], []).append(t)
    result = []
    for occ in _rows(OUT / 'onet_occupations.csv'):
        code = occ['onetsoc_code']
        if code in hidden:
            continue
        top = sorted(tasks[code], key=lambda t: -int(t['share_pct']))[:TOP_TASKS]
        result.append((code, occ['title'], [t['task_statement'] for t in top], templates[occ['soc_major']]['roadmap']))
    if pilot:
        # one occupation from each of the first 10 SOC groups
        seen, picked = set(), []
        for item in result:
            if item[0][:2] not in seen and len(picked) < 10:
                seen.add(item[0][:2])
                picked.append(item)
        result = picked
    return result


def _workdir(pilot):
    path = CACHE / 'projects-pilot' if pilot else OUT
    path.mkdir(parents=True, exist_ok=True)
    return path


def _usage_tokens(message):
    u = message.usage
    return (u.input_tokens + (u.cache_read_input_tokens or 0) + (u.cache_creation_input_tokens or 0),
            u.output_tokens)


def _write(workdir, projects, usage):
    with (workdir / 'projects_llm.csv').open('w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f, lineterminator='\n')
        writer.writerow(['onetsoc_code', 'phase', 'project'])
        for code in sorted(projects):
            for phase, text in enumerate(projects[code], start=1):
                writer.writerow([code, phase, text])
    usage['occupations_written'] = len(projects)
    usage['cost_usd'] = round(batch_cost(usage['input_tokens'], usage['output_tokens']), 2)
    (workdir / 'projects_usage.json').write_text(json.dumps(usage, indent=2), encoding='utf-8')


def _read(workdir):
    path = workdir / 'projects_llm.csv'
    projects = {}
    if path.exists():
        for r in _rows(path):
            projects.setdefault(r['onetsoc_code'], ['', '', ''])[int(r['phase']) - 1] = r['project']
    return projects


def submit(pilot):
    import anthropic
    from anthropic.types.message_create_params import MessageCreateParamsNonStreaming
    from anthropic.types.messages.batch_create_params import Request

    requests = [Request(custom_id=code.replace('.', '_'),
                        params=MessageCreateParamsNonStreaming(**build_params(title, top, phases, GUIDE)))
                for code, title, top, phases in _occupations(pilot)]
    batch = anthropic.Anthropic().messages.batches.create(requests=requests)
    state = _workdir(pilot) / 'projects_batch.json'
    state.write_text(json.dumps({'batch_id': batch.id, 'model': MODEL, 'effort': EFFORT,
                                 'occupations': len(requests)}, indent=2), encoding='utf-8')
    print(f'submitted batch {batch.id} with {len(requests)} occupations ({state})')


def collect(pilot):
    import anthropic

    workdir = _workdir(pilot)
    batch_id = json.loads((workdir / 'projects_batch.json').read_text(encoding='utf-8'))['batch_id']
    client = anthropic.Anthropic()
    while True:
        batch = client.messages.batches.retrieve(batch_id)
        if batch.processing_status == 'ended':
            break
        print(f'{batch.processing_status}: {batch.request_counts.processing} processing', flush=True)
        time.sleep(60)
    projects, failures = {}, []
    usage = {'batch_id': batch_id, 'model': MODEL, 'effort': EFFORT, 'input_tokens': 0, 'output_tokens': 0}
    for result in client.messages.batches.results(batch_id):
        code = result.custom_id.replace('_', '.')
        if result.result.type != 'succeeded':
            failures.append((code, result.result.type))
            continue
        message = result.result.message
        tin, tout = _usage_tokens(message)
        usage['input_tokens'] += tin
        usage['output_tokens'] += tout
        text = next((b.text for b in message.content if b.type == 'text'), '')
        try:
            projects[code] = parse_projects(text)
        except (ValueError, KeyError, json.JSONDecodeError) as e:
            failures.append((code, str(e)[:160]))
    usage['failed'] = failures
    _write(workdir, projects, usage)
    print(json.dumps({k: v for k, v in usage.items() if k != 'failed'}, indent=2))
    print(f'failed: {len(failures)}')
    for code, reason in failures:
        print(f'  {code}: {reason}')


def retry(pilot, attempts=3):
    """Synchronously re-write occupations missing from projects_llm.csv (list price = 2x batch)."""
    import anthropic

    workdir = _workdir(pilot)
    projects = _read(workdir)
    usage_path = workdir / 'projects_usage.json'
    usage = json.loads(usage_path.read_text(encoding='utf-8'))
    client = anthropic.Anthropic()
    for code, title, top, phases in _occupations(pilot):
        if code in projects and all(projects[code]):
            continue
        for attempt in range(1, attempts + 1):
            message = client.messages.create(**build_params(title, top, phases, GUIDE))
            tin, tout = _usage_tokens(message)
            usage['input_tokens'] += 2 * tin
            usage['output_tokens'] += 2 * tout
            text = next((b.text for b in message.content if b.type == 'text'), '')
            try:
                projects[code] = parse_projects(text)
                print(f'{code}: written (attempt {attempt})')
                break
            except (ValueError, KeyError, json.JSONDecodeError) as e:
                print(f'{code}: attempt {attempt} rejected: {str(e)[:120]}')
    usage['retried'] = [c for c, _ in usage.get('failed', [])]
    usage['failed'] = []
    _write(workdir, projects, usage)
    print(f"written: {usage['occupations_written']}, total cost: ${usage['cost_usd']}")


if __name__ == '__main__':
    commands = {'submit': submit, 'collect': collect, 'retry': retry}
    if len(sys.argv) < 2 or sys.argv[1] not in commands:
        sys.exit(__doc__)
    commands[sys.argv[1]]('--pilot' in sys.argv)
