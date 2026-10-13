"""Tests for build.py. Run: python -m unittest discover -s tools/onet-import -p "test_*.py" """
import csv
import tempfile
import unittest
import zipfile
from pathlib import Path

from build import build, largest_remainder, soc_major

ONET_FILES = {
    'Occupation Data.txt': [
        ['O*NET-SOC Code', 'Title', 'Description'],
        ['15-1252.00', 'Software Developers', 'Research, design, and develop software.'],
        ['35-2014.00', 'Cooks, Restaurant', 'Prepare dishes in restaurants.'],
        ['11-9199.00', 'Managers, All Other', 'All other managers.'],
    ],
    'Task Statements.txt': [
        ['O*NET-SOC Code', 'Task ID', 'Task', 'Task Type', 'Incumbents Responding', 'Date', 'Domain Source'],
        ['15-1252.00', '3', 'Review code.', 'Supplemental', '', '', ''],
        ['15-1252.00', '1', 'Write code.', 'Core', '', '', ''],
        ['15-1252.00', '2', 'Talk to users.', 'Core', '', '', ''],
        ['35-2014.00', '10', 'Café – menu planning.', 'Core', '', '', ''],
        ['35-2014.00', '11', 'Cook food.', 'Core', '', '', ''],
    ],
    'Alternate Titles.txt': [
        ['O*NET-SOC Code', 'Alternate Title', 'Short Title', 'Source(s)'],
        ['15-1252.00', 'Software Engineer', 'SWE', '08'],
        ['15-1252.00', 'Coder', 'n/a', '08'],
    ],
    'Related Occupations.txt': [
        ['O*NET-SOC Code', 'Related O*NET-SOC Code', 'Relatedness Tier', 'Index'],
        ['15-1252.00', '15-1211.00', 'Primary-Long', '6'],
        ['15-1252.00', '15-1253.00', 'Primary-Short', '2'],
        ['15-1252.00', '15-1251.00', 'Primary-Short', '1'],
    ],
}

AEI_HEADER = ['onetsoc_code', 'occupation_title', 'task_id', 'task_statement', 'time_share', 'physical',
              'exposure_tier', 'reasoning', 'instances', 'sources']
AEI_TASKS = [
    ['15-1252.00', 'Software Developers', '1', 'Write code.', '0.5', 'No', '', '', '', ''],
    ['15-1252.00', 'Software Developers', '2', 'Talk to users.', '0.3', 'No', '', '', '', ''],
    ['15-1252.00', 'Software Developers', '3', 'Review code.', '0.2', 'No', '', '', '', ''],
    ['35-2014.00', 'Cooks, Restaurant', '10', 'Café – menu planning.', '0.6', 'No', '', '', '', ''],
    ['35-2014.00', 'Cooks, Restaurant', '11', 'Cook food.', '0.4', 'Yes', 'E1', '', '', ''],
]
# 'Write code. ' has a trailing space on purpose; 'Talk to users.' has no row.
PENETRATION = [['task', 'penetration'], ['Write code. ', '0.8'], ['Review code.', '0.0'],
               ['Café – menu planning.', '0.1'], ['Cook food.', '0.0']]


def write_csv(path, rows):
    with path.open('w', encoding='utf-8', newline='') as f:
        csv.writer(f).writerows(rows)


def make_fixture(root, aei_tasks=None):
    raw = root / 'raw'
    raw.mkdir()
    onet_zip = raw / 'db.zip'
    with zipfile.ZipFile(onet_zip, 'w') as z:
        for name, rows in ONET_FILES.items():
            z.writestr('db_29_3_text/' + name, '\n'.join('\t'.join(r) for r in rows) + '\n')
    aei = raw / 'robot_exposure_tasks.csv'
    write_csv(aei, [AEI_HEADER] + (aei_tasks if aei_tasks is not None else AEI_TASKS))
    pen = raw / 'task_penetration.csv'
    write_csv(pen, PENETRATION)
    return onet_zip, aei, pen


def read_rows(path):
    with path.open(encoding='utf-8', newline='') as f:
        return list(csv.DictReader(f))


