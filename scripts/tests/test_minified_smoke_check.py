"""The R8 smoke lane must fail unless MinifiedSmokeTest is executed and passed."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('smoke_check', Path(__file__).parents[1] / 'check-minified-smoke-results.py')
smoke = importlib.util.module_from_spec(spec)
spec.loader.exec_module(smoke)

CLASS, NAME = smoke.EXPECTED
PASS = f'<testsuites><testsuite name="t"><testcase classname="{CLASS}" name="{NAME}"/></testsuite></testsuites>'
SKIPPED = f'<testsuites><testsuite name="t"><testcase classname="{CLASS}" name="{NAME}"><skipped/></testcase></testsuite></testsuites>'
FAILED = f'<testsuites><testsuite name="t"><testcase classname="{CLASS}" name="{NAME}"><failure>boom</failure></testcase></testsuite></testsuites>'
# What AGP wrote for every R8 lane run so far (runs 84-93): green Gradle, crashed instrumentation.
CRASHED = ('<testsuites><testsuite name="android-test"><testcase name="testRunFailed" classname="android-test">'
           '<failure>Test run failed to complete. Instrumentation run failed due to Process crashed.</failure>'
           '</testcase></testsuite></testsuites>')


class MinifiedSmokeCheckTest(unittest.TestCase):
    def run_check(self, xml=None, second=None):
        with tempfile.TemporaryDirectory() as tmp:
            if xml is not None:
                (Path(tmp) / 'TEST-a.xml').write_text(xml)
            if second is not None:
                (Path(tmp) / 'TEST-b.xml').write_text(second)
            return smoke.check(tmp)

    def test_executed_and_passed_is_accepted(self):
        self.assertTrue(self.run_check(PASS)[0])

    def test_missing_xml_fails(self):
        self.assertFalse(self.run_check()[0])

    def test_process_crashed_fails_even_though_gradle_was_green(self):
        ok, message = self.run_check(CRASHED)
        self.assertFalse(ok)
        self.assertIn('Process crashed', message)

    def test_skipped_fails(self):
        self.assertFalse(self.run_check(SKIPPED)[0])

    def test_failed_fails(self):
        self.assertFalse(self.run_check(FAILED)[0])

    def test_duplicate_fails(self):
        self.assertFalse(self.run_check(PASS, PASS)[0])

    def test_other_cases_only_fails(self):
        other = '<testsuites><testsuite name="t"><testcase classname="x.Y" name="z"/></testsuite></testsuites>'
        self.assertFalse(self.run_check(other)[0])


if __name__ == '__main__':
    unittest.main()
