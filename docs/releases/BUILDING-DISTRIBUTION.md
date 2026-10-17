# Build/package 0.18.31 from corresponding source

These are developer instructions. Players only import the compiled JAR and
restart Mindustry; see [player instructions](../player/INSTALL.txt).

Use a fresh extracted source tree, Python 3, complete 64-bit JDK 17, the installed
160.7 `desktop.jar`, and JNA core 5.19.1 in `lib/jna-5.19.1.jar`.
JNA SHA-256: `4fb141dd8ef6b0585ffceea4bc49602fbc6312fa977e2c488794ea3e6aafecae`.
See [dependency sources and licenses](../../THIRD_PARTY_NOTICES.md).

```powershell
$env:FPV_JDK = 'E:\Tools\jdk-17'
$env:FPV_MINDUSTRY = 'E:\SteamLibrary\steamapps\common\Mindustry'
python tools/build.py
```

`FPV_MINDUSTRY` may instead identify `desktop.jar` directly; `FPV_JNA` can identify
the dependency JAR. JDK discovery also accepts `JAVA_HOME` or `javac` on PATH.
The builder never downloads dependencies and never overwrites an existing
versioned JAR. Use a new source directory for subsequent validation builds.
Its tests and temporary files stay beneath the source tree's `build/` directory.

For maintainer packaging from a clean, verified Git checkpoint:

```powershell
python tools/distribution.py --jar dist/mindustry-fpv-0.18.31.jar `
  --out dist/alpha-1.0.4 --source-commit (git rev-parse HEAD)
```

The packager verifies metadata, mandatory license files, forbidden host/artifact
paths and JAR/folder payload identity. It creates the JAR, convenience ZIP,
Workshop folder/ZIP, matching source ZIP, manifests and SHA256SUMS. It refuses
an existing output directory, performs no network operations and does not publish.
An extracted source ZIP can rebuild the JAR without Git; Git is only needed for
the maintainer's checkpoint-based source export.

Archive timestamps can change the outer JAR SHA-256 between builds. The native
and Python canonical payload algorithms compare every included file's bytes,
path and length; their equality was used to verify the independent source build.
Use the exact distributed JAR for download checksum verification.

Optional native import/restart tests use `tools/java/DistributionImportProbe.java`
and `tools/distribution_probe.py`. Compile the probe against the host JAR into
`local/distribution-audit/instrumentation`, then run `distribution_probe.py --help`.
The import stage uses the host's real API; restart drops the probe classpath and
loads only the installed host and mod. A fresh test directory is mandatory. The
Steam host uses `cwd/saves` for its profile. Physical menu/input acceptance is
separate. Local network tests use PowerShell 7 and explicit game/JDK/server paths.

Historical release/launch/profiling tools target older accepted binaries and can
retain developer paths. Do not run them to recreate or replace historical assets.

For the 0.18.31 audit, an existing global Workshop subscription meant an empty
profile alone did not isolate the candidate. The recorded rerun used an ignored
test launcher adapter to exclude subscribed content enumeration. It is not a
player requirement, is not packaged, and is disclosed in BROWSER-VALIDATION.json.
Do not mistake a fresh profile for a clean Windows machine or a physical menu test.
