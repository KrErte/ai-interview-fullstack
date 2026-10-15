"""Spec O3: a whole occupation-specific roadmap per occupation, written by Claude.

write_projects.py (O2) replaced only the project sentence. Phase titles and items still came from one of
22 SOC-group templates, so an actuary got the IT template's "Pair programming with AI" and a pharmacist
the same phases as a dental hygienist. Here Claude writes, from the occupation's own tasks and their
automation ratings: three phases (title, three items, project), three AI tools and three human-edge
strengths. catalog.py prefers this over the template and the O2 project.

    python tools/onet-import/write_roadmaps.py direct --pilot --max-usd 4   # the PILOT occupations
    python tools/onet-import/write_roadmaps.py direct --max-usd 80          # every visible occupation

Resumable: occupations already in out/roadmaps_llm.json are skipped. Needs ANTHROPIC_API_KEY.
"""
import json
import sys
import threading
from concurrent.futures import ThreadPoolExecutor, as_completed

from build import OUT
from catalog import PROFESSIONS, hidden_codes_from
from rate_tasks import EFFORT, MODEL
from write_projects import MAX_PROJECT_CHARS, _rows, _usage_tokens, list_cost

TARGET = OUT / 'roadmaps_llm.json'
USAGE = OUT / 'roadmaps_usage.json'
TOP_TASKS = 10
MAX_TITLE_CHARS = 60
MAX_ITEM_CHARS = 160
MAX_TOOL_CHARS = 80
MAX_EDGE_CHARS = 120

# Common occupations that no hand-written profession covers, plus a few the group templates fit badly
# (actuaries got the IT template). Search data was too thin to rank by on 2026-10-15.
PILOT = [
    '15-2011.00',  # Actuaries
    '41-2031.00',  # Retail Salespersons
    '41-2011.00',  # Cashiers
    '43-9061.00',  # Office Clerks, General
    '43-6014.00',  # Secretaries and Administrative Assistants
    '43-6011.00',  # Executive Secretaries and Executive Administrative Assistants
    '43-3031.00',  # Bookkeeping, Accounting, and Auditing Clerks
    '13-2051.00',  # Financial and Investment Analysts
    '13-1111.00',  # Management Analysts
    '27-1024.00',  # Graphic Designers
    '23-2011.00',  # Paralegals and Legal Assistants
    '41-9022.00',  # Real Estate Sales Agents
    '41-9021.00',  # Real Estate Brokers
    '41-3021.00',  # Insurance Sales Agents
    '13-2072.00',  # Loan Officers
    '13-1081.00',  # Logisticians
    '13-1023.00',  # Purchasing Agents
    '17-1011.00',  # Architects
    '17-2051.00',  # Civil Engineers
    '17-2141.00',  # Mechanical Engineers
    '29-1051.00',  # Pharmacists
    '29-1292.00',  # Dental Hygienists
    '21-1021.00',  # Child, Family, and School Social Workers
    '19-3033.00',  # Clinical and Counseling Psychologists
    '27-3023.00',  # News Analysts, Reporters, and Journalists
    '27-3041.00',  # Editors
    '27-4021.00',  # Photographers
    '35-1011.00',  # Chefs and Head Cooks
    '53-3032.00',  # Heavy and Tractor-Trailer Truck Drivers
    '43-4171.00',  # Receptionists and Information Clerks
]

GUIDE = """You write an AI learning plan for one occupation. The reader works in that job, has heard they
should "learn AI", and does not know where to start. The plan must fit their real work, so it is built
from the occupation's tasks: each task has its share of the work week and how much of it AI can do
today (0 = nothing, 1 = all of it).

Write three phases, in this order:
1. Start where AI already helps: the tasks with the largest share x automation. Everyday AI tools on
   those tasks, with the checks this job needs.
2. Make it repeatable: turn what worked into templates, workflows or small automations for this job's
   recurring work, with a human review step where mistakes are costly.
3. Grow what AI cannot do: the tasks with low automation (judgement, people, physical work,
   responsibility). Get better at those, become the person who leads AI use in the team, or move
   toward one of the related occupations given.

For each phase:
- title: at most 60 characters, specific to this occupation (not "Phase 1", not generic office words).
- items: exactly 3, each at most 160 characters. Each names a real task of this occupation and what to
  do with AI or without it. Concrete actions, not topics.
- project: one sentence, at most 200 characters, imperative mood. Something the worker can do alone in
  1-4 weeks with today's tools, measurable where possible (a count, time saved, before/after).

Also write:
- aiTools: exactly 3, each at most 80 characters. Tools or tool types this occupation would really use.
  Prefer well-known general products or plain categories; do not invent product names. No coding tools
  unless the occupation writes code.
- humanEdge: exactly 3, each at most 120 characters. What this worker does that AI cannot take over,
  drawn from the low-automation tasks, written as a short strength ("Calming an upset client and
  finding a fix they accept"), not copied from the task list.

Ground rules: no client, patient or personal data in public AI tools; decisions that need a licence or
signature stay with the person; no salary figures and no predictions that the job will disappear.
Plain English, no hype."""

