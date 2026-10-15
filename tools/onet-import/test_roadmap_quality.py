"""Tests for roadmap_quality.py. Run: python -m unittest discover -s tools/onet-import -p "test_*.py" """
import json
import unittest

from roadmap_quality import MIN_ITEM_CHARS, MIN_PROJECT_CHARS, normalize, overlap, quality_problems
from write_roadmaps import TARGET, _occupations


def entry(**changes):
    data = {'roadmap': [{'title': f'Phase {i}: AI for experience studies',
                         'items': [f'Step {i}.{j}: rebuild one mortality study with AI and reconcile it to last year.'
                                   for j in (1, 2, 3)],
                         'project': f'Project {i}: redo one lapse study with AI help, reconcile it to the old result '
                                    f'and record the hours per step.'}
                        for i in (1, 2, 3)],
            'aiTools': ['Coding assistant for R scripts', 'Claude for memo drafts', 'Copilot in Excel'],
            'humanEdge': ['Signing off reserves and answering for them', 'Explaining uncertainty to a board',
                          'Judging whether a model fits a new product']}
    data.update(changes)
    return data


class QualityProblemsTest(unittest.TestCase):

    def test_full_entry_passes(self):
        self.assertEqual(quality_problems(entry()), [])

    def test_thin_entry_fails_on_every_field(self):
        thin = entry(roadmap=[{'title': 'Teaching', 'items': ['Set rules.', 'Advise students.', 'Track progress.'],
                               'project': 'Mentor students.'} for _ in (1, 2, 3)],
                     aiTools=['Chat', 'LMS', 'Apps'], humanEdge=['Teaching', 'Mentoring', 'Research'])
        text = ' | '.join(quality_problems(thin))
        for reason in ('title under', 'project under', 'items under', 'average item', 'AI tool under',
                       'human edge under'):
            self.assertIn(reason, text)

    def test_one_short_item_or_project_fails(self):
        short_item = entry()
        short_item['roadmap'][1]['items'][0] = 'x' * (MIN_ITEM_CHARS - 1)
        self.assertTrue(any('items under' in p for p in quality_problems(short_item)))
        short_project = entry()
        short_project['roadmap'][2]['project'] = 'y' * (MIN_PROJECT_CHARS - 1)
        self.assertTrue(any('project under' in p for p in quality_problems(short_project)))

    def test_filler_phrase_fails(self):
        filler = entry()
        filler['roadmap'][2]['items'][2] = 'Teaching, mentoring and leading discussions in class stays human work.'
        self.assertTrue(any('filler' in p for p in quality_problems(filler)))

    def test_same_item_twice_fails(self):
        twice = entry()
        twice['roadmap'][1]['items'][0] = twice['roadmap'][0]['items'][0].upper()
        self.assertIn('the same item twice', quality_problems(twice))


class TextHelpersTest(unittest.TestCase):

    def test_normalize_keeps_lowercase_words(self):
        self.assertEqual(normalize('Set rules, then  CHECK them!'), 'set rules then check them')

    def test_overlap(self):
        self.assertEqual(overlap('Draft papers with AI.', 'draft papers with ai'), 1.0)
        self.assertEqual(overlap('Draft papers with AI.', 'Cook dinner tonight.'), 0.0)
        self.assertEqual(overlap('', 'Anything'), 0.0)


class CommittedRoadmapsTest(unittest.TestCase):

    def test_every_written_roadmap_passes_and_is_a_visible_occupation(self):
        roadmaps = json.loads(TARGET.read_text(encoding='utf-8')) if TARGET.exists() else {}
        visible = {code for code, *_ in _occupations(set(roadmaps))}
        for code, roadmap in roadmaps.items():
            self.assertEqual(quality_problems(roadmap), [], code)
            self.assertIn(code, visible)


if __name__ == '__main__':
    unittest.main()
