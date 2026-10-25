"""Local desktop SDK/runtime selection. Never discovers or launches Steam implicitly."""
from pathlib import Path
import json, os, re, shutil, subprocess, zipfile

ROOT=Path(__file__).resolve().parents[1]
CONFIG=ROOT/'local/dev-runtime.json'

def config():
    return json.loads(CONFIG.read_text(encoding='utf-8-sig')) if CONFIG.is_file() else {}

def local_path(value):
    p=Path(value).expanduser()
    return (ROOT/p).resolve() if not p.is_absolute() else p.resolve()

def game_dir(value=None):
    candidate=value or os.environ.get('FPV_MINDUSTRY') or config().get('gameRoot')
    p=local_path(candidate) if candidate else ROOT.parent/'ForCodex/OriginalGameMindustry'
    return p.parent.parent if p.is_file() and p.name=='desktop.jar' else p

def host_jar(value=None):
    p=game_dir(value)/'jre/desktop.jar'
    if not p.is_file():raise FileNotFoundError('Standalone desktop.jar not found: '+str(p)+'; set FPV_MINDUSTRY or local/dev-runtime.json. No Steam fallback is used.')
    return p

def properties(jar):
    with zipfile.ZipFile(jar) as z:text=z.read('version.properties').decode('utf-8')
    return dict(line.split('=',1) for line in text.splitlines() if '=' in line and not line.startswith('#'))

def inspect(value=None):
    game=game_dir(value);jar=host_jar(game);version=properties(jar)
    if version.get('build')!='160.7':raise ValueError('Expected Mindustry 160.7, found '+str(version.get('build')))
    if version.get('modifier')=='steam':raise ValueError('This is a Steam host. Select the standalone installation explicitly; no automatic Steam fallback is allowed.')
    release=game/'jre/release';text=release.read_text() if release.exists() else ''
    java=game/'jre/bin'/('java.exe' if os.name=='nt' else 'java')
    if not java.is_file():raise FileNotFoundError('Missing bundled Java: '+str(java))
    launcher=game/'Mindustry.json'
    return {'gameRoot':str(game),'desktopJar':str(jar),'gameVersion':version.get('build'),'modifier':version.get('modifier'),'type':version.get('type'),'bundledJava':str(java),'javaVersion':re.search(r'JAVA_VERSION="([^"]+)"',text).group(1) if 'JAVA_VERSION=' in text else 'unknown','modules':re.search(r'MODULES="([^"]+)"',text).group(1).split() if 'MODULES=' in text else [],'launcher':json.loads(launcher.read_text()) if launcher.exists() else None}

def jdk_home(value=None):
    hint=value or os.environ.get('FPV_JDK') or config().get('java17') or os.environ.get('JAVA_HOME')
    if hint:p=local_path(hint)
    else:
        compiler=shutil.which('javac')
        if not compiler:raise FileNotFoundError('A complete Java 17+ JDK is required to build: set FPV_JDK or local/dev-runtime.json java17. Players do not need a JDK.')
        p=Path(compiler).resolve().parent.parent
    if not java_tool(p,'javac').is_file():raise FileNotFoundError('Missing JDK compiler in '+str(p))
    return p

def java_tool(home,name):return Path(home)/'bin'/(name+'.exe' if os.name=='nt' else name)

def runtime(mode='bundled25',value=None):
    if mode=='bundled25':
        info=inspect(value)
        if not info['javaVersion'].startswith('25.'):raise ValueError('bundled25 requires an actual Java 25 runtime; detected '+info['javaVersion'])
        return Path(info['bundledJava'])
    java=java_tool(jdk_home(),'java')
    result=subprocess.run([str(java),'-version'],capture_output=True,text=True,check=True)
    if not re.search(r'version "17[.\"]',result.stdout+result.stderr):raise ValueError('java17 requires an actual Java 17 JDK; select it using FPV_JDK or local/dev-runtime.json')
    return java

if __name__=='__main__':
    import argparse
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--game');parser.add_argument('--java',choices=['java17','bundled25'])
    args=parser.parse_args()
    print(str(runtime(args.java,args.game)) if args.java else json.dumps(inspect(args.game),indent=2))
