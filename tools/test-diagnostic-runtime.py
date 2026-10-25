"""Run every compiled regression against an explicitly selected standalone Java runtime."""
import argparse,json,os,subprocess,time
from pathlib import Path
from dev_runtime import ROOT,host_jar,runtime,inspect

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--build',type=Path,required=True);p.add_argument('--jar',type=Path,help='Explicit production JAR; otherwise discover the diagnostic JAR');p.add_argument('--java',choices=['java17','bundled25'],required=True);p.add_argument('--run',required=True);a=p.parse_args()
 if not a.run.replace('-','').isalnum():p.error('Use a new simple run name')
 build=a.build.resolve();result=build/(a.run+'-tests.json');logfile=build/(a.run+'-tests.log')
 if result.exists() or logfile.exists():p.error('Existing evidence is preserved; choose a new run')
 jars=[a.jar.resolve()] if a.jar else list(build.glob('mindustry-fpv-*-diagnostics.jar'))
 if len(jars)!=1 or not jars[0].is_file():p.error('Expected exactly one built diagnostic JAR')
 (build/'tmp').mkdir(exist_ok=True)
 java=runtime(a.java);cp=os.pathsep.join(map(str,[build/'test-classes',jars[0],host_jar(),ROOT/'lib/jna-5.19.1.jar']))
 results=[]
 with logfile.open('x',encoding='utf-8') as log:
  for src in sorted((ROOT/'src/test/java').rglob('*.java')):
   name=src.relative_to(ROOT/'src/test/java').with_suffix('').as_posix().replace('/','.');start=time.monotonic()
   flags=['--enable-native-access=ALL-UNNAMED'] if a.java=='bundled25' else []
   if a.java=='bundled25' and 'java.logging' not in inspect()['modules']:flags.append('-Dfpv.test.expectNoLogging=true')
   test=subprocess.run([str(java),*flags,'-Djava.io.tmpdir='+str(build/'tmp'),'-ea','-cp',cp,name],capture_output=True,text=True,encoding='utf-8',errors='replace')
   log.write('\n### '+name+'\n'+test.stdout+test.stderr);log.flush();results.append({'program':name,'exitCode':test.returncode,'seconds':round(time.monotonic()-start,3)})
 report={'runtime':str(java),'game':str(host_jar()),'passed':sum(r['exitCode']==0 for r in results),'failed':sum(r['exitCode']!=0 for r in results),'results':results};result.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({k:v for k,v in report.items() if k!='results'}));raise SystemExit(report['failed']!=0)
if __name__=='__main__':main()
