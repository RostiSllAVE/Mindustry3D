"""Developer-only isolated diagnostic build. Never overwrites Alpha release artifacts."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import zipfile
from preflight import ROOT, run
import argparse

parser=argparse.ArgumentParser()
parser.add_argument('--task', choices=('a2-01','a2-02','a2-03','a2-04','a2-05','a2-06','a2-07','a2-08a','a2-08b','a2-09a','a2-09b','a2-09c','a2-10'), default='a2-01')
parser.add_argument('--run', default='build', help='New local task subdirectory; existing builds are never overwritten')
options=parser.parse_args()
if not options.run or any(c not in 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_' for c in options.run):
    raise SystemExit('Use a simple run name')
output = ROOT / 'local' / options.task / options.run
if output.exists():
    raise SystemExit('Diagnostic build already exists; preserve it and select a new --run name')
jdk = Path(os.environ['FPV_JDK'])
host = Path(os.environ['FPV_MINDUSTRY'])
if host.is_dir(): host = host / 'jre/desktop.jar'
jna = Path(os.environ.get('FPV_JNA', ROOT / 'lib/jna-5.19.1.jar'))
assert hashlib.sha256(jna.read_bytes()).hexdigest() == '4fb141dd8ef6b0585ffceea4bc49602fbc6312fa977e2c488794ea3e6aafecae'
run()
classes, tests, temp = (output / name for name in ('classes', 'test-classes', 'tmp'))
for path in (classes, tests, temp): path.mkdir(parents=True)
cp = str(host) + os.pathsep + str(jna)
sources = sorted((ROOT/'src/main/java').rglob('*.java')) + sorted((ROOT/'build/generated').rglob('*.java'))
argfile = output / 'compile.args'
argfile.write_text('\n'.join('"'+a.replace('\\','/')+'"' for a in ['--release','17','-encoding','UTF-8','-cp',cp,'-d',str(classes)]+[str(p) for p in sources]))
subprocess.run([str(jdk/'bin/javac.exe'),'@'+str(argfile)],check=True)
test_sources = sorted((ROOT/'src/test/java').rglob('*.java'))
subprocess.run([str(jdk/'bin/javac.exe'),'--release','17','-encoding','UTF-8','-cp',str(classes)+os.pathsep+cp,'-d',str(tests)]+[str(p) for p in test_sources],check=True)
passed=[]
for source in test_sources:
    name=source.relative_to(ROOT/'src/test/java').with_suffix('').as_posix().replace('/','.')
    subprocess.run([str(jdk/'bin/java.exe'),'-Djava.io.tmpdir='+str(temp),'-ea','-cp',str(tests)+os.pathsep+str(classes)+os.pathsep+cp,name],check=True)
    passed.append(name)
jar=output/f'mindustry-fpv-{options.task}-diagnostics.jar'
with zipfile.ZipFile(jar,'w',zipfile.ZIP_DEFLATED) as z:
    for base in (classes,ROOT/'src/main/resources'):
        for p in sorted(base.rglob('*')):
            if p.is_file(): z.write(p,p.relative_to(base).as_posix())
    for p in (ROOT/'licenses').glob('*.txt'): z.write(p,'licenses/'+p.name)
    for name in ('LICENSE','THIRD_PARTY_NOTICES.md'): z.write(ROOT/name,name)
    for p in (ROOT/'design').glob('*.sheet.json'): z.write(p,'design/'+p.name)
    with zipfile.ZipFile(jna) as dependency:
        for item in dependency.infolist():
            if not item.is_dir() and not item.filename.startswith('META-INF/'): z.writestr(item.filename,dependency.read(item))
report={'status':'UNRELEASED DIAGNOSTIC BUILD; not Alpha 1.0.4 replacement','testsPassed':len(passed),'programs':passed,'sha256':hashlib.sha256(jar.read_bytes()).hexdigest()}
(output/'build-result.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps({k:v for k,v in report.items() if k!='programs'}))
