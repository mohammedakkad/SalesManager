"""Isolated integrity fixtures; placeholder execution bytes are never passed to JaCoCo or reported as production coverage."""
import importlib.util
from pathlib import Path
import tempfile
import io
import os
import json
from contextlib import redirect_stdout
from unittest.mock import patch
import unittest

spec = importlib.util.spec_from_file_location('suite_report', Path(__file__).parents[1] / 'report-suite-coverage.py')
report = importlib.util.module_from_spec(spec)
spec.loader.exec_module(report)


class SuiteCoverageReportTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.old = report.ROOT
        report.ROOT = Path(self.temp.name)
        for module, local, whole in [('app', 40, 70), ('salesmanager', 20, 20)]:
            path = report.ROOT / module / 'build/jacoco/testDebugUnitTest.exec'
            path.parent.mkdir(parents=True)
            path.write_bytes(b'unit-test placeholder, never passed to JaCoCo')
            for task, value in [('qaCoverage', local), ('qaAllJvmCoverage', whole)]:
                self.xml(module, task, value)

    def tearDown(self):
        report.ROOT = self.old
        self.temp.cleanup()

    def xml(self, module, task, value, name=None, total=100):
        path = report.ROOT / module / f'build/reports/jacoco/{task}/{task}.xml'
        path.parent.mkdir(parents=True, exist_ok=True)
        counts = {'LINE': (value, total), 'BRANCH': (value // 2, 100), 'METHOD': (value // 10, 10), 'CLASS': (1, 1)}
        metrics = ''.join(f'<counter type="{k}" covered="{c}" missed="{n-c}"/>' for k, (c, n) in counts.items())
        path.write_text(f'<report><package name="fixture"><class name="{name or module}"/></package>{metrics}</report>')

    def test_both_actual_streams_are_retained_and_weighted_counts_sum_once(self):
        measured = report.measured()
        self.assertEqual({'covered': 90, 'missed': 110}, measured['weighted overall']['LINE'])
        self.assertEqual(2, len(measured['executionData']))
        self.assertEqual(40, measured['modules']['app']['projectLocalJvm']['LINE']['covered'])

    def test_absent_or_empty_execution_stream_is_rejected(self):
        (report.ROOT / 'app/build/jacoco/testDebugUnitTest.exec').write_bytes(b'')
        with self.assertRaisesRegex(ValueError, 'execution data'):
            report.measured()

    def test_changed_class_membership_is_rejected(self):
        self.xml('app', 'qaAllJvmCoverage', 70, name='hidden-class')
        with self.assertRaisesRegex(ValueError, 'class set'):
            report.measured()

    def test_duplicate_production_class_across_modules_is_rejected(self):
        for task in ('qaCoverage', 'qaAllJvmCoverage'):
            self.xml('salesmanager', task, 20, name='app')
        with self.assertRaisesRegex(ValueError, 'overlapping'):
            report.measured()

    def test_reduced_denominator_is_rejected(self):
        self.xml('app', 'qaAllJvmCoverage', 70, total=90)
        with self.assertRaisesRegex(ValueError, 'denominator'):
            report.measured()

    def test_lost_project_local_coverage_is_rejected(self):
        self.xml('app', 'qaAllJvmCoverage', 30)
        with self.assertRaisesRegex(ValueError, 'lost prior'):
            report.measured()

    def test_device_mode_requires_actual_device_stream(self):
        with self.assertRaisesRegex(ValueError, 'Android execution'):
            report.measured(True)

    def test_device_merge_must_retain_coverage_from_both_jvm_suites(self):
        data = report.ROOT / 'salesmanager/build/outputs/code_coverage/device/coverage.ec'
        data.parent.mkdir(parents=True)
        data.write_bytes(b'isolated placeholder, never passed to JaCoCo')
        for module, local in [('app', 40), ('salesmanager', 20)]:
            self.xml(module, 'qaCombinedCoverage', local)
            self.xml(module, 'qaAllCombinedCoverage', local)
        with self.assertRaisesRegex(ValueError, 'lost prior'):
            report.measured(True)

    def test_class_identity_warning_is_rejected_before_publishing(self):
        log = report.ROOT / 'actual-gradle.log'
        log.write_text('Execution data for class fixture/Unit does not match.')
        with self.assertRaisesRegex(ValueError, 'Class identity mismatch'):
            report.main(['--log', str(log)])

    def test_public_metrics_cannot_swap_lines_methods_and_branches(self):
        log = report.ROOT / 'actual-gradle.log'
        log.write_text('BUILD SUCCESSFUL')
        output = io.StringIO()
        with patch.dict(os.environ, {'GITHUB_ACTIONS': 'true', 'GITHUB_STEP_SUMMARY': str(report.ROOT / 'summary.md')}), redirect_stdout(output):
            report.main(['--log', str(log)])
        notice = next(line for line in output.getvalue().splitlines() if line.startswith('::notice '))
        payload = json.loads(notice.split('::', 2)[2])
        self.assertEqual({'covered': 90, 'missed': 110}, payload['LINE']['weighted overall'])
        self.assertEqual({'covered': 9, 'missed': 11}, payload['METHOD']['weighted overall'])
        self.assertEqual({'covered': 45, 'missed': 155}, payload['BRANCH']['weighted overall'])
