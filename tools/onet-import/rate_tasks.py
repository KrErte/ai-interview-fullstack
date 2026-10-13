"""Stage 2, source A: rate every O*NET task's automation (0..1) with Claude via the Message Batches API.

One request per occupation, so the model sees each task in its occupation's context. Rubric: rubric.md.
Needs the `anthropic` package (requirements.txt) and ANTHROPIC_API_KEY in the environment.

    python tools/onet-import/rate_tasks.py submit --pilot   # 24 calibration occupations -> .cache/pilot/
    python tools/onet-import/rate_tasks.py collect --pilot  # waits, writes ratings_llm.csv + usage
    python tools/onet-import/rate_tasks.py submit           # all 923 occupations -> out/ratings_llm.csv
    python tools/onet-import/rate_tasks.py collect
    python tools/onet-import/rate_tasks.py retry            # re-rate occupations missing from ratings_llm.csv
"""
import csv
import json
import sys
import time
from pathlib import Path

from build import CACHE, HERE, OUT

MODEL = 'claude-opus-5'
EFFORT = 'medium'
# claude-opus-5 list price is $5 / $25 per million tokens; batches are billed at 50%
PRICES_BATCH = {'input': 2.50, 'output': 12.50}

RUBRIC = HERE / 'rubric.md'
CURATED = HERE / 'curated_onet_codes.json'

SCHEMA = {
    'type': 'object',
    'properties': {
        'ratings': {
            'type': 'array',
            'items': {
                'type': 'object',
                'properties': {'task_id': {'type': 'string'}, 'score': {'type': 'number'}},
                'required': ['task_id', 'score'],
                'additionalProperties': False,
            },
        },
    },
    'required': ['ratings'],
    'additionalProperties': False,
}


def build_params(title, tasks, rubric):
    """Messages API params for one occupation. tasks: [(task_id, statement, share_pct)]."""
    lines = [f'Occupation: {title}', '', 'Tasks:']
    lines += [f'- task_id {tid} ({share}% of working time): {statement}' for tid, statement, share in tasks]
    return {
        'model': MODEL,
        'max_tokens': 16000,
        'system': [{'type': 'text', 'text': rubric, 'cache_control': {'type': 'ephemeral'}}],
        'output_config': {'effort': EFFORT, 'format': {'type': 'json_schema', 'schema': SCHEMA}},
        'messages': [{'role': 'user', 'content': '\n'.join(lines)}],
    }


def parse_ratings(text, expected_ids):
    """{task_id: score}; raises ValueError unless every expected task is rated once within 0..1."""
    ratings = {}
    for item in json.loads(text)['ratings']:
        tid, score = str(item['task_id']), float(item['score'])
        if tid in ratings:
            raise ValueError(f'duplicate rating for task {tid}')
        if not 0.0 <= score <= 1.0:
            raise ValueError(f'score {score} for task {tid} out of range 0..1')
        ratings[tid] = score
    unexpected = set(ratings) - set(expected_ids)
    if unexpected:
        raise ValueError(f'unexpected task ids {sorted(unexpected)}')
    missing = set(expected_ids) - set(ratings)
    if missing:
        raise ValueError(f'missing ratings for tasks {sorted(missing)}')
    return ratings


def batch_cost(input_tokens, output_tokens):
    """US dollars at batch prices (output tokens include thinking)."""
    return (input_tokens * PRICES_BATCH['input'] + output_tokens * PRICES_BATCH['output']) / 1_000_000


def _occupations(pilot):
    with (OUT / 'onet_occupations.csv').open(encoding='utf-8', newline='') as f:
        titles = {r['onetsoc_code']: r['title'] for r in csv.DictReader(f)}
    with (OUT / 'onet_tasks.csv').open(encoding='utf-8', newline='') as f:
        tasks = {}
        for r in csv.DictReader(f):
            tasks.setdefault(r['onetsoc_code'], []).append((r['task_id'], r['task_statement'], r['share_pct']))
    codes = sorted(titles)
    if pilot:
        wanted = {c for cs in json.loads(CURATED.read_text(encoding='utf-8')).values() for c in cs}
        codes = [c for c in codes if c in wanted]
    return [(c, titles[c], tasks[c]) for c in codes]


def _workdir(pilot):
    path = CACHE / 'pilot' if pilot else OUT
    path.mkdir(parents=True, exist_ok=True)
    return path


def submit(pilot):
    import anthropic
    from anthropic.types.message_create_params import MessageCreateParamsNonStreaming
    from anthropic.types.messages.batch_create_params import Request

    rubric = RUBRIC.read_text(encoding='utf-8')
    occupations = _occupations(pilot)
    requests = [Request(custom_id=code.replace('.', '_'),
                        params=MessageCreateParamsNonStreaming(**build_params(title, tasks, rubric)))
                for code, title, tasks in occupations]
    batch = anthropic.Anthropic().messages.batches.create(requests=requests)
    state = _workdir(pilot) / 'batch.json'
    state.write_text(json.dumps({'batch_id': batch.id, 'model': MODEL, 'effort': EFFORT,
                                 'occupations': len(requests)}, indent=2), encoding='utf-8')
    print(f'submitted batch {batch.id} with {len(requests)} occupations ({state})')


