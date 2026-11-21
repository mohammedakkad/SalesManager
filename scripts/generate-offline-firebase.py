#!/usr/bin/env python3
"""Packaging fixture for isolated QA jobs, never a valid Firebase credential/project."""
import json
from pathlib import Path
data={'project_info':{'project_number':'123456789','project_id':'seller-qa-offline'},'client':[]}
for package in ('com.trader.salesmanager','com.trader.salesmanager.debug'):
    data['client'].append({'client_info':{'mobilesdk_app_id':'1:123456789:android:0000000000000000','android_client_info':{'package_name':package}},'api_key':[{'current_key':'offline-test-fixture-not-a-credential'}]})
Path('salesmanager/google-services.json').write_text(json.dumps(data))
