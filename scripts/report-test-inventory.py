#!/usr/bin/env python3
"""Inventory actual JUnit cases and explicit test-plan work items; not a claim of all possible tests."""
import csv
import json
import re
from collections import Counter
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/qa/evidence/continuation/coverage100/final'


def classify(source, name):
    text = source.read_text()
    tags = []
    if 'OfflineSellerUiTest()' in text or 'OfflineNavigationFixture()' in text:
        tags.append('Compose UI on JVM')
    if re.search(r'Room\.(inMemoryDatabaseBuilder|databaseBuilder)|openOrCreateDatabase|SQLiteDatabase\.(create|open)', text):
        tags.append('Room/SQLite on JVM')
    if 'MigrationTest' in source.name:
        tags.append('Room migration')
    if 'GraphicsMode.Mode.NATIVE' in text:
        tags.append('Native bitmap graphics')
    if 'MockWebServer' in text and (source.name != 'ReleaseMetadataContractsTest.kt' or name.startswith('loopback')):
        tags.append('Loopback HTTP')
    if 'TestListenableWorkerBuilder' in text and (source.name != 'UpdateLifecycleTest.kt' or name.startswith('background')):
        tags.append('WorkManager lifecycle')
    if source.name == 'LowStockSchedulingContractsTest.kt' and name in ('repeatedSchedulingKeepsOneExistingPeriodicRequest', 'explicitReplacementCancelsTheOldRequestAndCreatesOneActiveRequest', 'cancellationCancelsTheScheduledUniqueWork'):
        tags.append('WorkManager lifecycle')
    if 'mockk' in text and ('FirebaseDatabase' in text or 'FirebaseFirestore' in text or 'FirebaseOutboxTransport' in source.name):
        tags.append('Firebase SDK/protocol fake')
    if '/backup/' in str(source):
        tags.append('Backup format/crypto')
    if source.name == 'WorkbookExportIntegrityTest.kt':
        tags.append('Real XLSX file content')
    if source.name in ('NotificationContractsTest.kt', 'ChatPushNotificationContractsTest.kt'):
        tags.append('Android notification component on JVM')
    if any(x in name.lower() for x in ('restart', 'reopen', 'restored')):
        tags.append('Restart/recovery in test harness')
    order = ['Compose UI on JVM', 'Room migration', 'Room/SQLite on JVM', 'Native bitmap graphics', 'Loopback HTTP',
             'WorkManager lifecycle', 'Firebase SDK/protocol fake', 'Backup format/crypto', 'Real XLSX file content',
             'Android notification component on JVM']
    primary = next((tag for tag in order if tag in tags), 'Unit/model with deterministic fakes')
    return primary, tags


def main():
    cases = []
    for module in ('app', 'salesmanager'):
        sources = {p.stem: p for p in (ROOT / module / 'src/test').rglob('*.kt')}
        files = sorted((ROOT / module / 'build/test-results/testDebugUnitTest').glob('TEST-*.xml'))
        if not files:
            raise ValueError(f'No executed JUnit results: {module}')
        for path in files:
            suite = ET.parse(path).getroot()
            rows = list(suite.iter('testcase'))
            if int(suite.get('tests', -1)) != len(rows):
                raise ValueError(f'JUnit count mismatch: {path}')
            for case in rows:
                source = sources[case.get('classname').split('.')[-1].split('$')[0]]
                primary, tags = classify(source, case.get('name'))
                result = next((tag for tag in ('failure', 'error', 'skipped') if case.find(tag) is not None), 'passed')
                cases.append({'module': module, 'class': case.get('classname'), 'name': case.get('name'), 'result': result,
                              'source': str(source.relative_to(ROOT)), 'primary': primary, 'tags': tags})
    identities = {(c['module'], c['class'], c['name']) for c in cases}
    if len(identities) != len(cases):
        raise ValueError('Duplicate JUnit identity')
    baseline = json.loads((ROOT / 'docs/qa/evidence/continuation/coverage70/final/jvm-cases.json').read_text())
    required = set()
    for row in baseline:
        name = row['name']
        if row['class'].endswith('.StockOperationsQualificationTest') and name == 'stockRetryFailureKeepsLedgerFailedAndSuccessAcknowledgesPendingQuantity':
            name = 'failedStockLedgerRetriesWithoutRewritingItsPendingState'
        required.add((row['module'], row['class'], name))
    if required - identities:
        raise ValueError(f'Missing prior qualified regressions: {sorted(required-identities)}')
    if any(c['result'] != 'passed' for c in cases):
        raise ValueError('Cannot issue a passing inventory from failed or skipped JVM results')
    plan_path = ROOT / 'docs/qa/remaining-test-plan.csv'
    plan = list(csv.DictReader(plan_path.open())) if plan_path.exists() else []
    summary = {'jvmCases': len(cases), 'result': dict(Counter(c['result'] for c in cases)),
               'primaryCategories': dict(Counter(c['primary'] for c in cases)),
               'overlappingTags': dict(Counter(tag for c in cases for tag in c['tags'])),
               'remainingDefinedWorkItems': len(plan), 'remainingByStatus': dict(Counter(p['status'] for p in plan)),
               'remainingByKind': dict(Counter(p['kind'] for p in plan)),
               'scope': 'Categories partition actual JVM cases; tags overlap. Remaining work items are an explicit plan, not a count of all possible tests.'}
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / 'test-inventory.json').write_text(json.dumps({'summary': summary, 'cases': cases, 'remainingPlan': plan}, indent=2, ensure_ascii=False) + '\n')
    print(json.dumps(summary, indent=2, ensure_ascii=False))


if __name__ == '__main__':
    main()
