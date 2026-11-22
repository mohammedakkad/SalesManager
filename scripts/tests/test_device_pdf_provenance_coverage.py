"""Metadata guard fixtures use ordinary binary files; no stand-in PDFs are created."""
import hashlib
import importlib.util
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('pdf_provenance', Path(__file__).resolve().parents[1] / 'check-device-pdf-provenance.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class DeviceProvenanceGuardTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.directory = Path(self.tmp.name)
        self.path = self.directory / 'metadata-fixture.bin'
        self.path.write_bytes(b'ordinary metadata guard test payload')
        digest = hashlib.sha256(self.path.read_bytes()).hexdigest()
        self.notice = f'package={module.PACKAGE} source=/storage/emulated/0/Android/data/{module.PACKAGE}/files/qa-pdf/{self.path.name} exported=/data/local/tmp/seller-qa-pdf/{self.path.name} sha256={digest}'

    def check(self, text):
        return module.verify_provenance(text, self.directory, {self.path.name})

    def test_matching_notice_and_pulled_bytes_are_accepted(self):
        self.assertEqual(self.notice.split('sha256=')[1], self.check(self.notice)[0]['sha256'])

    def test_repeated_identical_log_lines_do_not_create_extra_artifacts(self):
        self.assertEqual(1, len(self.check(self.notice + '\n' + self.notice)))

    def test_missing_notice_cannot_qualify_a_pulled_file(self):
        with self.assertRaisesRegex(ValueError, 'Missing actual device'):
            self.check('')

    def test_modified_pulled_bytes_are_rejected(self):
        self.path.write_bytes(b'changed payload')
        with self.assertRaisesRegex(ValueError, 'Pulled bytes differ'):
            self.check(self.notice)

    def test_different_package_is_rejected(self):
        with self.assertRaisesRegex(ValueError, 'Unexpected Android provenance'):
            self.check(self.notice.replace('package=' + module.PACKAGE, 'package=another.app'))

    def test_conflicting_notices_are_rejected(self):
        second = self.notice.split('sha256=')[0] + 'sha256=' + '0' * 64
        with self.assertRaisesRegex(ValueError, 'Conflicting device hashes'):
            self.check(self.notice + '\n' + second)
