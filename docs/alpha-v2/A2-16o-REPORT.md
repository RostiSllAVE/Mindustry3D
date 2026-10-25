# A2-16o — production, payloads, utilities and local rendering settings

Baseline: accepted `d071769` on `main`, followed by standalone configuration checkpoint `d409f22`. The owner's current A2-16o label is used despite older report filename drift. No branch switch, reset, merge, publication, Workshop update or published artifact replacement is part of this task.

## Confirmed production defects and changes

The new native regression reproduces the old failure: `FactorySpawnSafety.placement()` approves a clear XYZ exit, then `UnitPayload.dump()` rejects it because `Units.count()` counts a grounded unit at the same XY on another floor. This is a real double-validation mismatch, not a production-speed or resource defect.

The final ground release boundary now retains the full swept XYZ checks, native `Units.canCreate`, server-only `unit.add`, `unit.unloaded`, `UnitUnloadEvent` and `Units.notifyUnitSpawn`. It omits only the superseded XY occupancy veto and native random birth impulse; the existing safe birth velocity/altitude initialization still precedes notification. Repeated dump of an already added actor cannot announce a second birth. Blocked exits retain their payload and paid production state; no recipe is consumed again.

Restored and externally supplied native conveyor/router payloads previously bypassed the factory wrapper because those blocks use `item`, not `PayloadBlockBuild.payload`. Narrow native conveyor/router subclasses now wrap at `handlePayload`/`updateTile`; the original scheduler, accept rules, routing and transfer synchronization remain in control. Saved payloads use the same wrapper. There is no per-frame full-world scan or new queue. Custom third-party payload subclasses are not replaced. Upper transport retains its existing layer-owned native adapters and full XYZ release path. Warning overlays identify blocked releases; boats still wait at dry exits and cannot spawn on a dry upper slab.

Real native production, reconstruction and small payload chains are covered by the regression suite. Ground/Flying chains release on Z0/Z1/Z5, and production/reconstructor fixtures cover all native plans, rotations and Z0/Z1/Z3/Z5. These adapter fixtures do not grant new placement permissions or water navigation.

## Models and live displays

All 14 requested blocks have explicit cached profiles: Item Source/Void, Liquid Source/Void, Power Source/Void, Payload Source/Void, Repair Point, Repair Turret, Payload Conveyor, Payload Router, Tiled Logic Display and Target Dummy. See `A2-16o-MODEL-COVERAGE.md` and the complete native CSV. Source configuration colors, native payload identity/position, repair target/rotation/strength, and actual dummy counters remain authoritative. Rendering never produces items, heals targets or spawns a dummy actor.

Actual screenshot inspection caught transparent side-material samples in the initial utility prototype. Each model now selects an opaque original-atlas swatch once at cache creation, preserving its roof art and solid visible sides. An actual side-view GPU pixel test fails on the initial prototype and passes on the final renderer; mock opaque atlases alone could not establish this.

Tiled screens use the existing native shared framebuffer. Each tile samples its native 32-pixel slice with the original outer-frame crop and connected bezel. Whole-canvas UV reuse would repeat the same image on every panel. The upper proxy also now marks native regrouping dirty on actual proximity changes: its common lifecycle guard suppressed the native `onProximityAdded` dirty flag. Native `TileableLogicDisplay.linkDisplays` runs against layer-local proximity; no cross-floor canvas links or new display instruction interpreter are introduced. Native display size limits remain enforced. Queued contents update their existing framebuffer; unchanged contents cause no upload/redraw.

The native-family generator contained stale `canControl=false` overrides for continuous/laser turrets, while the accepted generated source already had the correct control guard. This was caught by the Meltdown regression during regeneration. The generator is synchronized with that accepted guard; previous turret behavior is preserved.

## Settings

Two independent normal persistent FPV preferences:

- **3D world draw distance:** 64–16,384 tiles, default 8,192. It limits the render pass's far plane and restores the camera afterward, including exceptional exits. Existing map/background reach remains the maximum.
- **3D high-detail distance:** 20–240 tiles, default 80. It scales existing terrain/detail-cache and mechanical/logistics/drill LOD thresholds. Budget caps remain. Unit families without an existing distance LOD are not redesigned.

Neither preference changes aiming/picking range, simulation, construction, support, costs, networking or save formats. Default detail thresholds match the accepted implementation. Defaults keep normal maps visible to their previous extent.

