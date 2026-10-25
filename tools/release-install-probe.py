"""Local standalone native import + real restart of an unmodified production JAR/folder.
The observer contains no mod classes; no Steam calls or smoke-mode flags are used.
"""
import argparse,hashlib,json,os,shutil,subprocess,zipfile
from pathlib import Path
from dev_runtime import ROOT,host_jar,jdk_home,java_tool,runtime,inspect
from distribution import jar_entries,payload_hash

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--jar',type=Path,required=True);p.add_argument('--workshop',type=Path)
    p.add_argument('--run',required=True);p.add_argument('--java',choices=['java17','bundled25'],required=True)
    a=p.parse_args()
    if not a.run.replace('-','').isalnum():p.error('Use a new simple run ID')
    out=ROOT/'local/a2-18'/a.run
    if out.exists():p.error('Existing evidence is preserved')
    profile=out/'data';classes=out/'observer';results=out/'results'
    for d in [profile/'mods',classes,results,out/'tmp']:d.mkdir(parents=True)
    entries=jar_entries(a.jar);identity=payload_hash(entries)
    import re
    version=re.search(r'^version:\s*(\S+)',entries['mod.hjson'].decode(),re.M).group(1)
    subprocess.run([str(java_tool(jdk_home(),'javac')),'--release','17','-cp',str(host_jar()),'-d',str(classes),str(ROOT/'tools/java/DistributionImportProbe.java'),str(ROOT/'tools/java/ReleaseStartupProbe.java')],check=True)
    assert all(not x.relative_to(classes).as_posix().startswith('fpv/') for x in classes.rglob('*.class'))
    env=dict(os.environ,APPDATA=str(out/'appdata'),LOCALAPPDATA=str(out/'localappdata'),TEMP=str(out/'tmp'),TMP=str(out/'tmp'))
    java=runtime(a.java);common=[str(java)]+(['--enable-native-access=ALL-UNNAMED'] if a.java=='bundled25' else [])+['-Dnodiscord=true','-Djava.io.tmpdir='+str(out/'tmp'),'-Dmindustry.data.dir='+str(profile),'-Dprobe.profile='+str(profile),'-Dprobe.version='+version]
    cp=str(host_jar())+os.pathsep+str(classes)
    flags=getattr(subprocess,'CREATE_NO_WINDOW',0)
    def launch(kind,args):
        with (results/(kind+'.log')).open('wb') as log:
            subprocess.run(common+args,cwd=out,env=env,stdout=log,stderr=log,creationflags=flags,timeout=180,check=True)
        result=json.loads((results/(kind+'.json')).read_text())
        if result['status']!='PASS':raise RuntimeError(result)
        return result
    if a.workshop:shutil.copytree(a.workshop,profile/'mods/Mindustry3')
    else:launch('import',['-Dprobe.jar='+str(a.jar.resolve()),'-Dprobe.output='+str(results/'import.json'),'-cp',cp,'DistributionImportProbe'])
    startup=launch('restart',['-Xlog:class+load=info:file=results/classes.log','-Dprobe.identity='+identity,'-Dprobe.folder='+str(bool(a.workshop)).lower(),'-Dprobe.output='+str(results/'restart.json'),'-cp',cp,'ReleaseStartupProbe','-width','960','-height','600','-maximized','false'])
    log=(results/'restart.log').read_text(errors='replace')
    assert '[FPV] Native Java mod initialized against 160.7' in log and '[E]' not in log,log[-3000:]
    for n,b in entries.items():
        if a.workshop:assert (profile/'mods/Mindustry3'/n).read_bytes()==b
    if not a.workshop:
        # Native import normalizes a JAR to a .zip filename; the payload stays byte-identical.
        installed=[x for x in (profile/'mods').iterdir() if x.suffix in ('.jar','.zip')];assert len(installed)==1
        assert hashlib.sha256(installed[0].read_bytes()).digest()==hashlib.sha256(a.jar.read_bytes()).digest()
    classlog=(results/'classes.log').read_text()
    origins=sorted({s.split(' source: ',1)[1] for s in classlog.splitlines() if '] fpv.' in s and ' source: file:' in s})
    assert origins and all('/data/mods/' in s.replace('\\','/') for s in origins),origins
    report={'status':'PASS','runtime':a.java,'host':inspect(),'nativeImportApi':not bool(a.workshop),'realJvmRestart':not bool(a.workshop),'normalStartup':True,'unmodifiedProductionPayload':True,'noSmokeMode':True,'parentClasspath':'host + test observer only; no FPV/JNA/developer classes','classOrigins':origins,'jarSha256':hashlib.sha256(a.jar.read_bytes()).hexdigest(),'startup':startup,'physicalMenuAndInput':'NOT TESTED','cleanMachine':'NOT TESTED: disposable profile on same PC','steamClientRequired':False}
    (results/'result.json').write_bytes((json.dumps(report,indent=2)+'\n').encode());print(json.dumps(report,indent=2))
if __name__=='__main__':main()
