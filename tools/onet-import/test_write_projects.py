"""Tests for write_projects.py (no network). Run: python -m unittest discover -s tools/onet-import -p "test_*.py" """
import json
import unittest

from rate_tasks import MODEL
from write_projects import MAX_PROJECT_CHARS, PRICES_LIST, build_params, list_cost, parse_projects

PHASES = [{'title': 'Automate the routine', 'items': ['a', 'b', 'c'], 'project': 'template 1'},
          {'title': 'Analysis and advice', 'items': ['d', 'e', 'f'], 'project': 'template 2'},
          {'title': 'Process owner', 'items': ['g', 'h', 'i'], 'project': 'template 3'}]
TASKS = ['Prepare tax returns.', 'Reconcile accounts.']


class BuildParamsTest(unittest.TestCase):

    def test_prompt_has_occupation_tasks_and_phases(self):
        params = build_params('Tax Preparers', TASKS, PHASES, 'GUIDE')
        self.assertEqual(params['model'], MODEL)
        self.assertEqual(params['system'][0]['text'], 'GUIDE')
        content = params['messages'][0]['content']
        self.assertIn('Occupation: Tax Preparers', content)
        self.assertIn('- Prepare tax returns.', content)
        self.assertIn('Phase 2: Analysis and advice (d; e; f)', content)

    def test_model_can_be_chosen(self):
        self.assertEqual(build_params('X', TASKS, PHASES, 'G', model='claude-sonnet-5')['model'], 'claude-sonnet-5')

    def test_schema_requires_three_phase_projects(self):
        schema = build_params('X', TASKS, PHASES, 'G')['output_config']['format']['schema']
        item = schema['properties']['projects']['items']
        self.assertEqual(sorted(item['required']), ['phase', 'project'])


class ListCostTest(unittest.TestCase):

    def test_list_prices(self):
        self.assertEqual(PRICES_LIST['claude-sonnet-5'], {'input': 2.0, 'output': 10.0})
        self.assertEqual(PRICES_LIST['claude-opus-5'], {'input': 5.0, 'output': 25.0})

    def test_cost_for_sonnet(self):
        self.assertAlmostEqual(list_cost('claude-sonnet-5', 1_000_000, 100_000), 2.0 + 1.0)

    def test_unknown_model_raises(self):
        with self.assertRaises(KeyError):
            list_cost('claude-unknown', 1, 1)


class ParseProjectsTest(unittest.TestCase):

    def ok(self, projects):
        return json.dumps({'projects': projects})

    def test_valid_sorted_by_phase(self):
        text = self.ok([{'phase': 2, 'project': 'B.'}, {'phase': 1, 'project': 'A.'}, {'phase': 3, 'project': 'C.'}])
        self.assertEqual(parse_projects(text), ['A.', 'B.', 'C.'])

    def test_missing_phase_raises(self):
        with self.assertRaisesRegex(ValueError, 'phases'):
            parse_projects(self.ok([{'phase': 1, 'project': 'A.'}, {'phase': 2, 'project': 'B.'}]))

    def test_duplicate_phase_raises(self):
        with self.assertRaisesRegex(ValueError, 'phases'):
            parse_projects(self.ok([{'phase': 1, 'project': 'A.'}, {'phase': 1, 'project': 'B.'},
                                    {'phase': 3, 'project': 'C.'}]))

    def test_too_long_or_empty_raises(self):
        with self.assertRaisesRegex(ValueError, 'length'):
            parse_projects(self.ok([{'phase': 1, 'project': 'x' * (MAX_PROJECT_CHARS + 1)},
                                    {'phase': 2, 'project': 'B.'}, {'phase': 3, 'project': 'C.'}]))
        with self.assertRaisesRegex(ValueError, 'length'):
            parse_projects(self.ok([{'phase': 1, 'project': '  '}, {'phase': 2, 'project': 'B.'},
                                    {'phase': 3, 'project': 'C.'}]))

    def test_whitespace_trimmed(self):
        text = self.ok([{'phase': 1, 'project': ' A. '}, {'phase': 2, 'project': 'B.'}, {'phase': 3, 'project': 'C.'}])
        self.assertEqual(parse_projects(text)[0], 'A.')


if __name__ == '__main__':
    unittest.main()