class BuildTest(unittest.TestCase):

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.out = self.root / 'out'
        self.stats = build(*make_fixture(self.root), self.out)

    def tearDown(self):
        self.tmp.cleanup()

    def test_build_writes_three_files_sorted(self):
        tasks = read_rows(self.out / 'onet_tasks.csv')
        self.assertEqual([(t['onetsoc_code'], t['task_id']) for t in tasks],
                         [('15-1252.00', '1'), ('15-1252.00', '2'), ('15-1252.00', '3'),
                          ('35-2014.00', '10'), ('35-2014.00', '11')])
        self.assertTrue((self.out / 'onet_occupations.csv').exists())
        alt = read_rows(self.out / 'onet_alternate_titles.csv')
        self.assertEqual([a['alternate_title'] for a in alt], ['Coder', 'Software Engineer'])
        self.assertEqual(alt[0]['short_title'], '')
        self.assertEqual(self.stats['occupations'], 2)
        self.assertEqual(self.stats['tasks'], 5)

    def test_shares_sum_to_100_per_occupation(self):
        totals = {}
        for t in read_rows(self.out / 'onet_tasks.csv'):
            totals[t['onetsoc_code']] = totals.get(t['onetsoc_code'], 0) + int(t['share_pct'])
        self.assertEqual(totals, {'15-1252.00': 100, '35-2014.00': 100})

    def test_occupation_without_tasks_is_excluded(self):
        codes = [o['onetsoc_code'] for o in read_rows(self.out / 'onet_occupations.csv')]
        self.assertEqual(codes, ['15-1252.00', '35-2014.00'])

    def test_occupation_fields(self):
        occ = read_rows(self.out / 'onet_occupations.csv')[0]
        self.assertEqual(occ['title'], 'Software Developers')
        self.assertEqual(occ['soc_major'], '15')
        self.assertEqual(occ['soc_major_title'], 'Computer and Mathematical')
        self.assertEqual(occ['task_count'], '3')

    def test_related_codes_primary_short_in_index_order(self):
        occ = read_rows(self.out / 'onet_occupations.csv')[0]
        self.assertEqual(occ['related_codes'], '15-1251.00;15-1253.00')

    def test_penetration_matched_after_strip(self):
        tasks = {t['task_id']: t for t in read_rows(self.out / 'onet_tasks.csv')}
        self.assertEqual(tasks['1']['penetration'], '0.8')
        self.assertEqual(tasks['2']['penetration'], '')
        self.assertEqual(self.stats['penetration_unmatched'], 1)

    def test_non_ascii_preserved(self):
        tasks = {t['task_id']: t for t in read_rows(self.out / 'onet_tasks.csv')}
        self.assertEqual(tasks['10']['task_statement'], 'Café – menu planning.')

    def test_deterministic(self):
        again = self.root / 'again'
        build(self.root / 'raw' / 'db.zip', self.root / 'raw' / 'robot_exposure_tasks.csv',
              self.root / 'raw' / 'task_penetration.csv', again)
        for name in ['onet_occupations.csv', 'onet_tasks.csv', 'onet_alternate_titles.csv']:
            self.assertEqual((self.out / name).read_bytes(), (again / name).read_bytes(), name)


class BuildMismatchTest(unittest.TestCase):

    def test_task_in_aei_missing_from_onet_raises(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            extra = AEI_TASKS + [['15-1252.00', 'Software Developers', '99', 'Ghost task.', '0.0', 'No',
                                  '', '', '', '']]
            with self.assertRaises(ValueError) as ctx:
                build(*make_fixture(root, extra), root / 'out')
            self.assertIn('99', str(ctx.exception))

    def test_task_in_onet_missing_from_aei_raises(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            with self.assertRaises(ValueError) as ctx:
                build(*make_fixture(root, AEI_TASKS[:-1]), root / 'out')
            self.assertIn('11', str(ctx.exception))


class LargestRemainderTest(unittest.TestCase):

    def test_exact_split(self):
        self.assertEqual(largest_remainder([0.5, 0.3, 0.2]), [50, 30, 20])

    def test_thirds_sum_to_100(self):
        result = largest_remainder([1 / 3] * 3)
        self.assertEqual(sum(result), 100)
        self.assertEqual(sorted(result), [33, 33, 34])

    def test_rounding_drift_still_sums_to_100(self):
        self.assertEqual(sum(largest_remainder([0.996, 0.004])), 100)

    def test_tiny_share_can_be_zero(self):
        result = largest_remainder([0.999, 0.001])
        self.assertEqual(sum(result), 100)
        self.assertEqual(result[-1], 0)


class SocMajorTest(unittest.TestCase):

    def test_computer_group(self):
        self.assertEqual(soc_major('15-1252.00'), ('15', 'Computer and Mathematical'))

    def test_healthcare_group(self):
        self.assertEqual(soc_major('29-1141.00')[1], 'Healthcare Practitioners and Technical')

    def test_unknown_group_raises(self):
        with self.assertRaises(ValueError):
            soc_major('99-0000.00')


if __name__ == '__main__':
    unittest.main()
