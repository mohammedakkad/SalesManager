#!/usr/bin/env python3
"""Verify device path notices against the actual pulled files, never substitute PDFs."""
import hashlib
import json
from pathlib import Path
import re

EXPECTED = {'invoice-content.pdf', 'invoice-long.pdf', 'customer-statement.pdf',
            'monthly-report.pdf', 'invoice-return-viewmodel.pdf', 'customer-statement-long.pdf',
            'invoice-manual-bank.pdf', 'invoice-full-return.pdf', 'invoice-fractional-logo.pdf',
            'monthly-report-empty.pdf'}
PACKAGE = 'com.trader.salesmanager.debug'


def verify_provenance(text, directory, expected=EXPECTED):
    rows = {}
    for match in re.finditer(r'package=(\S+) source=(\S+) exported=(\S+) sha256=([a-f0-9]{64})', text):
        package, source, exported, digest = match.groups()
        name = Path(exported).name
        if name not in expected:
            continue
        if package != PACKAGE or source != f'/storage/emulated/0/Android/data/{PACKAGE}/files/qa-pdf/{name}' or exported != f'/data/local/tmp/seller-qa-pdf/{name}':
            raise ValueError(f'Unexpected Android provenance for {name}')
        row = {'package': package, 'source': source, 'exported': exported, 'sha256': digest}
        if name in rows and rows[name] != row:
            raise ValueError(f'Conflicting device hashes for {name}')
        rows[name] = row
    if rows.keys() != expected:
        raise ValueError('Missing actual device path/hash notices: ' + ', '.join(sorted(expected - rows.keys())))
    for name, row in rows.items():
        path = directory / name
        if not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest() != row['sha256']:
            raise ValueError(f'Pulled bytes differ from device hash for {name}')
    return [rows[name] for name in sorted(rows)]


if __name__ == '__main__':
    directory = Path('/tmp/seller-qa-pdf')
    rows = verify_provenance((directory / 'device-paths.log').read_text(), directory)
    (directory / 'verified-device-provenance.json').write_text(json.dumps(rows, indent=2) + '\n')
    print('PASS: ten Android source/export paths match SHA256 of the actual pulled PDFs')
