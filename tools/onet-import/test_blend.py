"""Tests for blend.py. Run: python -m unittest discover -s tools/onet-import -p "test_*.py" """
import csv
import json
import tempfile
import unittest
from pathlib import Path

from blend import B_SCORES, apply_calibration, blend_task, fit_calibration, occupation_exposure, run


class BlendTaskTest(unittest.TestCase):

    def test_both_sources_average(self):
        self.assertAlmostEqual(blend_task(0.6, 'E1'), 0.5 * 0.6 + 0.5 * 0.80)

    def test_only_llm(self):
        self.assertEqual(blend_task(0.42, None), 0.42)

    def test_only_human_label(self):
        self.assertEqual(blend_task(None, 'E2'), B_SCORES['E2'])

    def test_neither(self):
        self.assertIsNone(blend_task(None, None))

    def test_label_values(self):
        self.assertEqual(B_SCORES, {'E0': 0.15, 'E1': 0.80, 'E2': 0.55})


class ExposureTest(unittest.TestCase):

    def test_weighted_sum_on_0_100_scale(self):
        self.assertAlmostEqual(occupation_exposure([(60, 0.5), (40, 0.25)]), 40.0)


class CalibrationTest(unittest.TestCase):

    def test_within_threshold_is_identity(self):
        a, b, mad = fit_calibration([(50, 55), (30, 25), (70, 72)])
        self.assertEqual((a, b), (1.0, 0.0))
        self.assertLess(mad, 10)

    def test_systematic_offset_is_fitted(self):
        # curated is always 20 points higher: expect a ~1, b ~20
        a, b, mad = fit_calibration([(20, 40), (40, 60), (60, 80)])
        self.assertAlmostEqual(a, 1.0, places=6)
        self.assertAlmostEqual(b, 20.0, places=6)
        self.assertAlmostEqual(mad, 20.0)

    def test_apply_shifts_exposure_by_b_and_clamps(self):
        # x' = a*x + b/100 keeps exposure' = a*exposure + b when shares sum to 100
        self.assertAlmostEqual(apply_calibration(0.5, 1.0, 20.0), 0.7)
        self.assertEqual(apply_calibration(0.95, 1.0, 20.0), 1.0)
        self.assertEqual(apply_calibration(0.05, 1.0, -20.0), 0.0)


def write_csv(path, header, rows):
    with path.open('w', encoding='utf-8', newline='') as f:
        w = csv.writer(f, lineterminator='\n')
        w.writerow(header)
        w.writerows(rows)


class RunTest(unittest.TestCase):

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        root = Path(self.tmp.name)
        self.out = root / 'out'
        self.out.mkdir()
        write_csv(self.out / 'onet_tasks.csv',
                  ['onetsoc_code', 'task_id', 'task_statement', 'task_type', 'time_share', 'share_pct',
                   'physical', 'penetration'],
                  [['15-1252.00', '1', 'Write code.', 'Core', '0.6', '60', 'No', '1.0'],
                   ['15-1252.00', '2', 'Talk to users.', 'Core', '0.4', '40', 'No', '0.0'],
                   ['47-2111.00', '3', 'Wire buildings.', 'Core', '1.0', '100', 'Yes', '']])
        self.labels = root / 'full_labelset.tsv'
        self.labels.write_text('\tO*NET-SOC Code\tTask ID\thuman_exposure_agg\n'
                               '0\t15-1252.00\t1.0\tE1\n0\t15-1252.00\t2.0\tE0\n0\t47-2111.00\t3.0\tE0\n',
                               encoding='utf-8')
        self.professions = root / 'professions.json'
        self.professions.write_text(json.dumps([{'id': 'software-developer', 'name': 'Software developer',
                                                 'onetCodes': ['15-1252.00'], 'tasks': [{'share': 100, 'automation': 0.54}]}]),
                                    encoding='utf-8')

    def tearDown(self):
        self.tmp.cleanup()

    def rows(self, name):
        with (self.out / name).open(encoding='utf-8', newline='') as f:
            return list(csv.DictReader(f))

    def test_b_only_run_writes_automation_and_flags(self):
        stats = run(self.out, None, self.labels, self.professions)
        auto = {r['task_id']: r for r in self.rows('onet_automation.csv')}
        self.assertEqual(auto['1']['b_label'], 'E1')
        self.assertEqual(auto['1']['llm_score'], '')
        self.assertEqual(auto['1']['observed_use'], 'true')
        self.assertEqual(auto['2']['observed_use'], 'false')
        self.assertEqual(auto['3']['observed_use'], 'false')
        # dev exposure = 60*0.80 + 40*0.15 = 54 == curated 54 -> identity calibration
        self.assertEqual(stats['calibration'], (1.0, 0.0))
        self.assertAlmostEqual(float(auto['1']['automation']), 0.80)
        self.assertTrue((self.out / 'calibration.md').exists())

    def test_llm_ratings_are_blended(self):
        ratings = self.out / 'ratings_llm.csv'
        write_csv(ratings, ['task_id', 'llm_score'], [['1', '0.6'], ['2', '0.2'], ['3', '0.1']])
        run(self.out, ratings, self.labels, self.professions)
        auto = {r['task_id']: r for r in self.rows('onet_automation.csv')}
        self.assertAlmostEqual(float(auto['1']['blended']), 0.5 * 0.6 + 0.5 * 0.80)

    def test_observed_use_with_low_automation_goes_to_review(self):
        ratings = self.out / 'ratings_llm.csv'
        # task 1 is observed in use, but LLM 0.0 + human E0 blend to 0.075 (< 0.3)
        self.labels.write_text('\tO*NET-SOC Code\tTask ID\thuman_exposure_agg\n'
                               '0\t15-1252.00\t1.0\tE0\n0\t15-1252.00\t2.0\tE0\n0\t47-2111.00\t3.0\tE0\n',
                               encoding='utf-8')
        write_csv(ratings, ['task_id', 'llm_score'], [['1', '0.0'], ['2', '0.2'], ['3', '0.1']])
        # dev exposure = 60*0.075 + 40*0.175 = 11.5; match it so calibration stays the identity
        self.professions.write_text(json.dumps([{'id': 'software-developer', 'name': 'Software developer',
                                                 'onetCodes': ['15-1252.00'], 'tasks': [{'share': 100, 'automation': 0.115}]}]),
                                    encoding='utf-8')
        stats = run(self.out, ratings, self.labels, self.professions)
        self.assertEqual(stats['calibration'], (1.0, 0.0))
        review = self.rows('review.csv')
        self.assertEqual([r['task_id'] for r in review], ['1'])
        self.assertEqual(stats['review'], 1)

    def test_preview_fills_missing_with_occupation_mean(self):
        self.labels.write_text('\tO*NET-SOC Code\tTask ID\thuman_exposure_agg\n'
                               '0\t15-1252.00\t1.0\tE1\n0\t47-2111.00\t3.0\tE0\n', encoding='utf-8')
        stats = run(self.out, None, self.labels, self.professions, fill_missing=True)
        auto = {r['task_id']: r for r in self.rows('onet_automation.csv')}
        self.assertAlmostEqual(float(auto['2']['blended']), 0.80)
        self.assertEqual(stats['filled'], 1)

    def test_task_without_any_score_raises(self):
        self.labels.write_text('\tO*NET-SOC Code\tTask ID\thuman_exposure_agg\n0\t15-1252.00\t1.0\tE1\n',
                               encoding='utf-8')
        with self.assertRaises(ValueError):
            run(self.out, None, self.labels, self.professions)


if __name__ == '__main__':
    unittest.main()
