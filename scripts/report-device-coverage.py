#!/usr/bin/env python3
"""Report real Android+JVM execution separately; never replace the JVM denominator or floors."""
import json
import os
import re
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]


def counters(report):
    return {c.get('type'): {key: int(c.get(key)) for key in ('covered', 'missed')}
            for c in report.findall('counter') if c.get('type') in ('LINE', 'BRANCH', 'METHOD', 'CLASS')}


def measured():
    data = list((ROOT / 'salesmanager/build/outputs/code_coverage').rglob('*.ec'))
    if not data or any(p.stat().st_size == 0 for p in data):
        raise ValueError('Missing/empty real Android execution data')
    result = {'scope': 'Additional combined Android device + JVM debug coverage; original JVM floors unchanged',
              'modules': {}, 'executionData': [{'path': str(p.relative_to(ROOT)), 'bytes': p.stat().st_size} for p in data]}
    totals = {kind: {'covered': 0, 'missed': 0} for kind in ('LINE', 'BRANCH', 'METHOD', 'CLASS')}
    for module in ('app', 'salesmanager'):
        original = ET.parse(ROOT / module / 'build/reports/jacoco/qaCoverage/qaCoverage.xml').getroot()
        combined = ET.parse(ROOT / module / 'build/reports/jacoco/qaCombinedCoverage/qaCombinedCoverage.xml').getroot()
        old, new = counters(original), counters(combined)
        if old.keys() != new.keys() or set(old) != set(totals):
            raise ValueError(f'{module}: missing measured metrics')
        if {c.get('name') for c in original.iter('class')} != {c.get('name') for c in combined.iter('class')}:
            raise ValueError(f'{module}: class set changed between reports')
        for kind, values in new.items():
            if sum(values.values()) != sum(old[kind].values()) or sum(values.values()) == 0:
                raise ValueError(f'{module}/{kind}: denominator changed or empty')
            if values['covered'] < old[kind]['covered']:
                raise ValueError(f'{module}/{kind}: combined data lost JVM coverage')
            for key in values:
                totals[kind][key] += values[key]
        result['modules'][module] = {'jvm': old, 'combined': new}
        if module == 'salesmanager':
            native = {s.get('name'): counters(s).get('LINE', {}) for p in combined.findall('package')
                      if p.get('name') == 'com/trader/salesmanager/util/pdf' for s in p.findall('sourcefile')
                      if s.get('name') in ('InvoicePdfGenerator.kt', 'ReportPdfGenerator.kt', 'PdfCanvas.kt')}
            if len(native) != 3 or any(c.get('covered', 0) == 0 for c in native.values()):
                raise ValueError('Native PDF production execution not recorded; refusing coverage claim')
            result['nativePdfLines'] = native
    result['weighted overall'] = totals
    return result


def main():
    log = Path('/tmp/seller-qa-combined-coverage.log').read_text()
    if re.search(r'execution data for class [^\n]*does not match|classes in bundle[^\n]*do not match', log, re.I):
        raise ValueError('Class identity mismatch; refusing combined coverage')
    result = measured()
    Path('/tmp/seller-qa-combined-counts.json').write_text(json.dumps(result, indent=2) + '\n')
    lines = ['## Additional Android + JVM measured coverage', '',
             'Original JVM gates and class denominators remain unchanged. No production exclusions.', '',
             '| Scope | Covered lines | Total lines | Line coverage |', '| --- | ---: | ---: | ---: |']
    scopes = [(name, row['combined']['LINE']) for name, row in result['modules'].items()]
    scopes.append(('weighted overall', result['weighted overall']['LINE']))
    for name, c in scopes:
        total = c['covered'] + c['missed']
        lines.append(f"| {name} | {c['covered']} | {total} | {100*c['covered']/total:.4f}% |")
    lines += ['', 'Native PDF line counters: `' + json.dumps(result['nativePdfLines'], sort_keys=True) + '`', '']
    summary = '\n'.join(lines)
    print(summary)
    if os.environ.get('GITHUB_STEP_SUMMARY'):
        with Path(os.environ['GITHUB_STEP_SUMMARY']).open('a') as f:
            f.write(summary)


if __name__ == '__main__':
    main()
