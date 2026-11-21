"""Preserve actual instrumentation failure descriptions when raw archives are inaccessible."""
import importlib.util
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

spec = importlib.util.spec_from_file_location('manifest_failure_details', Path(__file__).parents[1] / 'device_qa_manifest.py')
manifest = importlib.util.module_from_spec(spec)
spec.loader.exec_module(manifest)


class DeviceFailureCoverageEvidenceTest(unittest.TestCase):
    def test_body_only_android_failure_retains_the_actual_assertion(self):
        case = ET.fromstring('<testcase><failure>java.lang.AssertionError: Password draft length changed\n at fixture.Test.case(Test.kt:7)</failure></testcase>')
        self.assertIn('Password draft length changed', manifest.failure_details(case))

    def test_message_attribute_remains_the_preferred_description(self):
        case = ET.fromstring('<testcase><failure message="useful assertion">long secondary stack trace</failure></testcase>')
        self.assertEqual('useful assertion', manifest.failure_details(case))

    def test_empty_attribute_falls_back_to_body_for_each_nonpassing_outcome(self):
        for tag in ('failure', 'error', 'skipped'):
            with self.subTest(outcome=tag):
                case = ET.fromstring(f'<testcase><{tag} message=""> useful {tag} body </{tag}></testcase>')
                self.assertEqual(f'useful {tag} body', manifest.failure_details(case))
