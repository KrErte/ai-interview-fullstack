"""Quality gate for written roadmaps (spec O3): rejects entries too thin to be worth publishing.

Calibrated on the 30 pilot roadmaps (2026-10-15). Their shortest item is 48 characters, project 99,
phase title 23, AI tool 16 and human-edge strength 29. Later batches drifted to items such as
"Set rules." and projects such as "Mentor students.", which is the generic-template problem O3 exists
to fix. catalog.py publishes only entries that pass; the others keep the group template and O2 project.
"""
import re

MIN_ITEM_CHARS = 45
MIN_AVG_ITEM_CHARS = 65
MIN_PROJECT_CHARS = 90
MIN_TITLE_CHARS = 20
MIN_TOOL_CHARS = 12
MIN_EDGE_CHARS = 25
# says nothing about what to get better at; name the skill instead
FILLER = ('stays human', 'stay human')


def normalize(text):
    """Lowercase words only, for comparing items across entries."""
    return ' '.join(re.findall(r'[a-z0-9]+', text.lower()))


def overlap(a, b):
    """Share of distinct words (3+ letters) two texts have in common, 0..1 (Jaccard)."""
    wa = {w for w in normalize(a).split() if len(w) >= 3}
    wb = {w for w in normalize(b).split() if len(w) >= 3}
    return len(wa & wb) / len(wa | wb) if wa and wb else 0.0


def quality_problems(entry):
    """Reasons an entry {'roadmap', 'aiTools', 'humanEdge'} is too thin to publish; empty when it passes."""
    problems = []
    items = [item for phase in entry['roadmap'] for item in phase['items']]
    for phase in entry['roadmap']:
        if len(phase['title']) < MIN_TITLE_CHARS:
            problems.append(f"title under {MIN_TITLE_CHARS} chars: {phase['title']!r}")
        if len(phase['project']) < MIN_PROJECT_CHARS:
            problems.append(f"project under {MIN_PROJECT_CHARS} chars: {phase['project']!r}")
    short = [item for item in items if len(item) < MIN_ITEM_CHARS]
    if short:
        problems.append(f'{len(short)} items under {MIN_ITEM_CHARS} chars, e.g. {short[0]!r}')
    if items and sum(map(len, items)) / len(items) < MIN_AVG_ITEM_CHARS:
        problems.append(f'average item under {MIN_AVG_ITEM_CHARS} chars')
    filler = [item for item in items if any(f in item.lower() for f in FILLER)]
    if filler:
        problems.append(f'filler phrase in {filler[0]!r}')
    if len({normalize(item) for item in items}) < len(items):
        problems.append('the same item twice')
    problems += [f'AI tool under {MIN_TOOL_CHARS} chars: {t!r}' for t in entry['aiTools'] if len(t) < MIN_TOOL_CHARS]
    problems += [f'human edge under {MIN_EDGE_CHARS} chars: {t!r}' for t in entry['humanEdge'] if len(t) < MIN_EDGE_CHARS]
    return problems