SCHEMA = {
    'type': 'object',
    'properties': {
        'phases': {
            'type': 'array',
            'items': {
                'type': 'object',
                'properties': {
                    'title': {'type': 'string'},
                    'items': {'type': 'array', 'items': {'type': 'string'}},
                    'project': {'type': 'string'},
                },
                'required': ['title', 'items', 'project'],
                'additionalProperties': False,
            },
        },
        'aiTools': {'type': 'array', 'items': {'type': 'string'}},
        'humanEdge': {'type': 'array', 'items': {'type': 'string'}},
    },
    'required': ['phases', 'aiTools', 'humanEdge'],
    'additionalProperties': False,
}


def build_params(title, tasks, related, model=MODEL):
    """tasks: [(task statement, share %, automation 0..1)], largest share first."""
    lines = [f'Occupation: {title}', '', 'Tasks (share of the work week · how much AI can do today):']
    lines += [f'- {share}% · {automation:.2f} · {name}' for name, share, automation in tasks]
    if related:
        lines += ['', 'Related occupations: ' + '; '.join(related)]
    return {
        'model': model,
        'max_tokens': 16000,
        'system': [{'type': 'text', 'text': GUIDE, 'cache_control': {'type': 'ephemeral'}}],
        'output_config': {'effort': EFFORT, 'format': {'type': 'json_schema', 'schema': SCHEMA}},
        'messages': [{'role': 'user', 'content': '\n'.join(lines)}],
    }


def _texts(values, count, limit, what):
    values = [str(v).strip() for v in values]
    if len(values) != count:
        raise ValueError(f'expected {count} {what}, got {len(values)}')
    for v in values:
        if not v or len(v) > limit:
            raise ValueError(f'{what} length {len(v)} outside 1..{limit}')
    return values


def parse_roadmap(text):
    """{'roadmap': 3 phases, 'aiTools': 3, 'humanEdge': 3}; raises ValueError on a wrong count or length."""
    data = json.loads(text)
    phases = data['phases']
    if len(phases) != 3:
        raise ValueError(f'expected 3 phases, got {len(phases)}')
    roadmap = [{'title': _texts([p['title']], 1, MAX_TITLE_CHARS, 'title')[0],
                'items': _texts(p['items'], 3, MAX_ITEM_CHARS, 'items'),
                'project': _texts([p['project']], 1, MAX_PROJECT_CHARS, 'project')[0]}
               for p in phases]
    return {'roadmap': roadmap,
            'aiTools': _texts(data['aiTools'], 3, MAX_TOOL_CHARS, 'aiTools'),
            'humanEdge': _texts(data['humanEdge'], 3, MAX_EDGE_CHARS, 'humanEdge')}


def _occupations(codes=None):
    """[(code, title, tasks, related titles)] for visible occupations, optionally only the given codes."""
    hidden = hidden_codes_from(PROFESSIONS)
    occupations = _rows(OUT / 'onet_occupations.csv')
    titles = {o['onetsoc_code']: o['title'] for o in occupations}
    automation = {a['task_id']: float(a['automation']) for a in _rows(OUT / 'onet_automation.csv')}
    tasks = {}
    for t in _rows(OUT / 'onet_tasks.csv'):
        tasks.setdefault(t['onetsoc_code'], []).append(t)
    result = []
    for occ in occupations:
        code = occ['onetsoc_code']
        if code in hidden or (codes is not None and code not in codes):
            continue
        top = sorted(tasks[code], key=lambda t: -int(t['share_pct']))[:TOP_TASKS]
        related = [titles[c] for c in occ['related_codes'].split(';') if c in titles][:3]
        result.append((code, occ['title'],
                       [(t['task_statement'], int(t['share_pct']), automation[t['task_id']]) for t in top],
                       related))
    return result


