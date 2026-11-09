#!/usr/bin/env python3
"""Expose actual named device/PDFBox XML outcomes, including failures, without requiring log/artifact download."""
import json
import os
from pathlib import Path
import sys
import xml.etree.ElementTree as ET
from device_qa_manifest import INSTRUMENTATION, PDFBOX

root = Path(__file__).resolve().parents[1]
rows = []
for group, directory, expected in (
    ('Android instrumentation', root / 'salesmanager/build/outputs/androidTest-results', INSTRUMENTATION),
    ('PDFBox on Android-generated files', root / 'qa/pdf-verifier/build/test-results', PDFBOX),
):
    seen = {}
    for path in directory.rglob('TEST-*.xml'):
        for case in ET.parse(path).getroot().iter('testcase'):
            identity = (case.get('classname'), case.get('name'))
            if identity in expected:
                status = next((tag.upper() for tag in ('failure', 'error', 'skipped') if case.find(tag) is not None), 'PASS')
                if identity in seen:
                    status = 'DUPLICATE'
                details = ' '.join(case.find(tag).get('message', '') for tag in ('failure', 'error', 'skipped') if case.find(tag) is not None)
                seen[identity] = {'group': group, 'class': identity[0], 'test': identity[1], 'status': status, 'details': details[:1500]}
    for identity in sorted(expected):
        rows.append(seen.get(identity, {'group': group, 'class': identity[0], 'test': identity[1], 'status': 'NOT EXECUTED', 'details': ''}))
result = {'source': os.environ.get('GITHUB_SHA', 'local'), 'scope': 'Actual JUnit XML; missing/skipped/duplicate is never success', 'cases': rows}
Path('/tmp/seller-qa-device-test-summary.json').write_text(json.dumps(result, indent=2) + '\n')
lines = ['## Seller actual device and PDFBox runtime cases', '', '| Group | Test | Result |', '| --- | --- | --- |']
for row in rows:
    lines.append(f"| {row['group']} | {row['test']} | {row['status']} |")
    if row['details']:
        lines.append('\nFailure detail: `' + row['details'].replace('`', "'").replace('\n', ' ') + '`\n')
summary = '\n'.join(lines) + '\n'
print(summary)
if '--annotation' in sys.argv and os.environ.get('GITHUB_ACTIONS') == 'true':
    # Public annotations expose actual XML outcomes when authenticated logs/artifacts
    # and job summaries are unavailable; never infer success from compilation.
    notice = json.dumps([{'test': row['test'], 'result': row['status'], 'detail': row['details'][:300]}
                         for row in rows], ensure_ascii=False)
    notice = notice.replace('%', '%25').replace('\r', '%0D').replace('\n', '%0A')
    print('::notice title=Seller actual device and PDFBox XML::' + notice)
if os.environ.get('GITHUB_STEP_SUMMARY'):
    path = Path(os.environ['GITHUB_STEP_SUMMARY'])
    begin, end = '<!-- seller-qa-runtime-start -->', '<!-- seller-qa-runtime-end -->'
    previous = path.read_text() if path.exists() else ''
    block = begin + '\n' + summary + end + '\n'
    if begin in previous and end in previous:
        prefix, rest = previous.split(begin, 1)
        _, suffix = rest.split(end, 1)
        path.write_text(prefix + block + suffix)
    else:
        with path.open('a') as stream:
            stream.write(block)
if '--check' in sys.argv and any(row['status'] != 'PASS' for row in rows):
    raise SystemExit('Required runtime XML cases incomplete or unsuccessful')
