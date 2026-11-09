"""Bounded seeded-fault sensitivity experiment, not a comprehensive mutation score.
Runs only in a disposable git archive: never edits the working repository's production code.
"""
import hashlib,io,json,shutil,subprocess,sys,tarfile,time,xml.etree.ElementTree as E
from pathlib import Path
repo=Path('/workspace/SalesManager')
head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=repo,text=True).strip()
work=Path('/workspace/.salesmanager-tools')/f'sale-mutation-{head[:7]}-{int(time.time())}'
work.mkdir()
out=repo/'docs/qa/evidence/continuation/coverage100/sale-mutation-sample'
out.mkdir(exist_ok=True)
archive=subprocess.check_output(['git','archive',head],cwd=repo)
with tarfile.open(fileobj=io.BytesIO(archive)) as source_archive:source_archive.extractall(work,filter='data')
subprocess.run(['python3','scripts/generate-offline-firebase.py'],cwd=work,check=True)
source=work/'salesmanager/src/main/java/com/trader/salesmanager/sales/SaleCommands.kt'
original=source.read_text()
helper='/workspace/.salesmanager-tools/stage10-gradle.sh'
mutants=[
 ('M01','Omit persisted invoice lines','        db.invoiceItemDao().insertAll(lines)\n','', ['successfulCreation']),
 ('M02','Reverse sale inventory effect','lines.forEach { stock(it, -it.quantity, id, merchantId) }','lines.forEach { stock(it, it.quantity, id, merchantId) }',['successfulCreation']),
 ('M03','Omit sale cash effect','            cash(null, sale.copy(id = id), merchantId)\n','',['successfulCreation']),
 ('M04','Allow negative remaining stock','        require(next.isFinite() && next >= 0) { "Insufficient stock" }\n','',['concurrentLastStockAllowsOnlyOneSale']),
 ('M05','Accept a changed request under an already used operation identifier','            check(it.kind == kind && it.requestHash == hash) { "Operation ID reused for another request" }\n','',['operationIdCannotBeReusedWithDifferentPayload']),
 ('M06','Accept stale editing snapshot','        check(sameSale(current.toDomain(), expected)) { "Sale changed; reload before editing" }\n','',['staleEditIsRejectedAndReplayCannotOverwriteLaterEdit']),
 ('M07','Restore already returned stock again on deletion','        remaining(lines, returned).forEach { line ->','        lines.forEach { line ->',['partialReturnDeletionRestoresOnlyRemainingStockAndCash','fullReturnDeletionDoesNotRestoreOrRefundTwice']),
 ('M08','Omit durable transactional outbox insertion','''        db.saleOutboxDao().insert(SaleOutboxEntity(eventId = UUID.randomUUID().toString(), operationId = op,
            merchantId = merchant, aggregateId = id, aggregateRevision = db.saleOutboxDao().revision(merchant, id) + 1,
            eventType = kind, payloadVersion = 1, payload = Gson().toJson(payload), createdAt = now, updatedAt = now))\n''','',['commitCreatesExactlyOneOutboxEventAndReplayDoesNotDuplicateIt','outboxInsertFailureRollsBackSaleStockCashAndReceipt']),
]
for ident,description,old,new,names in mutants:
    assert original.count(old)==1,(ident,'Mutation anchor not unique or not present')
case_class='com.trader.salesmanager.sales.SaleCommandsTest'
def execute(label,names):
    log=out/f'{label}.log'
    args=['bash',helper,'-p',str(work),':salesmanager:testDebugUnitTest']
    for name in names:args+=['--tests',case_class+'.'+name]
    started=time.time()
    with log.open('w') as stream:
        process=subprocess.run(args,cwd=work,stdout=stream,stderr=subprocess.STDOUT,timeout=600)
    text=log.read_text()
    xml=work/'salesmanager/build/test-results/testDebugUnitTest'/f'TEST-{case_class}.xml'
    result={'label':label,'exitCode':process.returncode,'seconds':round(time.time()-started,2),'selectedTests':names}
    if xml.exists() and xml.stat().st_mtime>=started-2 and '> Task :salesmanager:testDebugUnitTest' in text:
        suite=E.parse(xml).getroot()
        cases=list(suite.findall('testcase'))
        if {case.get('name') for case in cases}==set(names):
            shutil.copy2(xml,out/f'{label}.xml')
            result['counts']={key:int(suite.get(key,0)) for key in ('tests','failures','errors','skipped')}
            result['failures']=[{'test':c.get('name'),'type':f.get('type'),'message':f.get('message','')[:1500]}
                                for c in cases for f in list(c.findall('failure'))+list(c.findall('error'))]
    return result
result={'source':head,'productionSourceSha256':hashlib.sha256(original.encode()).hexdigest(),
        'scope':'Eight preselected local fault injections. No production changes. No comprehensive mutation score or coverage increase is claimed.',
        'archiveWorkspace':str(work),'mutants':[]}
all_names=sorted(set(name for mutant in mutants for name in mutant[-1]))
result['baseline']=execute('baseline',all_names)
assert result['baseline']['exitCode']==0 and result['baseline'].get('counts',{}).get('tests')==len(all_names),result['baseline']
print('Baseline selected real Room tests PASS: '+str(len(all_names)),flush=True)
try:
    for ident,description,old,new,names in mutants:
        source.write_text(original.replace(old,new))
        row=execute(ident,names)
        row.update({'description':description,'old':old,'new':new})
        failed=row.get('counts',{}).get('failures',0)+row.get('counts',{}).get('errors',0)
        row['result']='KILLED' if row['exitCode']!=0 and failed and '> Task :salesmanager:testDebugUnitTest FAILED' in (out/f'{ident}.log').read_text() else ('SURVIVED' if row['exitCode']==0 and row.get('counts',{}).get('tests')==len(names) else 'INVALID OR BLOCKED')
        result['mutants'].append(row)
        (out/'results.json').write_text(json.dumps(result,indent=2)+'\n')
        print(json.dumps({'mutant':ident,'result':row['result'],'counts':row.get('counts'),'seconds':row['seconds']}),flush=True)
        if row['result']=='INVALID OR BLOCKED':break
finally:
    source.write_text(original)
    result['isolatedSourceRestored']=source.read_text()==original
    assert (repo/'salesmanager/src/main/java/com/trader/salesmanager/sales/SaleCommands.kt').read_text()==original,'Working repository unexpectedly changed'
    (out/'results.json').write_text(json.dumps(result,indent=2)+'\n')