def roadmaps_from(path=TARGET):
    """{code: {'roadmap', 'aiTools', 'humanEdge', 'model'}}; empty when the file does not exist."""
    return json.loads(path.read_text(encoding='utf-8')) if path.exists() else {}


def _write(roadmaps, usage):
    TARGET.write_text(json.dumps(dict(sorted(roadmaps.items())), ensure_ascii=False, indent=1) + '\n',
                      encoding='utf-8')
    usage['occupations_written'] = len(roadmaps)
    USAGE.write_text(json.dumps(usage, indent=2), encoding='utf-8')


def direct(codes, model, max_usd, workers=8, attempts=3):
    """Write missing occupations with parallel requests at list price; stops at the max_usd budget."""
    import anthropic

    list_cost(model, 0, 0)  # fail fast on a model without a known price
    roadmaps = roadmaps_from()
    usage = json.loads(USAGE.read_text(encoding='utf-8')) if USAGE.exists() else {}
    usage = {'model': model, 'effort': EFFORT, 'input_tokens': usage.get('input_tokens', 0),
             'output_tokens': usage.get('output_tokens', 0), 'cost_usd': usage.get('cost_usd', 0.0)}
    spent_before = usage['cost_usd']
    todo = [o for o in _occupations(codes) if o[0] not in roadmaps]
    print(f'{len(roadmaps)} already written, {len(todo)} to write with {model}, budget ${max_usd}', flush=True)
    client = anthropic.Anthropic(max_retries=6)
    lock = threading.Lock()
    failures = []

    def one(occupation):
        code, title, tasks, related = occupation
        last_error = 'not started'
        for _ in range(attempts):
            with lock:
                if usage['cost_usd'] - spent_before >= max_usd:
                    return code, None, 'budget reached'
            try:
                message = client.messages.create(**build_params(title, tasks, related, model=model))
            except anthropic.APIStatusError as e:
                return code, None, f'HTTP {e.status_code}: {str(e.message)[:120]}'
            tin, tout = _usage_tokens(message)
            with lock:
                usage['input_tokens'] += tin
                usage['output_tokens'] += tout
                usage['cost_usd'] = round(usage['cost_usd'] + list_cost(model, tin, tout), 4)
            if message.stop_reason != 'end_turn':
                last_error = f'stop_reason={message.stop_reason}'
                continue
            text = next((b.text for b in message.content if b.type == 'text'), '')
            try:
                return code, {**parse_roadmap(text), 'model': model}, None
            except (ValueError, KeyError, TypeError, json.JSONDecodeError) as e:
                last_error = str(e)[:120]
        return code, None, last_error

    with ThreadPoolExecutor(max_workers=workers) as pool:
        futures = [pool.submit(one, o) for o in todo]
        for done, future in enumerate(as_completed(futures), start=1):
            code, result, error = future.result()
            with lock:
                if result:
                    roadmaps[code] = result
                else:
                    failures.append((code, error))
                if done % 25 == 0 or done == len(todo):
                    usage['failed'] = failures
                    _write(roadmaps, usage)
                    print(f"{done}/{len(todo)} done, cost ${usage['cost_usd'] - spent_before:.2f}", flush=True)
    usage['failed'] = failures
    _write(roadmaps, usage)
    print(f"written: {len(roadmaps)}, failed: {len(failures)}, "
          f"cost this run: ${usage['cost_usd'] - spent_before:.2f}")
    for code, error in failures[:20]:
        print(f'  {code}: {error}')


def _arg(name, default):
    return sys.argv[sys.argv.index(name) + 1] if name in sys.argv else default


if __name__ == '__main__':
    if len(sys.argv) < 2 or sys.argv[1] != 'direct':
        sys.exit(__doc__)
    direct(set(PILOT) if '--pilot' in sys.argv else None,
           _arg('--model', MODEL), float(_arg('--max-usd', '4')))
