"""Serial isolated standalone GPU checks. Explicit fixture overlays are never player artifacts."""
import argparse,json,subprocess,sys,time
from pathlib import Path
from dev_runtime import ROOT

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--jar',required=True,type=Path)
    p.add_argument('--java',choices=['java17','bundled25'],required=True);p.add_argument('--run',required=True)
    p.add_argument('--performance',action='store_true');a=p.parse_args()
    if not a.run.replace('-','').isalnum():p.error('New simple run name required')
    summary=ROOT/'local/a2-18'/(a.run+'.json')
    if summary.exists():p.error('Existing evidence is preserved')
    tests=[('performance','ReleasePerformanceHarness.java',[],[])] if a.performance else [
        ('repair-arrows','RepairArrowVisualHarness.java',[],[]),
        ('logic','StandaloneSmokeHarness.java',['LogicDisplayGpuChecks.java'],[]),
        ('mountains','MountainStylesHarness.java',[],[]),
        ('fog','MountainFogHarness.java',[],[]),
        ('aircraft','FlyingExhaustHarness.java',[],[]),
        ('utilities','SandboxVisualHarness.java',['SandboxGpuChecks.java'],['fpv.utilityGpu=true']),
        ('turrets','TurretFlightPolishHarness.java',['TurretPolishGpuChecks.java','TurretPolishGeometryProbe.java'],['fpv.polishGpu=true','fpv.polishGeometry=true'])]
    results=[]
    for label,fixture,helpers,props in tests:
        run=a.run+'-'+label;out=ROOT/'local/standalone-runs'/run
        command=[sys.executable,str(ROOT/'tools/launch-standalone.py'),'--run',run,'--jar',str(a.jar.resolve()),'--java',a.java,'--fixture',str(ROOT/'tools/java'/fixture)]
        for helper in helpers:command+=['--helper',str(ROOT/'tools/java'/helper)]
        for prop in props:command+=['--property',prop]
        launch=subprocess.run(command,cwd=ROOT,capture_output=True,text=True)
        if launch.returncode:raise RuntimeError(launch.stdout+launch.stderr)
        deadline=time.monotonic()+300
        while not (out/'result.json').exists():
            if (out/'failed.txt').exists():raise RuntimeError(str(out)+': '+(out/'failed.txt').read_text())
            if time.monotonic()>deadline:raise TimeoutError(str(out))
            time.sleep(1)
        result=json.loads((out/'result.json').read_text());results.append({'name':label,'directory':str(out),'result':result})
        if result.get('status')!='PASS':raise RuntimeError(result)
        # Allow the game to finish its requested normal exit before starting the next GPU workload.
        time.sleep(2);print(label+': PASS',flush=True)
    report={'status':'PASS','runtime':a.java,'fixtureOverlays':True,'physicalAcceptance':False,'results':results}
    summary.write_bytes((json.dumps(report,indent=2)+'\n').encode());print(str(summary))
if __name__=='__main__':main()
