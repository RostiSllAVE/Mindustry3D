"""Developer test only: native import API, real JVM restart and isolated SDL fixture.
The restart has only the installed host JAR on its parent classpath."""
import argparse
import json
import os
import shutil
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def run(args):
    work = args.work.resolve()
    if work.exists():
        raise SystemExit('Choose a new isolated test directory')
    # The Steam host uses cwd/saves, overriding the generic data-directory hint.
    profile, output, tmp = work / 'saves', work / 'results', work / 'tmp'
    for p in (profile / 'mods', output, tmp): p.mkdir(parents=True, exist_ok=True)
    host = args.game.resolve() / 'jre/desktop.jar'
    java = args.java_home.resolve() / 'bin/java.exe'
    instrumentation = ROOT / 'local/distribution-audit/instrumentation'
    if args.mode == 'jar' and not (instrumentation / 'DistributionImportProbe.class').is_file():
        raise SystemExit('Compile tools/java/DistributionImportProbe.java into local/distribution-audit/instrumentation first')
    (work / 'steam_appid.txt').write_text('1127400\n')
    version = subprocess.check_output([str(java), '-version'], stderr=subprocess.STDOUT, text=True)
    import re
    major = int(re.search(r'version "(\d+)', version).group(1))
    common = [str(java)] + (['--enable-native-access=ALL-UNNAMED'] if major >= 22 else []) + [
        '-Dnodiscord=true', '-Djava.io.tmpdir=' + str(tmp), '-Dmindustry.data.dir=' + str(profile),
        '-Djava.library.path=' + str(args.game.resolve())]
    creation = getattr(subprocess, 'CREATE_NO_WINDOW', 0)
    if args.mode == 'jar':
        command = common + ['-Dprobe.profile=' + str(profile), '-Dprobe.jar=' + str(args.jar.resolve()),
            '-Dprobe.output=' + str(output / 'import.json'), '-cp', str(host) + os.pathsep + str(instrumentation), 'DistributionImportProbe']
        with (output / 'import.log').open('w') as log:
            subprocess.run(command, cwd=work, stdout=log, stderr=log, creationflags=creation, timeout=120, check=True)
        imported = json.loads((output / 'import.json').read_text())
        if imported['status'] != 'PASS': raise SystemExit('Native import failed: ' + str(imported))
    else:
        if not args.workshop: raise SystemExit('--workshop is required for folder testing')
        shutil.copytree(args.workshop, profile / 'mods/Mindustry3')
    # No instrumentation, source/classes, JNA path or developer resources on this classpath.
    command = common + ['-Xlog:class+load=info:file=results/classes.log',
        '-Dfpv.lab.path=' + str(work), '-Dfpv.smoke=true', '-Dfpv.layers.smoke=true',
        '-Dfpv.mouse.smoke=true', '-Dfpv.placement.smoke=true', '-Dfpv.smoke.output=' + str(output),
        '-cp', str(host), 'mindustry.desktop.DesktopLauncher', '-width', '1280', '-height', '800', '-maximized', 'false']
    with (output / 'restart.log').open('w') as log:
        subprocess.run(command, cwd=work, stdout=log, stderr=log, creationflags=creation, timeout=180, check=True)
    summarize(work, args.mode, args.expect_unverified)

def summarize(work, mode, expect_unverified=False):
    work = work.resolve()
    profile, output = work / 'saves', work / 'results'
    report = json.loads((output / 'layers-result.json').read_text())
    if report['runStatus'] != 'passed': raise SystemExit('SDL regression failed')
    log = (output / 'restart.log').read_text(encoding='utf-8', errors='replace')
    if '[FPV] Native Java mod initialized against 160.7' not in log or '[E]' in log:
        raise SystemExit('Unexpected initialization/error log')
    unverified = 'Single-player remains available; multiplayer is disabled' in log
    if unverified != expect_unverified:
        raise SystemExit('Unexpected verification policy result')
    classlog = (output / 'classes.log').read_text(encoding='utf-8')
    origins = {line.split(' source: ', 1)[1] for line in classlog.splitlines() if '] fpv.' in line and ' source: ' in line}
    # Lambda classes name their defining class as source; actual classes have file URLs.
    if any(not origin.startswith(('file:', 'fpv.')) for origin in origins):
        raise SystemExit('Unknown FPV class origin')
    fpv_sources = sorted(origin for origin in origins if origin.startswith('file:'))
    if not fpv_sources or any(str(profile).replace('\\', '/') not in source.replace('file:/', '').replace('\\', '/') for source in fpv_sources):
        raise SystemExit('FPV loaded from an unexpected external source: ' + str(fpv_sources))
    summary = {'status': 'PASS', 'method': mode, 'java': '25.0.1' if '[JAVA] Version: 25.0.1' in log else 'see restart.log',
               'freshProfile': True, 'nativeImportApi': mode == 'jar', 'realJvmRestart': True,
               'restartParentClasspath': 'installed host JAR only', 'fpvClassSources': fpv_sources,
               'developerResourcesRequired': False, 'fixtureChecks': report['checks'],
               'networkVerification': 'unavailable; SP still passed' if unverified else 'verified',
               'physicalMenuAndInput': 'NOT TESTED by this script', 'cleanMachine': 'NOT TESTED; same PC'}
    (output / 'distribution-result.json').write_text(json.dumps(summary, indent=2))
    print(json.dumps(summary, indent=2))

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--work', required=True, type=Path)
    parser.add_argument('--java-home', required=True, type=Path)
    parser.add_argument('--game', required=True, type=Path)
    parser.add_argument('--jar', required=True, type=Path)
    parser.add_argument('--mode', choices=['jar', 'folder'], required=True)
    parser.add_argument('--workshop', type=Path)
    parser.add_argument('--inspect-completed', action='store_true', help='Only validate existing completed logs; never restart or modify the profile')
    parser.add_argument('--expect-unverified', action='store_true', help='Deliberately malformed test fixture only: require SP survival with unavailable network identity')
    args = parser.parse_args()
    if args.inspect_completed: summarize(args.work, args.mode, args.expect_unverified)
    else: run(args)
