"""Read-only verification of a completed local release package and corresponding source."""
import argparse,hashlib,json,zipfile
from pathlib import Path
from distribution import jar_entries,payload_hash,safe_name

def sha(data):return hashlib.sha256(data).hexdigest()
def verify(out):
    manifest=json.loads((out/'release-manifest.json').read_text())
    checks=0
    expected=set(manifest['files'])|{'release-manifest.json','SHA256SUMS.txt'}
    assert {p.name for p in out.iterdir() if p.is_file()}==expected;checks+=1
    assert {p.name for p in out.iterdir() if p.is_dir()}=={'workshop'};checks+=1
    seen=set()
    for line in (out/'SHA256SUMS.txt').read_text().splitlines():
        digest,name=line.split('  ',1);safe_name(name)
        assert name not in seen;seen.add(name)
        assert sha((out/name).read_bytes())==digest,name;checks+=1
    assert seen==expected-{'SHA256SUMS.txt'};checks+=1
    jar=out/f"mindustry-fpv-{manifest['version']}.jar";entries=jar_entries(jar)
    identity=payload_hash(entries);assert identity==manifest['payloadIdentity'];checks+=1
    folder=out/'workshop/Mindustry3'
    expanded={p.relative_to(folder).as_posix():p.read_bytes() for p in folder.rglob('*') if p.is_file()}
    assert payload_hash(expanded)==identity;checks+=1
    for name,data in entries.items():assert expanded[name]==data;checks+=1
    with zipfile.ZipFile(out/f"mindustry-fpv-{manifest['version']}-windows.zip") as z:
        assert z.read(jar.name)==jar.read_bytes();checks+=1
        assert z.read('INSTALL.txt')==(out/'INSTALL.txt').read_bytes();checks+=1
    with zipfile.ZipFile(out/f"mindustry-fpv-{manifest['version']}-workshop.zip") as z:
        assert {n.removeprefix('Mindustry3/'):z.read(n) for n in z.namelist()}==expanded;checks+=1
    with zipfile.ZipFile(out/f"mindustry-fpv-{manifest['version']}-corresponding-source.zip") as z:
        source=json.loads(z.read('SOURCE-MANIFEST.json'));assert source['sourceCommit']==manifest['sourceCommit'];checks+=1
        assert source['releaseJarSha256']==sha(jar.read_bytes()) and source['payloadIdentity']==identity;checks+=1
        assert len(source['files'])==manifest['sourceFileCount'];checks+=1
        assert set(z.namelist())==set(source['files'])|{'SOURCE-MANIFEST.json'};checks+=1
        for name,digest in source['files'].items():
            safe_name(name);assert not name.startswith(('.git/','local/','dist/','build/','lib/'));assert sha(z.read(name))==digest;checks+=1
        assert z.read('mod.hjson')==z.read('src/main/resources/mod.hjson')==entries['mod.hjson'];checks+=1
    return {'status':'PASS','checks':checks,'version':manifest['version'],'sourceCommit':manifest['sourceCommit'],'sourceFiles':manifest['sourceFileCount'],'jarSha256':sha(jar.read_bytes()),'identity':identity,'published':False}
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('directory',type=Path);a=p.parse_args();print(json.dumps(verify(a.directory),indent=2))
