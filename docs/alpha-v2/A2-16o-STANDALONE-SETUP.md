# A2-16o supplement — standalone development runtime

Baseline: accepted `d071769` on `main`. This checkpoint changes developer configuration/tools only. A2-16o gameplay/model cleanup follows it. No published mod/game artifacts are replaced.

## Inspected reference

`D:\Linux\ForCodex\OriginalGameMindustry` is the owner's clean Windows reference. Its `Mindustry.json` is a Packr launcher description: `Mindustry.exe` loads the bundled JVM, classpath `jre/desktop.jar` and `mindustry.desktop.DesktopLauncher`. The EXE is not the game archive.

- Official **Mindustry 160.7**, `modifier=release`, matching the mod's supported target.
- Game archive SHA-256: `8cfd4322726877c7ab558a5efe5fae493b74b486214dd300ecdc90761a0aa7c7`.
- Bundled **Java 25.0.1**, modules `java.base jdk.unsupported`; it intentionally lacks `java.logging`. The accepted optional-native-input fallback remains in use; no mod/runtime gameplay code changed here.
- Native Arc/SDL resources are provided by the game archive. No game DLL replacement, second SDL instance or Steam AppID file is required.
- The bundled Java command accepts JVM properties and normal DesktopLauncher arguments. A disposable EXE/JSON copy also passed with its original bundled JVM: `-width 960 -height 600` reached the real game viewport, and the custom data path was honored.

The original EXE, JSON, JRE and game archive were never written. All **98 reference files** matched their initial hashes after the tests. Local instrumentation copies only the EXE/JSON when explicitly requested; it does not redistribute game binaries.

## Default workflow

`tools/dev_runtime.py` selects the standalone SDK/runtime; it never discovers Steam or silently falls back to it. Defaults are the sibling `ForCodex/OriginalGameMindustry` installation, or explicit `FPV_MINDUSTRY` / ignored `local/dev-runtime.json` overrides. `tools/dev-runtime.example.json` documents the local configuration. The tested compiler/runtime is the separately available complete Java 17 JDK; the primary game runtime is its own bundled Java 25. The requested runtime version is checked explicitly.

`tools/build.py` and `tools/build-diagnostics.py` use that SDK default. A successful diagnostic build writes ignored `local/latest-diagnostic.json`. The new launcher selects `--jar` first, then that latest-build pointer, then the optional configured diagnostic JAR. Existing visual, layer, input, network and camera developer scripts use the shared standalone game resolver; ordinary current suite launchers select the latest diagnostic. Historical fixture/compatibility flags remain explicit. Previously archived scripts/results are not rewritten. Player Alpha launchers and published archives are preserved.

Every `tools/launch-standalone.py` run creates a **new** `local/standalone-runs/<run>/` with one mod JAR, its own game data/mods, APPDATA/LOCALAPPDATA, Java/native temporary extraction directory, logs and manifest. Existing run IDs are rejected. No campaign, personal maps, Workshop files or normal profile settings are imported. No backups are needed for an empty disposable profile; the clean reference is protected by the hash snapshot. Fixture compilation uses Java 17 and overlays only explicit `fpv` smoke/helper classes in the disposable mod copy; the source JAR is unchanged and both hashes are recorded.

## Exact commands

Run from the repository with Python 3. Developers need a complete JDK for compilation/fixtures; players using the JAR do not.

```powershell
python tools/dev_runtime.py
python tools/test-dev-runtime.py
python tools/launch-standalone.py --run my-playtest --interactive
python tools/launch-standalone.py --run my-smoke25 --fixture tools/java/StandaloneSmokeHarness.java --helper tools/java/LogicDisplayGpuChecks.java
python tools/launch-standalone.py --run my-smoke17 --java java17 --fixture tools/java/StandaloneSmokeHarness.java --helper tools/java/LogicDisplayGpuChecks.java
python tools/launch-standalone.py --run my-exe-test --exe-copy --fixture tools/java/StandaloneSmokeHarness.java --helper tools/java/LogicDisplayGpuChecks.java
python tools/build-diagnostics.py --task a2-16o --run build-unique-id
```

Use a fresh run ID each time. Supply `--jar <path>` to select a specific accepted build. The default runtime is the standalone bundled JVM. `--java java17` deliberately selects the configured complete Java 17 JDK. `--exe-copy` is a Windows-only launcher audit using its bundled Java 25 and a modified disposable JSON pointing at the read-only reference JRE. `--interactive` is for owner gameplay; automated launches request a hidden window and never synthesize physical keyboard/mouse events.

## Actual results

| Validation | Result |
|---|---|
| Standalone 160.7 metadata, packaged classes, reduced JVM/modules | PASS inspected directly |
| Fresh-profile JAR detection and complete mod initialization | PASS actual native game |
| Bundled standalone Java 25 | PASS 90 frames, 42 GPU checks, zero GL errors |
| Complete Java 17 with standalone game archive | PASS 90 frames, 42 GPU checks, zero GL errors |
| Disposable original Packr EXE + JSON launch | PASS 90 frames, 42 GPU checks; viewport 960×600; correct data path |
| FPV/Build Mode entry/exit and native planning/motion | PASS actual production APIs and native Unit.update fixtures |
| Steam-host flag / Steam game dependency | PASS standalone hosts report `Vars.steam=false`; no Steam launch/AppID/interactions |
| Logs and screenshots | PASS in each disposable run |
| Runtime resolution/safety regressions | PASS six Python tests; migrated PowerShell syntax validated |
| Clean installation integrity | PASS all 98 file hashes unchanged |
| Physical F5/Tab, continuous mouse rotation, manual Import Mod/restart | NOT TESTED in this automated setup pass |
| Separate clean machine or physical Linux host | NOT TESTED |

Evidence: ignored `local/standalone-setup/verification.json` and `local/standalone-runs/setup-j25a`, `setup-j17`, `setup-exe`. These are local development evidence, not game distributions. The underlying accepted diagnostic payload is the same Windows/Linux mod; no public compatibility claims are expanded. A2-16o's final regression/graphics builds will use this standalone host.
