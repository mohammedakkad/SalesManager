#!/usr/bin/env python3
"""R8 smoke lane: require MinifiedSmokeTest to be EXECUTED and PASSED in the instrumentation XML.

A green Gradle exit code is not enough: a crashed instrumentation run is reported by AGP as a
single `testRunFailed` case ("Instrumentation run failed due to Process crashed"). Missing,
skipped, failed, duplicated or crashed results are all failures here.
"""
import sys
from pathlib import Path
import xml.etree.ElementTree as ET

EXPECTED = ('com.trader.salesmanager.qa.MinifiedSmokeTest', 'realApplicationAndMainActivityStartFromTheMinifiedApk')
BAD_TAGS = ('failure', 'error', 'skipped')


def check(results_dir):
    """Return (ok, message) for the TEST-*.xml files below results_dir."""
    files = sorted(Path(results_dir).rglob('TEST-*.xml'))
    if not files:
        return False, f'No instrumentation XML found under {results_dir}: the R8 smoke test did not run.'
    seen = []
    problems = []
    for path in files:
        for case in ET.parse(path).getroot().iter('testcase'):
            identity = (case.get('classname'), case.get('name'))
            bad = [tag for tag in BAD_TAGS if case.find(tag) is not None]
            if identity == EXPECTED:
                seen.append((path, bad))
            elif bad:
                text = ' '.join((case.find(tag).get('message') or case.find(tag).text or '').strip() for tag in bad)
                problems.append(f'{identity[1]} [{",".join(bad)}] {text[:200]} ({path})')
    if problems:
        return False, 'Instrumentation did not complete cleanly: ' + ' | '.join(problems)
    if not seen:
        return False, f'MinifiedSmokeTest was not executed (expected {EXPECTED[1]}).'
    if len(seen) != 1:
        return False, f'MinifiedSmokeTest recorded {len(seen)} times, expected exactly once.'
    path, bad = seen[0]
    if bad:
        return False, f'MinifiedSmokeTest not passed ({",".join(bad)}) in {path}.'
    return True, 'MinifiedSmokeTest executed and passed.'


if __name__ == '__main__':
    ok, message = check(sys.argv[1] if len(sys.argv) > 1 else 'salesmanager/build/outputs/androidTest-results')
    print(message)
    sys.exit(0 if ok else 1)
