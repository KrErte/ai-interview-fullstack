"""Tests for write_roadmaps.py (no network). Run: python -m unittest discover -s tools/onet-import -p "test_*.py" """
import json
import unittest

from rate_tasks import MODEL
from write_roadmaps import (MAX_EDGE_CHARS, MAX_ITEM_CHARS, MAX_TITLE_CHARS, PILOT, _occupations, build_params,
                            parse_roadmap)

TASKS = [('Prepare tax returns.', 40, 0.7), ('Meet clients.', 30, 0.15)]


def answer(**changes):
    data = {'phases': [{'title': f'Phase {i}', 'items': ['a', 'b', 'c'], 'project': f'Project {i}.'}
                       for i in (1, 2, 3)],
            'aiTools': ['Tool 1', 'Tool 2', 'Tool 3'],
            'humanEdge': ['Edge 1', 'Edge 2', 'Edge 3']}
    data.update(changes)
    return json.dumps(data)


class BuildParamsTest(unittest.TestCase):

    def test_prompt_has_tasks_with_share_and_automation_and_related(self):
        params = build_params('Tax Preparers', TASKS, ['Accountants', 'Bookkeepers'])
        self.assertEqual(params['model'], MODEL)
        content = params['messages'][0]['content']
        self.assertIn('Occupation: Tax Preparers', content)
        self.assertIn('- 40% · 0.70 · Prepare tax returns.', content)
        self.assertIn('Related occupations: Accountants; Bookkeepers', content)

    def test_no_related_line_without_related(self):
        self.assertNotIn('Related', build_params('X', TASKS, [])['messages'][0]['content'])

    def test_schema_requires_phases_tools_and_edge(self):
        schema = build_params('X', TASKS, [])['output_config']['format']['schema']
        self.assertEqual(sorted(schema['required']), ['aiTools', 'humanEdge', 'phases'])


class ParseRoadmapTest(unittest.TestCase):

    def test_valid_answer_is_trimmed(self):
        parsed = parse_roadmap(answer(aiTools=[' Tool 1 ', 'Tool 2', 'Tool 3']))
        self.assertEqual(len(parsed['roadmap']), 3)
        self.assertEqual(parsed['roadmap'][0], {'title': 'Phase 1', 'items': ['a', 'b', 'c'], 'project': 'Project 1.'})
        self.assertEqual(parsed['aiTools'][0], 'Tool 1')

    def test_wrong_phase_count_raises(self):
        with self.assertRaisesRegex(ValueError, '3 phases'):
            parse_roadmap(answer(phases=[{'title': 'T', 'items': ['a', 'b', 'c'], 'project': 'P.'}]))

    def test_wrong_item_count_raises(self):
        phases = json.loads(answer())['phases']
        phases[1]['items'] = ['a', 'b']
        with self.assertRaisesRegex(ValueError, 'items'):
            parse_roadmap(answer(phases=phases))

    def test_wrong_tool_or_edge_count_raises(self):
        with self.assertRaisesRegex(ValueError, 'aiTools'):
            parse_roadmap(answer(aiTools=['a', 'b']))
        with self.assertRaisesRegex(ValueError, 'humanEdge'):
            parse_roadmap(answer(humanEdge=['a', 'b', 'c', 'd']))

    def test_too_long_or_empty_raises(self):
        phases = json.loads(answer())['phases']
        phases[0]['title'] = 'x' * (MAX_TITLE_CHARS + 1)
        with self.assertRaisesRegex(ValueError, 'title length'):
            parse_roadmap(answer(phases=phases))
        phases = json.loads(answer())['phases']
        phases[2]['items'][0] = 'x' * (MAX_ITEM_CHARS + 1)
        with self.assertRaisesRegex(ValueError, 'items length'):
            parse_roadmap(answer(phases=phases))
        with self.assertRaisesRegex(ValueError, 'humanEdge length'):
            parse_roadmap(answer(humanEdge=['x' * (MAX_EDGE_CHARS + 1), 'b', 'c']))
        with self.assertRaisesRegex(ValueError, 'aiTools length'):
            parse_roadmap(answer(aiTools=['  ', 'b', 'c']))


class PilotTest(unittest.TestCase):

    def test_pilot_codes_are_all_visible_occupations(self):
        found = {code for code, *_ in _occupations(set(PILOT))}
        self.assertEqual(found, set(PILOT))
        self.assertEqual(len(PILOT), len(set(PILOT)))


if __name__ == '__main__':
    unittest.main()
