"""Tests for catalog.py. Run: python -m unittest discover -s tools/onet-import -p "test_*.py" """
import csv
import json
import tempfile
import unittest
from pathlib import Path

from build import SOC_MAJOR_GROUPS
from catalog import TEMPLATES, build_aliases, build_catalog, build_hidden


def write_csv(path, header, rows):
    with path.open('w', encoding='utf-8', newline='') as f:
        w = csv.writer(f, lineterminator='\n')
        w.writerow(header)
        w.writerows(rows)


def written_roadmap():
    """A roadmap that passes the quality gate (roadmap_quality.py)."""
    return {'roadmap': [{'title': f'Cook phase {i}: costing and prep with AI',
                         'items': [f'Step {i}.{j}: cost one dish from supplier invoices with AI and check every price.'
                                   for j in (1, 2, 3)],
                         'project': f'Project {i}: cost the full menu with AI, check it against invoices, '
                                    f'and fix three dishes with a weak margin.'}
                        for i in (1, 2, 3)],
            'aiTools': ['Recipe costing app with AI', 'Voice notes for prep lists', 'ChatGPT or Claude for menus'],
            'humanEdge': ['Tasting and adjusting every plate', 'Running a busy line through service',
                          'Training new cooks on the station'],
            'model': 'claude-opus-5'}


def template(tag):
    return {'aiTools': [f'{tag} tool 1', f'{tag} tool 2', f'{tag} tool 3'],
            'roadmap': [{'title': f'{tag} phase {i}', 'items': ['a', 'b', 'c'], 'project': f'{tag} project {i}'}
                        for i in (1, 2, 3)]}


