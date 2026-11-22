#!/usr/bin/env python3
"""Reject tracked private signing/server credentials and FCM token logging; never print values."""
from pathlib import Path
import json
import re
import subprocess
import sys
root=Path(__file__).resolve().parents[1]
paths=subprocess.check_output(['git','ls-files','-z'],cwd=root).decode().split('\0')
failures=[]
for name in filter(None,paths):
    if name.startswith('admin/'):
        continue  # Admin business/configuration is outside this Seller change.
    p=root/name
    if not p.is_file():
        continue
    if p.suffix.lower() in ('.jks','.keystore','.p12','.pfx') or p.name=='keystore_base64.txt':
        failures.append(f'{name}: tracked signing material')
        continue
    if p.suffix.lower() not in ('.json','.kt','.java','.pem','.key','.env'):
        continue
    raw=p.read_text(errors='replace')
    if re.search(r'-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----',raw):
        failures.append(f'{name}: private key marker')
    if p.suffix=='.json':
        try:
            obj=json.loads(raw)
            if isinstance(obj,dict) and (obj.get('type')=='service_account' or 'private_key' in obj):
                failures.append(f'{name}: server-account credential structure')
        except json.JSONDecodeError:
            pass
    if p.name=='SalesManagerApp.kt' and re.search(r'Log\.[vdiew]\([^\n]*\$token',raw):
        failures.append(f'{name}: FCM registration token logging')
if failures:
    print('\n'.join(failures))
    sys.exit(1)
print('Seller tracked-signing/server-credential and token-log policy: PASS (not a complete secret scan).')
