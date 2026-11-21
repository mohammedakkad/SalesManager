"""Counter-integrity guards use isolated fixtures, never actual measured reports."""
import importlib.util
from pathlib import Path
import tempfile
import unittest
import json
import io
import os
from contextlib import redirect_stdout
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('device_report', Path(__file__).parents[1] / 'report-device-coverage.py')
report = importlib.util.module_from_spec(spec)
spec.loader.exec_module(report)


class DeviceCoverageReportTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.old_root = report.ROOT
        report.ROOT = Path(self.temp.name)
        data = report.ROOT / 'salesmanager/build/outputs/code_coverage/device/coverage.ec'
        data.parent.mkdir(parents=True)
        data.write_bytes(b'isolated execution-data placeholder; not passed to JaCoCo')
        for module in ('app', 'salesmanager'):
            for task, covered in [('qaCoverage', 70), ('qaCombinedCoverage', 80)]:
                path = report.ROOT / module / f'build/reports/jacoco/{task}/{task}.xml'
                path.parent.mkdir(parents=True)
                sources = ''.join(f'<sourcefile name="{name}"><counter type="LINE" covered="1" missed="1"/></sourcefile>'
                                  for name in ['InvoicePdfGenerator.kt', 'ReportPdfGenerator.kt', 'PdfCanvas.kt'])
                metrics = ''.join(f'<counter type="{kind}" covered="{covered}" missed="{100-covered}"/>'
                                  for kind in ['LINE', 'BRANCH', 'METHOD', 'CLASS'])
                path.write_text(f'<report><package name="com/trader/salesmanager/util/pdf"><class name="fixture"/>{sources}</package>{metrics}</report>')

    def tearDown(self):
        report.ROOT = self.old_root
        self.temp.cleanup()

    def change(self, module, old, new):
        path = report.ROOT / module / 'build/reports/jacoco/qaCombinedCoverage/qaCombinedCoverage.xml'
        path.write_text(path.read_text().replace(old, new))

    def test_preserves_weighted_denominator_and_retains_both_measurements(self):
        measured = report.measured()
        self.assertEqual({'covered': 160, 'missed': 40}, measured['weighted overall']['LINE'])
        self.assertEqual(70, measured['modules']['salesmanager']['jvm']['LINE']['covered'])

    def test_missing_real_device_file_is_rejected(self):
        for path in report.ROOT.rglob('*.ec'):
            path.unlink()
        with self.assertRaisesRegex(ValueError, 'execution data'):
            report.measured()

    def test_reduced_denominator_cannot_inflate_coverage(self):
        self.change('salesmanager', 'missed="20"', 'missed="19"')
        with self.assertRaisesRegex(ValueError, 'denominator'):
            report.measured()

    def test_class_exclusions_cannot_hide_in_combined_report(self):
        self.change('salesmanager', 'class name="fixture"', 'class name="different"')
        with self.assertRaisesRegex(ValueError, 'class set'):
            report.measured()

    def test_combined_report_cannot_lose_unit_test_coverage(self):
        self.change('salesmanager', 'covered="80" missed="20"', 'covered="60" missed="40"')
        with self.assertRaisesRegex(ValueError, 'lost JVM'):
            report.measured()

    def test_unrecorded_native_pdf_execution_is_rejected(self):
        self.change('salesmanager', 'covered="1" missed="1"', 'covered="0" missed="2"')
        with self.assertRaisesRegex(ValueError, 'Native PDF'):
            report.measured()

    def test_public_summary_exposes_line_method_and_branch_counters_without_changing_gates(self):
        actual_path = Path
        def isolated(value):
            path = actual_path(value)
            return report.ROOT / path.name if str(path).startswith('/tmp/seller-qa-') else path
        (report.ROOT / 'seller-qa-combined-coverage.log').write_text('')
        output = io.StringIO()
        with patch.object(report, 'Path', side_effect=isolated), patch.dict(os.environ, {
            'GITHUB_ACTIONS': 'true', 'SELLER_QA_ANDROID_API_ACTUAL': '28',
            'GITHUB_STEP_SUMMARY': str(report.ROOT / 'summary.md')
        }), redirect_stdout(output):
            report.main()
        notice = next(line for line in output.getvalue().splitlines()
                      if line.startswith('::notice title=Seller measured Android plus JVM coverage'))
        payload = json.loads(notice.split('::', 2)[2])
        for metric in ('lines', 'methods', 'branches'):
            self.assertEqual({'covered': 160, 'missed': 40}, payload[metric]['weighted overall'])
            self.assertEqual({'covered': 80, 'missed': 20}, payload[metric]['salesmanager'])
        self.assertEqual(70, report.measured()['modules']['salesmanager']['jvm']['LINE']['covered'])


if __name__ == '__main__':
    unittest.main()
