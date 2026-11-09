"""Validate independent Seller/weighted floors with temporary reports; never alter real measurements."""
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / 'report-seller-coverage.py'

class CoverageTargetTest(unittest.TestCase):
    def check(self, app, seller):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / 'scripts').mkdir()
            shutil.copyfile(SCRIPT, root / 'scripts/report-seller-coverage.py')
            (root / 'docs/qa').mkdir(parents=True)
            (root / 'docs/qa/coverage-thresholds.json').write_text(json.dumps({
                'salesmanager': {'LINE': 40}, 'weighted overall': {'LINE': 40}}))
            for module, (covered, total) in [('app', app), ('salesmanager', seller)]:
                report = root / module / 'build/reports/jacoco/qaCoverage/qaCoverage.xml'
                report.parent.mkdir(parents=True)
                report.write_text(f'<report><counter type="LINE" covered="{covered}" missed="{total-covered}"/></report>')
            return subprocess.run([sys.executable, str(root / 'scripts/report-seller-coverage.py'), '--check'], capture_output=True, text=True)

    def test_uses_weighted_counters_instead_of_mean_of_module_percentages(self):
        result = self.check((0, 100), (600, 1000))
        self.assertEqual(0, result.returncode, result.stderr)

    def test_high_shared_coverage_cannot_hide_seller_below_target(self):
        result = self.check((900, 1000), (10, 100))
        self.assertNotEqual(0, result.returncode)
        self.assertIn('salesmanager/LINE', result.stderr)

    def test_seller_above_target_cannot_hide_low_weighted_coverage(self):
        result = self.check((0, 1000), (50, 100))
        self.assertNotEqual(0, result.returncode)
        self.assertIn('weighted overall/LINE', result.stderr)

    def test_exact_seller_boundary_is_accepted_when_both_metrics_pass(self):
        result = self.check((90, 100), (400, 1000))
        self.assertEqual(0, result.returncode, result.stderr)

    def test_empty_reports_do_not_claim_target(self):
        result = self.check((0, 0), (0, 0))
        self.assertNotEqual(0, result.returncode)

if __name__ == '__main__':
    unittest.main()
