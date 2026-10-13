"""Build the O*NET occupation/task CSVs from O*NET 29.3 and the Anthropic Economic Index.

Stage 1 of docs/superpowers/specs/2026-10-13-onet-professions-design.md: no automation scores yet.
Standard library only. Run fetch.py first, then: python tools/onet-import/build.py
"""
import csv
import io
import math
import sys
import zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
CACHE = HERE / '.cache'
OUT = HERE / 'out'

# Raw inputs as fetch.py stores them in .cache/
DEFAULT_INPUTS = (
    CACHE / 'db_29_3_text.zip',
    CACHE / 'robot_exposure_tasks.csv',
    CACHE / 'task_penetration.csv',
)

# csv module default field limit is too small for some AEI 'reasoning' cells
csv.field_size_limit(sys.maxsize if sys.maxsize < 2**63 else 2**31 - 1)

# SOC 2018 major groups (first two digits of the O*NET-SOC code).
SOC_MAJOR_GROUPS = {
    '11': 'Management',
    '13': 'Business and Financial Operations',
    '15': 'Computer and Mathematical',
    '17': 'Architecture and Engineering',
    '19': 'Life, Physical, and Social Science',
    '21': 'Community and Social Service',
    '23': 'Legal',
    '25': 'Educational Instruction and Library',
    '27': 'Arts, Design, Entertainment, Sports, and Media',
    '29': 'Healthcare Practitioners and Technical',
    '31': 'Healthcare Support',
    '33': 'Protective Service',
    '35': 'Food Preparation and Serving Related',
    '37': 'Building and Grounds Cleaning and Maintenance',
    '39': 'Personal Care and Service',
    '41': 'Sales and Related',
    '43': 'Office and Administrative Support',
    '45': 'Farming, Fishing, and Forestry',
    '47': 'Construction and Extraction',
    '49': 'Installation, Maintenance, and Repair',
    '51': 'Production',
    '53': 'Transportation and Material Moving',
    '55': 'Military Specific',
}


def largest_remainder(shares, total=100):
    """Integer percentages that sum to exactly `total` (Hamilton / largest remainder method).

    Ties in the remainder go to the lower index, so the result is deterministic.
    """
    weight = sum(shares)
    exact = [s / weight * total for s in shares]
    result = [math.floor(x) for x in exact]
    by_remainder = sorted(range(len(shares)), key=lambda i: (-(exact[i] - result[i]), i))
    for i in by_remainder[:total - sum(result)]:
        result[i] += 1
    return result


def soc_major(code):
    """('15', 'Computer and Mathematical') for '15-1252.00'."""
    group = code[:2]
    if group not in SOC_MAJOR_GROUPS:
        raise ValueError(f"Unknown SOC major group in code '{code}'")
    return group, SOC_MAJOR_GROUPS[group]


def _read_onet(onet_zip, name):
    """Rows of one tab-delimited O*NET file inside the database zip, as dicts."""
    with zipfile.ZipFile(onet_zip) as z:
        member = next((n for n in z.namelist() if n.endswith('/' + name)), None)
        if member is None:
            raise ValueError(f"'{name}' not found in {onet_zip}")
        text = z.read(member).decode('utf-8')
    return list(csv.DictReader(io.StringIO(text), delimiter='\t'))


def _read_csv(path):
    with Path(path).open(encoding='utf-8', newline='') as f:
        return list(csv.DictReader(f))


def _write_csv(path, header, rows):
    with path.open('w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f, lineterminator='\n')
        writer.writerow(header)
        writer.writerows(rows)


def build(onet_zip, aei_tasks_csv, aei_penetration_csv, out_dir):
    """Join O*NET 29.3 with AEI time shares and penetration; write the three stage-1 CSVs.

    Raises ValueError when the O*NET and AEI task sets differ, instead of silently dropping tasks.
    """
    out_dir = Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    occupations = {r['O*NET-SOC Code']: r for r in _read_onet(onet_zip, 'Occupation Data.txt')}
    onet_tasks = {r['Task ID']: r for r in _read_onet(onet_zip, 'Task Statements.txt')}
    aei_tasks = {r['task_id']: r for r in _read_csv(aei_tasks_csv)}
    penetration = {r['task'].strip(): r['penetration'] for r in _read_csv(aei_penetration_csv)}

    only_aei = sorted(set(aei_tasks) - set(onet_tasks), key=int)
    only_onet = sorted(set(onet_tasks) - set(aei_tasks), key=int)
    if only_aei or only_onet:
        raise ValueError(f'Task sets differ. Only in AEI: {only_aei[:10]}; only in O*NET: {only_onet[:10]}')

    tasks_by_occ = {}
    for task_id, t in onet_tasks.items():
        code = t['O*NET-SOC Code']
        if code not in occupations:
            raise ValueError(f"Task {task_id} belongs to unknown occupation '{code}'")
        tasks_by_occ.setdefault(code, []).append(task_id)

    related = {}
    for r in _read_onet(onet_zip, 'Related Occupations.txt'):
        if r['Relatedness Tier'] == 'Primary-Short':
            related.setdefault(r['O*NET-SOC Code'], []).append((int(r['Index']), r['Related O*NET-SOC Code']))

    task_rows, occ_rows = [], []
    unmatched = 0
    for code in sorted(tasks_by_occ):
        ids = sorted(tasks_by_occ[code], key=int)
        shares = [float(aei_tasks[i]['time_share']) for i in ids]
        pcts = largest_remainder(shares)
        for task_id, share, pct in zip(ids, shares, pcts):
            statement = onet_tasks[task_id]['Task']
            pen = penetration.get(statement.strip(), '')
            if pen == '':
                unmatched += 1
            task_rows.append([code, task_id, statement, onet_tasks[task_id]['Task Type'],
                              share, pct, aei_tasks[task_id]['physical'], pen])
        group, group_title = soc_major(code)
        rel = ';'.join(c for _, c in sorted(related.get(code, [])))
        occ_rows.append([code, occupations[code]['Title'], group, group_title, len(ids), rel])

    alt_rows = sorted(
        (r['O*NET-SOC Code'], r['Alternate Title'], '' if r['Short Title'] == 'n/a' else r['Short Title'])
        for r in _read_onet(onet_zip, 'Alternate Titles.txt')
        if r['O*NET-SOC Code'] in tasks_by_occ
    )

    _write_csv(out_dir / 'onet_occupations.csv',
               ['onetsoc_code', 'title', 'soc_major', 'soc_major_title', 'task_count', 'related_codes'], occ_rows)
    _write_csv(out_dir / 'onet_tasks.csv',
               ['onetsoc_code', 'task_id', 'task_statement', 'task_type', 'time_share', 'share_pct',
                'physical', 'penetration'], task_rows)
    _write_csv(out_dir / 'onet_alternate_titles.csv', ['onetsoc_code', 'alternate_title', 'short_title'], alt_rows)

    return {'occupations': len(occ_rows), 'tasks': len(task_rows), 'penetration_unmatched': unmatched}


if __name__ == '__main__':
    missing = [p for p in DEFAULT_INPUTS if not p.exists()]
    if missing:
        sys.exit(f'Missing inputs {[p.name for p in missing]}. Run: python tools/onet-import/fetch.py')
    stats = build(*DEFAULT_INPUTS, OUT)
    for key, value in stats.items():
        print(f'{key}: {value}')