class BuildCatalogTest(unittest.TestCase):

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        out = self.out = Path(self.tmp.name)
        write_csv(out / 'onet_occupations.csv',
                  ['onetsoc_code', 'title', 'soc_major', 'soc_major_title', 'task_count', 'related_codes'],
                  [['15-1252.00', 'Software Developers', '15', 'Computer and Mathematical', '3',
                    '15-1253.00;35-2014.00'],
                   ['15-1253.00', 'Software QA Testers', '15', 'Computer and Mathematical', '1', '15-1252.00'],
                   ['35-2014.00', 'Cooks, Restaurant', '35', 'Food Preparation and Serving Related', '1', '']])
        write_csv(out / 'onet_tasks.csv',
                  ['onetsoc_code', 'task_id', 'task_statement', 'task_type', 'time_share', 'share_pct',
                   'physical', 'penetration'],
                  [['15-1252.00', '1', 'Write code.', 'Core', '0.5', '50', 'No', '1.0'],
                   ['15-1252.00', '2', 'Talk to users.', 'Core', '0.3', '30', 'No', ''],
                   ['15-1252.00', '3', 'Review code.', 'Core', '0.2', '20', 'No', '0.0'],
                   ['15-1253.00', '4', 'Test software.', 'Core', '1.0', '100', 'No', '0.0'],
                   ['35-2014.00', '5', 'Cook food.', 'Core', '1.0', '100', 'Yes', '0.0']])
        write_csv(out / 'onet_automation.csv',
                  ['onetsoc_code', 'task_id', 'llm_score', 'b_label', 'blended', 'automation', 'observed_use'],
                  [['15-1252.00', '1', '0.8', 'E1', '0.8', '0.8', 'true'],
                   ['15-1252.00', '2', '0.2', 'E0', '0.176', '0.176', 'false'],
                   ['15-1252.00', '3', '0.6', 'E2', '0.5753', '0.5753', 'false'],
                   ['15-1253.00', '4', '0.7', 'E1', '0.75', '0.75', 'false'],
                   ['35-2014.00', '5', '0.05', 'E0', '0.1', '0.1', 'false']])
        write_csv(out / 'onet_alternate_titles.csv', ['onetsoc_code', 'alternate_title', 'short_title'],
                  [['15-1252.00', 'Coder', ''], ['15-1252.00', 'Software Engineer', 'SWE']])
        self.templates = {'15': template('IT'), '35': template('Food')}
        self.catalog = build_catalog(out, self.templates, hidden_codes={'15-1253.00'})
        self.by_code = {p['onetCode']: p for p in self.catalog}

    def tearDown(self):
        self.tmp.cleanup()

    def test_profession_shape_and_id(self):
        dev = self.by_code['15-1252.00']
        self.assertEqual(dev['id'], 'onet-15-1252-00')
        self.assertEqual(dev['name'], 'Software Developers')
        self.assertEqual(dev['category'], 'Computer and Mathematical')
        self.assertEqual(dev['alternateTitles'], ['Coder', 'Software Engineer'])

    def test_shares_sum_to_100_and_automation_rounded(self):
        dev = self.by_code['15-1252.00']
        self.assertEqual(sum(t['share'] for t in dev['tasks']), 100)
        self.assertEqual([t['automation'] for t in dev['tasks']], [0.8, 0.18, 0.58])
        self.assertEqual([t['observedUse'] for t in dev['tasks']], [True, False, False])

    def test_human_edge_is_top3_share_times_one_minus_automation(self):
        # 50*(1-0.8)=10, 30*(1-0.176)=24.72, 20*(1-0.5753)=8.49
        self.assertEqual(self.by_code['15-1252.00']['humanEdge'], ['Talk to users.', 'Write code.', 'Review code.'])

    def test_pivot_roles_are_related_titles_in_order(self):
        self.assertEqual(self.by_code['15-1252.00']['pivotRoles'], ['Software QA Testers', 'Cooks, Restaurant'])

    def test_template_by_soc_major(self):
        cook = self.by_code['35-2014.00']
        self.assertEqual(cook['aiTools'], self.templates['35']['aiTools'])
        self.assertEqual(cook['roadmap'][0]['project'], 'Food project 1')

    def test_hidden_codes_excluded(self):
        self.assertNotIn('15-1253.00', self.by_code)

    def test_llm_projects_replace_template_projects_per_occupation(self):
        catalog = build_catalog(self.out, self.templates, hidden_codes=set(),
                                projects={'35-2014.00': ['Cook 1.', 'Cook 2.', 'Cook 3.']})
        by_code = {p['onetCode']: p for p in catalog}
        self.assertEqual([ph['project'] for ph in by_code['35-2014.00']['roadmap']], ['Cook 1.', 'Cook 2.', 'Cook 3.'])
        self.assertEqual(by_code['35-2014.00']['roadmap'][0]['title'], 'Food phase 1')
        # occupations without written projects keep the template's, and the template itself is not modified
        self.assertEqual(by_code['15-1252.00']['roadmap'][0]['project'], 'IT project 1')
        self.assertEqual(self.templates['35']['roadmap'][0]['project'], 'Food project 1')

    def test_written_roadmaps_replace_template_projects_and_human_edge(self):
        written = written_roadmap()
        catalog = build_catalog(self.out, self.templates, hidden_codes=set(),
                                projects={'35-2014.00': ['P1.', 'P2.', 'P3.']}, roadmaps={'35-2014.00': written})
        by_code = {p['onetCode']: p for p in catalog}
        cook = by_code['35-2014.00']
        self.assertEqual(cook['roadmap'], written['roadmap'])
        self.assertEqual(cook['aiTools'], written['aiTools'])
        self.assertEqual(cook['humanEdge'], written['humanEdge'])
        self.assertNotIn('model', cook)
        # others keep the template and the task-statement human edge
        self.assertEqual(by_code['15-1252.00']['roadmap'][0]['title'], 'IT phase 1')
        self.assertEqual(by_code['15-1252.00']['humanEdge'], ['Talk to users.', 'Write code.', 'Review code.'])

    def test_thin_written_roadmap_is_ignored_and_template_kept(self):
        thin = {'roadmap': [{'title': 'Cooking', 'items': ['Set rules.', 'Cook food.', 'Taste it.'],
                             'project': 'Cook more.'} for _ in (1, 2, 3)],
                'aiTools': ['Chat', 'Apps', 'Notes'], 'humanEdge': ['Taste', 'Lead', 'Teach']}
        catalog = build_catalog(self.out, self.templates, hidden_codes=set(),
                                projects={'35-2014.00': ['Cook 1.', 'Cook 2.', 'Cook 3.']},
                                roadmaps={'35-2014.00': thin})
        cook = {p['onetCode']: p for p in catalog}['35-2014.00']
        self.assertEqual(cook['roadmap'][0]['title'], 'Food phase 1')
        self.assertEqual([ph['project'] for ph in cook['roadmap']], ['Cook 1.', 'Cook 2.', 'Cook 3.'])
        self.assertEqual(cook['humanEdge'], ['Cook food.'])

    def test_aliases_carry_titles_of_hidden_occupations(self):
        aliases = build_aliases(self.out, {'software-developer': ['15-1252.00', '15-1253.00'], 'other': []})
        self.assertEqual(aliases, {'software-developer': ['Software Developers', 'Coder', 'Software Engineer',
                                                          'Software QA Testers']})

    def test_hidden_maps_each_hidden_occupation_title_to_its_profession(self):
        hidden = build_hidden(self.out, {'software-developer': ['15-1252.00', '15-1253.00'], 'other': []})
        self.assertEqual(hidden, {'Software Developers': 'software-developer',
                                  'Software QA Testers': 'software-developer'})

    def test_sorted_by_name(self):
        self.assertEqual([p['name'] for p in self.catalog], ['Cooks, Restaurant', 'Software Developers'])


class TemplatesFileTest(unittest.TestCase):

    def test_templates_cover_all_22_groups(self):
        templates = json.loads(TEMPLATES.read_text(encoding='utf-8'))
        expected = set(SOC_MAJOR_GROUPS) - {'55'}  # no military occupations have task data
        self.assertEqual(set(templates), expected)
        for group, t in templates.items():
            self.assertEqual(len(t['aiTools']), 3, group)
            self.assertEqual(len(t['roadmap']), 3, group)
            for phase in t['roadmap']:
                self.assertTrue(phase['title'] and phase['project'], group)
                self.assertEqual(len(phase['items']), 3, group)


if __name__ == '__main__':
    unittest.main()