def collect(pilot):
    import anthropic

    workdir = _workdir(pilot)
    batch_id = json.loads((workdir / 'batch.json').read_text(encoding='utf-8'))['batch_id']
    client = anthropic.Anthropic()
    while True:
        batch = client.messages.batches.retrieve(batch_id)
        if batch.processing_status == 'ended':
            break
        print(f'{batch.processing_status}: {batch.request_counts.processing} processing', flush=True)
        time.sleep(60)

    expected = {code.replace('.', '_'): {tid for tid, _, _ in tasks} for code, _, tasks in _occupations(pilot)}
    ratings, failures = {}, []
    input_tokens = output_tokens = 0
    for result in client.messages.batches.results(batch_id):
        cid = result.custom_id
        if result.result.type != 'succeeded':
            failures.append((cid, result.result.type))
            continue
        message = result.result.message
        input_tokens += (message.usage.input_tokens + (message.usage.cache_read_input_tokens or 0)
                         + (message.usage.cache_creation_input_tokens or 0))
        output_tokens += message.usage.output_tokens
        if message.stop_reason != 'end_turn':
            failures.append((cid, f'stop_reason={message.stop_reason}'))
            continue
        text = next((b.text for b in message.content if b.type == 'text'), '')
        try:
            ratings.update(parse_ratings(text, expected[cid]))
        except (ValueError, KeyError, json.JSONDecodeError) as e:
            failures.append((cid, str(e)[:200]))

    with (workdir / 'ratings_llm.csv').open('w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f, lineterminator='\n')
        writer.writerow(['task_id', 'llm_score'])
        writer.writerows(sorted(ratings.items(), key=lambda kv: int(kv[0])))
    usage = {'batch_id': batch_id, 'model': MODEL, 'effort': EFFORT, 'requests': len(expected),
             'failed': failures, 'tasks_rated': len(ratings), 'input_tokens': input_tokens,
             'output_tokens': output_tokens, 'cost_usd': round(batch_cost(input_tokens, output_tokens), 2)}
    (workdir / 'usage.json').write_text(json.dumps(usage, indent=2), encoding='utf-8')
    print(json.dumps({k: v for k, v in usage.items() if k != 'failed'}, indent=2))
    print(f'failed requests: {len(failures)}')
    for cid, reason in failures:
        print(f'  {cid}: {reason}')


def retry(pilot, attempts=3):
    """Re-rate, one synchronous request each, every occupation with a task missing from ratings_llm.csv."""
    import anthropic

    workdir = _workdir(pilot)
    ratings_path, usage_path = workdir / 'ratings_llm.csv', workdir / 'usage.json'
    with ratings_path.open(encoding='utf-8', newline='') as f:
        ratings = {r['task_id']: float(r['llm_score']) for r in csv.DictReader(f)}
    usage = json.loads(usage_path.read_text(encoding='utf-8'))
    rubric = RUBRIC.read_text(encoding='utf-8')
    client = anthropic.Anthropic()
    for code, title, tasks in _occupations(pilot):
        expected = {tid for tid, _, _ in tasks}
        if expected <= set(ratings):
            continue
        for attempt in range(1, attempts + 1):
            message = client.messages.create(**build_params(title, tasks, rubric))
            # synchronous requests are billed at list price, twice the batch price
            usage['input_tokens'] += 2 * (message.usage.input_tokens + (message.usage.cache_read_input_tokens or 0)
                                          + (message.usage.cache_creation_input_tokens or 0))
            usage['output_tokens'] += 2 * message.usage.output_tokens
            text = next((b.text for b in message.content if b.type == 'text'), '')
            try:
                ratings.update(parse_ratings(text, expected))
                print(f'{code}: rated {len(expected)} tasks (attempt {attempt})')
                break
            except (ValueError, KeyError, json.JSONDecodeError) as e:
                print(f'{code}: attempt {attempt} rejected: {str(e)[:120]}')
        else:
            print(f'{code}: still missing after {attempts} attempts')

    with ratings_path.open('w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f, lineterminator='\n')
        writer.writerow(['task_id', 'llm_score'])
        writer.writerows(sorted(ratings.items(), key=lambda kv: int(kv[0])))
    usage['tasks_rated'] = len(ratings)
    usage['cost_usd'] = round(batch_cost(usage['input_tokens'], usage['output_tokens']), 2)
    usage['retried'] = [cid for cid, _ in usage.get('failed', [])]
    usage['failed'] = []
    usage_path.write_text(json.dumps(usage, indent=2), encoding='utf-8')
    print(f"tasks rated: {usage['tasks_rated']}, total cost: ${usage['cost_usd']}")


if __name__ == '__main__':
    commands = {'submit': submit, 'collect': collect, 'retry': retry}
    if len(sys.argv) < 2 or sys.argv[1] not in commands:
        sys.exit(__doc__)
    commands[sys.argv[1]]('--pilot' in sys.argv)
