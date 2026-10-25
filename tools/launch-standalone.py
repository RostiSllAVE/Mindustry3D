"""Launch a fresh isolated standalone game. Existing runs/profiles are never overwritten."""
import argparse,hashlib,json,os,re,shutil,subprocess,sys,zipfile
from pathlib import Path
from dev_runtime import ROOT,config,inspect,jdk_home,java_tool,local_path,runtime

def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--run',required=True);p.add_argument('--jar');p.add_argument('--game');p.add_argument('--java',choices=['java17','bundled25'],default='bundled25')
    p.add_argument('--fixture',type=Path,help='Explicit developer-only SmokeHarness replacement source')
    p.add_argument('--helper',type=Path,action='append',default=[]);p.add_argument('--property',action='append',default=[])
    p.add_argument('--interactive',action='store_true');p.add_argument('--exe-copy',action='store_true',help='Test a disposable Packr exe/JSON copy; never alters the reference')
    a=p.parse_args()
    if not re.fullmatch('[a-zA-Z0-9-]+',a.run):p.error('Use a new simple run ID')
    if a.exe_copy and os.name!='nt':p.error('The original Windows Packr exe test is Windows-only')
    if a.exe_copy and a.java!='bundled25':p.error('The original exe-copy test uses its bundled Java25; use direct java17 for that compatibility check')
    info=inspect(a.game);game=Path(info['gameRoot']);java=runtime(a.java,a.game)
    hint=a.jar
    latest=ROOT/'local/latest-diagnostic.json'
    if not hint and latest.exists():hint=json.loads(latest.read_text())['jar']
    if not hint:hint=config().get('diagnosticJar')
    if not hint:p.error('Supply --jar with the tested diagnostic JAR, or set local/dev-runtime.json diagnosticJar')
    jar=local_path(hint)
    if not jar.is_file():p.error('Mod JAR not found: '+str(jar))
    out=ROOT/'local/standalone-runs'/a.run
    if out.exists():p.error('Existing run; select a new ID to preserve evidence')
    for name in ['data/mods','tmp','appdata','localappdata']: (out/name).mkdir(parents=True,exist_ok=True)
    mod=out/'data/mods/mindustry-fpv.jar';overlays={}
    if a.fixture:
        sources=[a.fixture.resolve()]+[x.resolve() for x in a.helper]
        if not all(x.is_file() for x in sources):p.error('Fixture/helper source is missing')
        classes=out/'fixture-classes';classes.mkdir()
        subprocess.run([str(java_tool(jdk_home(),'javac')),'--release','17','-cp',str(jar)+os.pathsep+info['desktopJar'],'-d',str(classes),*map(str,sources)],check=True)
        overlays={x.relative_to(classes).as_posix():x.read_bytes() for x in classes.rglob('*.class')}
        if 'fpv/SmokeHarness.class' not in overlays or any(not n.startswith('fpv/') for n in overlays):p.error('Fixtures must explicitly provide fpv.SmokeHarness and only fpv test helper classes')
        with zipfile.ZipFile(jar) as z,zipfile.ZipFile(mod,'x',zipfile.ZIP_DEFLATED) as dest:
            for entry in z.infolist():dest.writestr(entry,overlays.get(entry.filename,z.read(entry)))
            for n,data in overlays.items():
                if n not in z.namelist():dest.writestr(n,data)
    else:shutil.copy2(jar,mod)
    props=['-Dnodiscord=true','-Djava.io.tmpdir='+str(out/'tmp'),'-Dmindustry.data.dir='+str(out/'data'),'-Dfpv.lab.path='+str(out)]
    if a.fixture:props+=['-Dfpv.smoke=true','-Dfpv.visualProfile=true']
    for item in a.property:
        if not re.fullmatch(r'fpv\.[A-Za-z0-9_.-]+=[^\r\n]*',item):p.error('Only explicit fpv.* fixture properties are accepted')
        props.append('-D'+item)
    if a.java=='bundled25':props=['--enable-native-access=ALL-UNNAMED']+props
    args=['-width','960','-height','600','-maximized','false']
    command=[str(java),*props,'-cp',info['desktopJar'],'mindustry.desktop.DesktopLauncher',*args]
    if a.exe_copy:
        exe=out/'Mindustry.exe';shutil.copy2(game/'Mindustry.exe',exe)
        launcher=dict(info['launcher']);launcher['jrePath']=str(game/'jre');launcher['classPath']=[info['desktopJar']];launcher['vmArgs']=launcher.get('vmArgs',[])+props
        (out/'Mindustry.json').write_text(json.dumps(launcher,indent=2)+'\n')
        command=[str(exe),*args]
    env=dict(os.environ,APPDATA=str(out/'appdata'),LOCALAPPDATA=str(out/'localappdata'),TEMP=str(out/'tmp'),TMP=str(out/'tmp'))
    startup=None;flags=0
    if os.name=='nt' and not a.interactive:
        startup=subprocess.STARTUPINFO();startup.dwFlags|=subprocess.STARTF_USESHOWWINDOW;startup.wShowWindow=0;flags=subprocess.CREATE_NO_WINDOW
    with (out/'stdout.log').open('wb') as stdout,(out/'stderr.log').open('wb') as stderr:
        process=subprocess.Popen(command,cwd=out,env=env,stdin=subprocess.DEVNULL,stdout=stdout,stderr=stderr,startupinfo=startup,creationflags=flags)
    record={'pid':process.pid,'runDirectory':str(out),'host':info,'runtime':a.java,'java':str(java),'sourceJarSha256':sha(jar),'installedJarSha256':sha(mod),'fixtureOverlays':{n:hashlib.sha256(data).hexdigest() for n,data in overlays.items()},'exeCopy':a.exe_copy,'steamAppIdFileCreated':False,'profileIsNew':True}
    (out/'process.json').write_text(json.dumps(record,indent=2)+'\n');print(json.dumps({'pid':process.pid,'runDirectory':str(out),'gameVersion':info['gameVersion'],'runtime':a.java,'exeCopy':a.exe_copy},indent=2))
if __name__=='__main__':main()
