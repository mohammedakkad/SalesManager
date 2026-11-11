#!/usr/bin/env python3
"""Additional coverage from both JVM suites, optionally Android. Established project-local gates stay unchanged."""
import argparse
import json
import os
import re
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
KINDS = ('LINE', 'METHOD', 'BRANCH', 'CLASS')


def read(module, task):
    tree = ET.parse(ROOT / module / f'build/reports/jacoco/{task}/{task}.xml').getroot()
    return {c.get('name') for c in tree.iter('class')}, {
        c.get('type'): {k: int(c.get(k)) for k in ('covered', 'missed')}
        for c in tree.findall('counter') if c.get('type') in KINDS}


def measured(device=False):
    data = [ROOT / m / 'build/jacoco/testDebugUnitTest.exec' for m in ('app', 'salesmanager')]
    if any(not p.is_file() or not p.stat().st_size for p in data):
        raise ValueError('Missing actual JVM execution data from either suite')
    if device:
        android = list((ROOT / 'salesmanager/build/outputs/code_coverage').rglob('*.ec'))
        if not android or any(not p.stat().st_size for p in android):
            raise ValueError('Missing actual Android execution data')
        data += android
    totals = {kind: {'covered': 0, 'missed': 0} for kind in KINDS}
    result = {'scope': 'Both JVM suites' + (' plus actual Android' if device else ''),
              'executionData': [{'path': str(p.relative_to(ROOT)), 'bytes': p.stat().st_size} for p in data], 'modules': {}}
    previous_classes = set()
    for module in ('app', 'salesmanager'):
        classes, original = read(module, 'qaCoverage')
        merged_classes, merged = read(module, 'qaAllCombinedCoverage' if device else 'qaAllJvmCoverage')
        if classes != merged_classes or previous_classes & classes:
            raise ValueError(f'{module}: changed or overlapping production class set')
        previous_classes |= classes
        comparisons = [original]
        if device:
            for task in ('qaCombinedCoverage', 'qaAllJvmCoverage'):
                additional_classes, additional = read(module, task)
                if additional_classes != classes:
                    raise ValueError(f'{module}: changed production class set')
                comparisons.append(additional)
        for baseline in comparisons:
            if set(merged) != set(KINDS) or set(baseline) != set(KINDS):
                raise ValueError(f'{module}: missing counters')
            for kind in KINDS:
                if sum(merged[kind].values()) != sum(baseline[kind].values()) or sum(merged[kind].values()) <= 0:
                    raise ValueError(f'{module}/{kind}: changed or empty denominator')
                if merged[kind]['covered'] < baseline[kind]['covered']:
                    raise ValueError(f'{module}/{kind}: lost prior measured coverage')
        result['modules'][module] = {'projectLocalJvm': original, 'allSuites': merged}
        for kind in KINDS:
            for key in totals[kind]:
                totals[kind][key] += merged[kind][key]
    result['weighted overall'] = totals
    return result


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--device', action='store_true')
    parser.add_argument('--log', required=True, type=Path)
    args = parser.parse_args(argv)
    log = args.log.read_text()
    if re.search(r'execution data for class [^\n]*does not match|classes in bundle[^\n]*do not match', log, re.I):
        raise ValueError('Class identity mismatch; refusing suite-wide coverage')
    result = measured(args.device)
    label = 'all-combined' if args.device else 'all-jvm'
    output = ROOT / f'salesmanager/build/reports/jacoco/suite-{label}-summary.json'
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, indent=2) + '\n')
    lines = ['## Additional whole-suite coverage', '', result['scope'] + '; original reports and gates unchanged.', '',
             '| Scope | Metric | Covered | Total | Coverage |', '| --- | --- | ---: | ---: | ---: |']
    published = {}
    for kind in ('LINE', 'METHOD', 'BRANCH'):
        rows = [(name, value['allSuites'][kind]) for name, value in result['modules'].items()]
        rows.append(('weighted overall', result['weighted overall'][kind]))
        published[kind] = dict(rows)
        for name, value in rows:
            total = sum(value.values())
            lines.append(f"| {name} | {kind} | {value['covered']} | {total} | {100*value['covered']/total:.4f}% |")
    summary = '\n'.join(lines) + '\n'
    print(summary)
    if os.environ.get('GITHUB_ACTIONS') == 'true':
        api = os.environ.get('SELLER_QA_ANDROID_API_ACTUAL')
        suffix = f' [API {api}]' if args.device and api else ''
        print(f'::notice title=Seller measured {label} suite coverage{suffix}::' + json.dumps(published, sort_keys=True).replace('%', '%25'))
    if os.environ.get('GITHUB_STEP_SUMMARY'):
        with open(os.environ['GITHUB_STEP_SUMMARY'], 'a') as stream:
            stream.write(summary)


if __name__ == '__main__':
    main()
