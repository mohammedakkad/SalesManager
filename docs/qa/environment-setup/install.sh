#!/usr/bin/env bash
set -euo pipefail
mkdir -p /workspace/setup /workspace/toolchains
cd /workspace/setup
curl -fsSL https://services.gradle.org/distributions/gradle-8.9-bin.zip.sha256 -o gradle.sha256
if [ ! -f gradle.zip ]; then
  curl -fsSL https://services.gradle.org/distributions/gradle-8.9-bin.zip -o gradle.zip
fi
python3 - <<'PY'
import pathlib,hashlib,zipfile
p=pathlib.Path('/workspace/setup')
assert hashlib.sha256((p/'gradle.zip').read_bytes()).hexdigest()==(p/'gradle.sha256').read_text().strip(), 'Gradle checksum mismatch'
if not pathlib.Path('/workspace/toolchains/gradle-8.9/bin/gradle').is_file():
 zipfile.ZipFile(p/'gradle.zip').extractall('/workspace/toolchains')
for f in pathlib.Path('/workspace/toolchains/gradle-8.9/bin').iterdir(): f.chmod(0o755)
PY
if [ ! -x /workspace/toolchains/android-sdk/cmdline-tools/latest/bin/sdkmanager ]; then
 curl -fsSL https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip -o android-tools.zip
 python3 - <<'PY'
import pathlib,hashlib,zipfile
p=pathlib.Path('/workspace/setup/android-tools.zip')
assert hashlib.sha1(p.read_bytes()).hexdigest()=='e025545c62a8e64c7559119566a569fb1dec5f60', 'Android tools checksum mismatch (official repository metadata)'
out=pathlib.Path('/workspace/toolchains/android-sdk/cmdline-tools/latest')
with zipfile.ZipFile(p) as z:
 for i in z.infolist():
  if i.is_dir(): continue
  f=out/i.filename.removeprefix('cmdline-tools/')
  f.parent.mkdir(parents=True,exist_ok=True); f.write_bytes(z.read(i)); f.chmod((i.external_attr>>16)&0o777 or 0o644)
PY
fi
sdk=/workspace/toolchains/android-sdk
printf '%s\n' y y y y y y y y y y | "$sdk/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$sdk" 'platforms;android-35' 'build-tools;34.0.0' 'platform-tools'
/workspace/toolchains/gradle-8.9/bin/gradle --version
"$sdk/platform-tools/adb" version
# Preserve an existing Firebase file. Never print configuration contents.
python3 - <<'PY'
import os,pathlib,json
p=pathlib.Path('/workspace/SalesManager/app/google-services.json')
v=os.environ.get('GOOGLE_SERVICES_JSON')
if not p.exists() and v:
 data=json.loads(v)
 assert any(c.get('client_info',{}).get('android_client_info',{}).get('package_name')=='com.trader.salesmanager.debug' for c in data.get('client',[])), 'Firebase config needs a debug application client'
 fd=os.open(p,os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
 with os.fdopen(fd,'w') as f: f.write(v)
if not p.exists(): print('Firebase configuration missing: provide app/google-services.json for com.trader.salesmanager.debug before full validation.')
PY

cat > /workspace/setup/gradle <<'CODEX_GRADLE_WRAPPER'
#!/usr/bin/env python3
import os,sys
from urllib.parse import urlparse
os.environ['ANDROID_HOME']='/workspace/toolchains/android-sdk'
os.environ['ANDROID_SDK_ROOT']=os.environ['ANDROID_HOME']
args=['/workspace/toolchains/gradle-8.9/bin/gradle']
p=urlparse(os.environ.get('HTTPS_PROXY',''))
if p.hostname:
 for scheme in ('https','http'):
  args.extend([f'-D{scheme}.proxyHost={p.hostname}',f'-D{scheme}.proxyPort={p.port or 8080}'])
os.execv(args[0],args+sys.argv[1:])
CODEX_GRADLE_WRAPPER
chmod +x /workspace/setup/gradle


# Full JDK 17 is required by the audited branch's Robolectric tests.
if [ ! -x /workspace/toolchains/jdk-17.0.17+10/bin/javac ]; then
  curl -fsSL 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.17%2B10/OpenJDK17U-jdk_x64_linux_hotspot_17.0.17_10.tar.gz' -o /workspace/setup/jdk17.tar.gz
fi
python3 - <<'VERIFY_JDK'
from pathlib import Path
import hashlib,tarfile
archive=Path('/workspace/setup/jdk17.tar.gz')
expected='992f96e7995075ac7636bb1a8de52b0c61d71ed3137fafc979ab96b4ab78dd75'
assert hashlib.sha256(archive.read_bytes()).hexdigest()==expected, 'JDK17 checksum mismatch'
if not Path('/workspace/toolchains/jdk-17.0.17+10/bin/javac').is_file():
 with tarfile.open(archive) as source: source.extractall('/workspace/toolchains',filter='data')
VERIFY_JDK
/workspace/toolchains/jdk-17.0.17+10/bin/javac -version
mkdir -p /workspace/setup/continuation
cat > /workspace/setup/continuation/gradle <<'AUDIT_GRADLE'
#!/usr/bin/env bash
set -euo pipefail
export JAVA_HOME='/workspace/toolchains/jdk-17.0.17+10'
exec /workspace/setup/gradle -Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts -Dorg.gradle.jvmargs='-Xmx6g -Dfile.encoding=UTF-8' -Pkotlin.compiler.execution.strategy=in-process --configure-on-demand --no-configuration-cache --no-daemon --max-workers=4 "$@"
AUDIT_GRADLE
chmod +x /workspace/setup/continuation/gradle
