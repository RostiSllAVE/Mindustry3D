"""Local reproducible build against the user's installed Mindustry; never bundles host classes."""
import hashlib
import json
import os
import re
from pathlib import Path
import subprocess
import shutil
import sys
import zipfile
from preflight import ROOT, run
from dev_runtime import host_jar, jdk_home, java_tool

metadata = ROOT / "src/main/resources/mod.hjson"
if not (ROOT / "mod.hjson").is_file() or (ROOT / "mod.hjson").read_bytes() != metadata.read_bytes():
    raise SystemExit("Root and packaged mod.hjson must be identical")
version = re.search(r"^version:\s*(\S+)", metadata.read_text(encoding="utf-8"), re.M).group(1)
output = ROOT / "dist" / ("mindustry-fpv-" + version + ".jar")
manifest_output = output.with_name(output.stem + "-manifest.json")
if output.exists() or manifest_output.exists():
    raise SystemExit("Refusing to overwrite an existing versioned build: " + str(output))
jdk = jdk_home()
host = host_jar()
jna = Path(os.environ.get("FPV_JNA", str(ROOT / "lib/jna-5.19.1.jar")))
for path in (java_tool(jdk,"javac"), host, jna):
    if not path.is_file():
        raise SystemExit(f"Missing required local dependency: {path}")
if hashlib.sha256(jna.read_bytes()).hexdigest() != "4fb141dd8ef6b0585ffceea4bc49602fbc6312fa977e2c488794ea3e6aafecae":
    raise SystemExit("Expected unmodified JNA core 5.19.1")
run()
build = ROOT / "build"
classes = build / "classes"
# Build output is owned by this tool; remove stale classes before compiling a new version.
if classes.exists():
    if classes.is_symlink() or classes.resolve() != ROOT.resolve() / "build" / "classes":
        raise SystemExit("Unexpected class-output location")
    shutil.rmtree(classes)
classes.mkdir(parents=True, exist_ok=True)
sources = sorted((ROOT / "src/main/java").rglob("*.java")) + sorted((build / "generated").rglob("*.java"))
args = ["--release", "17", "-encoding", "UTF-8", "-classpath", str(host) + os.pathsep + str(jna), "-d", str(classes)] + [str(p) for p in sources]
argfile = build / "javac.args"
argfile.write_text("\n".join('"' + a.replace("\\", "/") + '"' for a in args), encoding="utf-8")
subprocess.run([str(java_tool(jdk,"javac")), "@" + str(argfile)], check=True)
tests = sorted((ROOT / "src/test/java").rglob("*.java"))
if tests:
    testclasses = build / "test-classes"
    testclasses.mkdir(exist_ok=True)
    cp = str(classes) + os.pathsep + str(host) + os.pathsep + str(jna)
    subprocess.run([str(java_tool(jdk,"javac")), "--release", "17", "-encoding", "UTF-8", "-classpath", cp, "-d", str(testclasses)] + [str(p) for p in tests], check=True)
    test_tmp = build / "test-tmp"
    test_tmp.mkdir(exist_ok=True)
    for test in tests:
        name = test.relative_to(ROOT / "src/test/java").with_suffix("").as_posix().replace("/", ".")
        subprocess.run([str(java_tool(jdk,"java")), "-Djava.io.tmpdir=" + str(test_tmp), "-ea", "-cp", str(testclasses) + os.pathsep + cp, name], check=True)
dist = ROOT / "dist"
dist.mkdir(exist_ok=True)
entries = []
with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as z:
    for base in (classes, ROOT / "src/main/resources"):
        for p in sorted(base.rglob("*")):
            if p.is_file():
                rel = p.relative_to(base).as_posix()
                z.write(p, rel)
    for p in (ROOT / "licenses").glob("*.txt"):
        z.write(p, "licenses/" + p.name)
    z.write(ROOT / "LICENSE", "LICENSE")
    z.write(ROOT / "THIRD_PARTY_NOTICES.md", "THIRD_PARTY_NOTICES.md")
    # Include only the redistributable dependency. The player's game JAR is compile-only.
    with zipfile.ZipFile(jna) as dep:
        for entry in dep.infolist():
            if entry.is_dir() or entry.filename.startswith("META-INF/"):
                continue
            z.writestr(entry.filename, dep.read(entry))
    for p in (ROOT / "design").glob("*.sheet.json"):
        z.write(p, "design/" + p.name)
with zipfile.ZipFile(output) as z:
    entries = [{"path":e.filename,"size":e.file_size} for e in z.infolist()]
    forbidden = [e["path"] for e in entries if e["path"].startswith(("arc/", "mindustry/")) or "Assembly-CSharp" in e["path"]]
    if forbidden:
        raise SystemExit("Unexpected game classes in package: " + str(forbidden))
manifest = {"fileName":output.name,"bytes":output.stat().st_size,"sha256":hashlib.sha256(output.read_bytes()).hexdigest(),"entries":entries,"status":"local distribution candidate; not published"}
manifest_output.write_text(json.dumps(manifest, indent=2), encoding="utf-8")
print(json.dumps({k:v for k,v in manifest.items() if k != "entries"}, indent=2))
