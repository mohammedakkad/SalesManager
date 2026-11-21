"""Annotation transport checks use ordinary metadata, never device execution or PDF fixtures."""
import importlib.util
import json
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('annotation_manifest', Path(__file__).parents[1] / 'device_qa_manifest.py')
manifest = importlib.util.module_from_spec(spec)
spec.loader.exec_module(manifest)


class AnnotationBudgetTest(unittest.TestCase):
    def test_small_metadata_keeps_one_complete_json_array(self):
        rows = [{'name': 'metadata', 'value': '100%'}]
        self.assertEqual([json.dumps(rows).replace('%', '%25')], manifest.annotation_chunks(rows))
        self.assertEqual(['[]'], manifest.annotation_chunks([]))

    def test_unicode_and_percent_escaping_fit_bytes_without_losing_or_reordering_rows(self):
        rows = [{'index': index, 'text': 'بيانات % ' * 35} for index in range(29)]
        chunks = manifest.annotation_chunks(rows)
        self.assertGreater(len(chunks), 1)
        self.assertTrue(all(len(chunk.encode('utf-8')) <= 3500 for chunk in chunks))
        decoded = [row for chunk in chunks for row in json.loads(chunk.replace('%25', '%'))]
        self.assertEqual(rows, decoded)

    def test_oversized_single_row_is_rejected_instead_of_truncated(self):
        with self.assertRaisesRegex(ValueError, 'Single annotation row'):
            manifest.annotation_chunks([{'text': 'ع' * 3500}])
