"""Local-only player packaging. No network operations or publishing commands."""
import argparse
import hashlib
import json
import struct
import subprocess
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
COSMETIC = {'steamid.json', 'preview.png', 'icon.png'}

def safe_name(name):
    if not name or name.startswith('/') or '\\' in name or ':' in name or any(p in ('', '.', '..') for p in name.split('/')):
        raise ValueError('Unsafe archive path: ' + name)

def payload_hash(entries):
    if not {'mod.hjson', 'fpv/FpvMod.class'} <= entries.keys():
        raise ValueError('Incomplete compiled mod payload')
    digest = hashlib.sha256(b'Mindustry3-payload-v1\0')
    for name, data in sorted(entries.items()):
        safe_name(name)
        if name in COSMETIC:
            continue
        path = name.encode('utf-8')
        digest.update(struct.pack('>I', len(path)) + path + struct.pack('>q', len(data)) + data)
    return 'payload1-' + digest.hexdigest()

def jar_entries(jar):
    with zipfile.ZipFile(jar) as archive:
        names = [e.filename for e in archive.infolist() if not e.is_dir()]
        if len(names) != len(set(names)):
            raise ValueError('Duplicate archive entries')
        for name in names:
            safe_name(name)
        entries = {name: archive.read(name) for name in names}
    forbidden = [n for n in entries if n.startswith(('arc/', 'mindustry/')) or n.endswith(('.msav', '.etl', '.jfr', '.exe', '.jar', '.zip'))]
    if forbidden:
        raise ValueError('Unexpected distribution payload: ' + str(forbidden))
    for required in ('LICENSE', 'THIRD_PARTY_NOTICES.md', 'licenses/JNA-LICENSE.txt', 'licenses/JNA-APACHE-2.0.txt', 'licenses/LIBFFI-LICENSE.txt'):
        if required not in entries:
            raise ValueError('Missing license: ' + required)
    if entries['mod.hjson'] != (ROOT / 'mod.hjson').read_bytes() or entries['mod.hjson'] != (ROOT / 'src/main/resources/mod.hjson').read_bytes():
        raise ValueError('Repository/packaged metadata differ')
    return entries

def package(jar, out, commit=None):
    import re
    entries = jar_entries(jar)
    version = re.search(r'^version:\s*(\S+)', entries['mod.hjson'].decode(), re.M).group(1)
    git = ['git', '-c', 'safe.directory=' + ROOT.as_posix(), '-C', str(ROOT)]
    if commit:
        head = subprocess.check_output(git + ['rev-parse', 'HEAD'], text=True).strip()
        if head != commit or subprocess.check_output(git + ['status', '--porcelain'], text=True).strip():
            raise ValueError('A clean matching source checkpoint is required')
    if out.exists():
        raise ValueError('Refusing to overwrite existing package directory')
    out.mkdir(parents=True)
    (out / jar.name).write_bytes(jar.read_bytes())
    notes = (ROOT / 'docs/player/INSTALL.txt').read_bytes()
    with zipfile.ZipFile(out / f'mindustry-fpv-{version}-windows.zip', 'w', zipfile.ZIP_DEFLATED) as archive:
        archive.writestr(jar.name, jar.read_bytes())
        archive.writestr('INSTALL.txt', notes)
        for name in entries:
            if name in ('LICENSE', 'THIRD_PARTY_NOTICES.md') or name.startswith('licenses/'):
                archive.writestr(name, entries[name])
    workshop = out / 'workshop' / 'Mindustry3'
    workshop.mkdir(parents=True)
    for name, data in entries.items():
        target = workshop / name
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
    (workshop / 'preview.png').write_bytes((ROOT / 'assets/workshop-preview.png').read_bytes())
    unpacked = {p.relative_to(workshop).as_posix(): p.read_bytes() for p in workshop.rglob('*') if p.is_file()}
    identity = payload_hash(entries)
    if payload_hash(unpacked) != identity:
        raise ValueError('Workshop payload differs from JAR')
    with zipfile.ZipFile(out / f'mindustry-fpv-{version}-workshop.zip', 'w', zipfile.ZIP_DEFLATED) as archive:
        for name, data in sorted(unpacked.items()):
            archive.writestr('Mindustry3/' + name, data)
    source_count = None
    if commit:
        import io
        raw = subprocess.check_output(git + ['archive', '--format=zip', commit])
        with zipfile.ZipFile(io.BytesIO(raw)) as exported:
            names = [e.filename for e in exported.infolist() if not e.is_dir()]
            for name in names:
                safe_name(name)
                if name.startswith(('.git/', 'local/', 'build/', 'dist/', 'research/')) or name.endswith(('.jar', '.msav', '.jfr', '.etl')):
                    raise ValueError('Private/build artifact in source snapshot')
            source_count = len(names)
            manifest = {'sourceCommit': commit, 'version': version, 'releaseJarSha256': hashlib.sha256(jar.read_bytes()).hexdigest(), 'payloadIdentity': identity,
                        'files': {n: hashlib.sha256(exported.read(n)).hexdigest() for n in names}}
            with zipfile.ZipFile(out / f'mindustry-fpv-{version}-corresponding-source.zip', 'w', zipfile.ZIP_DEFLATED) as source:
                for name in names:
                    source.writestr(name, exported.read(name))
                source.writestr('SOURCE-MANIFEST.json', json.dumps(manifest, indent=2))
    (out / 'INSTALL.txt').write_bytes(notes)
    for name in ('ALPHA-1.0.3-DISTRIBUTION.md', 'DISTRIBUTION-VALIDATION.json'):
        evidence = ROOT / 'docs/releases' / name
        if evidence.is_file(): (out / name).write_bytes(evidence.read_bytes())
    files = {p.name: {'bytes': p.stat().st_size, 'sha256': hashlib.sha256(p.read_bytes()).hexdigest()} for p in out.iterdir() if p.is_file()}
    manifest = {'version': version, 'recommendedRelease': 'Alpha 1.0.3 [v160.7]', 'target': 'Windows x64 / Mindustry 160.7', 'sourceCommit': commit, 'sourceFileCount': source_count,
                'payloadIdentity': identity, 'jarEntries': len(entries), 'workshopEntries': len(unpacked),
                'status': 'LOCAL ONLY; publishing and real Workshop subscription are not performed', 'files': files}
    (out / 'release-manifest.json').write_bytes((json.dumps(manifest, indent=2) + '\n').encode())
    (out / 'SHA256SUMS.txt').write_bytes(('\n'.join(v['sha256'] + '  ' + n for n, v in files.items()) + '\n').encode())
    print(json.dumps(manifest, indent=2))

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--jar', required=True, type=Path)
    parser.add_argument('--out', required=True, type=Path)
    parser.add_argument('--source-commit', help='Verified clean HEAD; omit only for test staging')
    args = parser.parse_args()
    package(args.jar, args.out, args.source_commit)
