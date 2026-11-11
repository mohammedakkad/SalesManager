#!/usr/bin/env python3
"""Summarize actual JaCoCo counters and enumerate gaps; never fabricate a baseline."""
from pathlib import Path
import csv
import json
import sys
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
output = root / 'docs/qa'
output.mkdir(parents=True, exist_ok=True)
rows = []
gaps = []
class_gaps = []
total = {}
for module in ('app', 'salesmanager'):
    report = root / module / 'build/reports/jacoco/qaCoverage/qaCoverage.xml'
    if not report.is_file():
        raise SystemExit(f'Missing measured report: {report}')
    tree = ET.parse(report).getroot()
    for counter in tree.findall('counter'):
        kind = counter.attrib['type']
        missed, covered = int(counter.attrib['missed']), int(counter.attrib['covered'])
        if kind in ('LINE', 'BRANCH', 'METHOD', 'CLASS'):
            rows.append((module, kind, covered, missed))
            old = total.get(kind, (0, 0))
            total[kind] = (old[0] + covered, old[1] + missed)
    for package in tree.findall('package'):
        for cls in package.findall('class'):
            counters = {c.get('type'): c for c in cls.findall('counter')}
            if any(int(c.get('missed', '0')) for c in counters.values()):
                class_gaps.append([module, cls.get('name')] + [counters[k].get(part, '0') if k in counters else '0' for k in ('LINE', 'BRANCH', 'METHOD', 'CLASS') for part in ('covered', 'missed')])
        for source in package.findall('sourcefile'):
            missed_lines = [line.attrib['nr'] for line in source.findall('line') if int(line.attrib['mi']) > 0]
            missed_branches = [f"{line.attrib['nr']}:{line.attrib['mb']}" for line in source.findall('line') if int(line.attrib['mb']) > 0]
            if missed_lines or missed_branches:
                gaps.append((module, package.attrib['name'] + '/' + source.attrib['name'], ' '.join(missed_lines), ' '.join(missed_branches)))
with (output / 'seller-coverage-gaps.csv').open('w', newline='') as f:
    writer = csv.writer(f, lineterminator="\n")
    writer.writerow(['module', 'source', 'lines_with_missed_instructions', 'line:missed_branch_count'])
    writer.writerows(gaps)
with (output / 'seller-class-coverage-gaps.csv').open('w', newline='') as f:
    writer = csv.writer(f, lineterminator="\n")
    writer.writerow(['module', 'class'] + [k + '_' + part for k in ('line', 'branch', 'method', 'class') for part in ('covered', 'missed')])
    writer.writerows(class_gaps)
s = '# Seller measured coverage\n\nDebug JVM tests only. Not device/instrumentation or minified release coverage. Counts include compiled production classes, generated classes and compiler-generated branches; no production exclusions. Partial lines count as covered in JaCoCo line totals, while the gap CSV also identifies lines with any missed instructions.\n\n| Module | Metric | Covered | Missed | Coverage |\n| --- | --- | ---: | ---: | ---: |\n'
for module, kind, covered, missed in rows + [('weighted overall', k, c, m) for k, (c, m) in total.items()]:
    percent = f'{100 * covered / (covered + missed):.4f}%' if covered + missed else 'N/A'
    s += f'| {module} | {kind} | {covered} | {missed} | {percent} |\n'
s += '\nExact class counters: seller-class-coverage-gaps.csv. Exact source line and branch gaps: seller-coverage-gaps.csv (branch counts per line; no true/false labels in JaCoCo XML). HTML and XML: each module build/reports/jacoco/qaCoverage/. No claim of 100% coverage or absence of defects. Initial production baseline had no tests/reports and was unmeasured; no verified numeric baseline was reduced. Reviewed CI floors in coverage-thresholds.json are based on measured module baselines with a small fixed margin for scheduled/generated paths; they are raised explicitly, never rewritten automatically by a test run.\n\nRemaining coverage: repository/DAO/migration faults, activation, sync conflicts/process interruption, cash/stock atomicity, backup restore/PDF, every Compose screen and navigation journey, workers/update behavior and release runtime. Full component inventory and test strategy identify the required continuation.\n'
(output / 'seller-coverage-report.md').write_text(s)
print(s)

if '--check' in sys.argv:
    thresholds = json.loads((output / 'coverage-thresholds.json').read_text())
    measured_rows = rows + [('weighted overall', k, c, m) for k, (c, m) in total.items()]
    violations = []
    for module, metrics in thresholds.items():
        for metric, minimum in metrics.items():
            matches = [(c, m) for mod, kind, c, m in measured_rows if mod == module and kind == metric]
            if len(matches) != 1:
                violations.append(f'{module}/{metric}: missing measured counter')
                continue
            c, m = matches[0]
            measured = 100 * c / (c + m) if c + m else 0
            if measured < minimum:
                violations.append(f'{module}/{metric}: {measured:.4f}% < {minimum}%')
    if violations:
        raise SystemExit('Coverage gate failed: ' + '; '.join(violations))
    print('Measured baseline coverage gates passed.')
