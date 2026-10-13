"""Tests for rate_tasks.py (no network). Run: python -m unittest discover -s tools/onet-import -p "test_*.py" """
import json
import unittest

from rate_tasks import MODEL, PRICES_BATCH, batch_cost, build_params, parse_ratings

TASKS = [('1', 'Write code.', 60), ('2', 'Talk to users.', 40)]


class BuildParamsTest(unittest.TestCase):

    def setUp(self):
        self.params = build_params('Software Developers', TASKS, 'RUBRIC TEXT')

    def test_model_and_rubric_in_system(self):
        self.assertEqual(self.params['model'], MODEL)
        self.assertEqual(MODEL, 'claude-opus-5')
        self.assertEqual(self.params['system'][0]['text'], 'RUBRIC TEXT')

    def test_user_message_lists_every_task_with_id_and_share(self):
        content = self.params['messages'][0]['content']
        self.assertIn('Software Developers', content)
        self.assertIn('task_id 1 (60% of working time): Write code.', content)
        self.assertIn('task_id 2 (40% of working time): Talk to users.', content)

    def test_structured_output_schema(self):
        schema = self.params['output_config']['format']['schema']
        self.assertEqual(schema['required'], ['ratings'])
        item = schema['properties']['ratings']['items']
        self.assertEqual(sorted(item['required']), ['score', 'task_id'])
        self.assertFalse(item['additionalProperties'])


class ParseRatingsTest(unittest.TestCase):

    def test_valid(self):
        text = json.dumps({'ratings': [{'task_id': '1', 'score': 0.8}, {'task_id': '2', 'score': 0.2}]})
        self.assertEqual(parse_ratings(text, {'1', '2'}), {'1': 0.8, '2': 0.2})

    def test_missing_task_raises(self):
        text = json.dumps({'ratings': [{'task_id': '1', 'score': 0.8}]})
        with self.assertRaisesRegex(ValueError, 'missing'):
            parse_ratings(text, {'1', '2'})

    def test_unknown_task_raises(self):
        text = json.dumps({'ratings': [{'task_id': '1', 'score': 0.8}, {'task_id': '2', 'score': 0.2},
                                       {'task_id': '9', 'score': 0.5}]})
        with self.assertRaisesRegex(ValueError, 'unexpected'):
            parse_ratings(text, {'1', '2'})

    def test_out_of_range_raises(self):
        text = json.dumps({'ratings': [{'task_id': '1', 'score': 1.2}, {'task_id': '2', 'score': 0.2}]})
        with self.assertRaisesRegex(ValueError, 'range'):
            parse_ratings(text, {'1', '2'})

    def test_duplicate_raises(self):
        text = json.dumps({'ratings': [{'task_id': '1', 'score': 0.8}, {'task_id': '1', 'score': 0.7},
                                       {'task_id': '2', 'score': 0.2}]})
        with self.assertRaisesRegex(ValueError, 'duplicate'):
            parse_ratings(text, {'1', '2'})


class CostTest(unittest.TestCase):

    def test_batch_prices_are_half_of_list(self):
        self.assertEqual(PRICES_BATCH, {'input': 2.50, 'output': 12.50})

    def test_cost(self):
        self.assertAlmostEqual(batch_cost(input_tokens=2_000_000, output_tokens=400_000), 5.0 + 5.0)


if __name__ == '__main__':
    unittest.main()