## Standalone workflow and evidence

Primary host is the owner's clean **official standalone Mindustry 160.7**, `modifier=release`, with its bundled **Java 25.0.1**. Java 17 compatibility uses the explicitly selected complete **17.0.20.1** JDK. No Steam game/client launch, AppID file, DLL replacement or second SDL instance is used. Every native graphics run has its own new profile, mod copy, temporary extraction directory, logs, screenshots and process manifest. See `A2-16o-STANDALONE-SETUP.md` for discovery, hashes and commands.

Validation details and measurements are recorded in `A2-16o-TEST-MATRIX.md` and `A2-16o-VALIDATION.json`. Actual OpenGL screenshots/API checks are automated Windows host tests, separate from physical owner acceptance. Both Java modes use the same diagnostic JAR; no game binaries are redistributed. Third-party/GPL notices are retained in the JAR and test kit. The corresponding-source ZIP is source, not an importable mod.

Developer commands from the repository (choose fresh run names):

```powershell
python tools/build-diagnostics.py --task a2-16o --run my-build
python tools/test-diagnostic-runtime.py --build local/a2-16o/my-build --java bundled25 --run my-java25
python tools/launch-standalone.py --run my-gallery25 --fixture tools/java/SandboxVisualHarness.java --helper tools/java/SandboxGpuChecks.java --property fpv.utilityGpu=true
python tools/launch-standalone.py --run my-gallery17 --java java17 --fixture tools/java/SandboxVisualHarness.java --helper tools/java/SandboxGpuChecks.java --property fpv.utilityGpu=true
```

The launcher uses the latest successful diagnostic pointer unless `--jar` is explicit. Linux developers must select their own standalone 160.7 SDK and complete JDK through `FPV_MINDUSTRY`/`FPV_JDK` or the ignored local configuration; the Windows reference installation is not a Linux runtime. Compiler tool names use the host platform. The tested player JAR needs neither Python nor a separately installed Java runtime.

## Limitations

- Physical owner controls/import/restart, physical Linux graphics/mouse, current physical campaign transitions, and real two-computer production/payload acceptance remain **NOT TESTED** in this pass.
- Dry/occupied/unsupported exits intentionally wait; no teleport fallback or artificial water platform is added. Custom payload subclasses keep their own behavior.
- Carried payloads retain lightweight native icon presentation; the visible actor takes over after real release. A fully volumetric carried-unit model is outside this cleanup.
- Existing static framebuffer pixels are not a new save format. Saved processor programs redraw as in vanilla. Initial/joined panel drawing is tested; preserving a one-shot image through every canvas resize/split is not separately certified.
- The older broad support-effects smoke helper fails its server repair-snapshot assertion in this paused standalone fixture on **both the accepted baseline and the modified build**. Focused actual Repair Point/Turret HP/start-stop tests pass at Z0/Z1/Z5. No speculative combat/network fix was made for that separate harness failure.
- Existing Windows windowed presentation issues remain a known environment limitation; fullscreen remains recommended for owner testing. No FPS improvement is claimed from these model changes.

Follow the three actions in `A2-16o-MANUAL-TEST.md`. The configuration checkpoint and final A2-16o checkpoint are local only.

## Local artifacts

- `local/a2-16o/test-kit/mindustry-fpv-a2-16o-diagnostics.jar` — one importable Windows/Linux diagnostic; SHA-256 `4e7d9d1fc9808eb33c9f3396e129101291af3a90f66857c2a3dfa1220f1f84ee`.
- `local/a2-16o/test-kit/mindustry-fpv-a2-16o-corresponding-source.zip` — tracked source snapshot from the final local checkpoint; no history, game, profile or build output.
- `local/a2-16o/test-kit/SHA256SUMS.txt` — kit checksums, including source and selected evidence.
- `local/a2-16o/mindustry-fpv-a2-16o-test-kit.zip` — convenience test kit, not a standalone game.

Final results: **207/207 Java17 + 207/207 bundled Java25 regression programs**, **25 OpenGL scenes / 1,125 frames / 40 focused GPU/repair checks per runtime**, zero GL errors. All **1,070 rebuilt FPV classes** match the JAR byte-for-byte. The **98 reference game files** and **2,194 previous artifacts** retain their hashes. Physical acceptance remains separate.
