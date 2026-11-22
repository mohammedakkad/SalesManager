#!/usr/bin/env python3
"""Require the actual device tests, not merely a successful Gradle invocation."""
from pathlib import Path
import xml.etree.ElementTree as ET

from device_qa_manifest import INSTRUMENTATION
expected = INSTRUMENTATION

seen = set()
for path in Path('salesmanager/build/outputs/androidTest-results').rglob('TEST-*.xml'):
    for case in ET.parse(path).getroot().iter('testcase'):
        identity = (case.get('classname'), case.get('name'))
        if identity in expected:
            if identity in seen or any(case.find(tag) is not None for tag in ('failure', 'error', 'skipped')):
                raise SystemExit(f'Duplicate or unsuccessful device test: {identity} in {path}')
            seen.add(identity)
if seen != expected:
    raise SystemExit(f'Missing executed device tests: {sorted(expected - seen)}')
print(f'All {len(expected)} required device tests executed and passed in instrumentation XML.')
