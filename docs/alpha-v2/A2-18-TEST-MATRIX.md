# A2-18 — Test matrix

RC: internal **0.19.0**, public **Alpha 2.0.0 RC1 [v160.7]**. Baseline `3fc6f3e`.
PASS below is scoped to the evidence column. It does not imply a physical keyboard/menu/network test. Final source commit and artifact hashes are in the distribution manifests.

| Check | Result | Evidence / boundary |
| --- | --- | --- |
| Production compile/design preflight/full Java 17 suite | PASS | 214 executable regression programs; build-java17.log. Player JAR uses the same compiled main classes. |
| Production-JAR suite on reduced stock Java 25.0.1 | PASS | 214/214, zero failures; a2-18-stock25-final-tests.json. Only java.base/jdk.unsupported supplied by this host. |
| First Java 25 test attempt | FAIL, test infrastructure corrected | 212/214: two campaign tests could not create their temporary directories. Runner now creates build/tmp; full rerun 214/214. No campaign runtime fix required. |
| Native JAR import → new JVM, Java 17 and 25 | PASS | Real Mods.importMod API, restart, one installed mod, unmodified payload, all FPV class origins inside disposable data/mods. Not physical Import menu input. |
| Native import observer first attempt | FAIL, test observer corrected | Host legitimately normalizes the imported JAR filename to .zip; old observer looked only for .jar. Actual import/restart already passed; fresh rerun validates unchanged bytes. |
| Compiled Workshop folder startup on Java 17/25 | PASS | Native prePublish prerequisites, preview decode, normal init and canonical fingerprint. No Steam upload/subscription. |
| Disposable original standalone EXE copy | PASS automated | Actual Packr launcher/JRE, three viewpoints/90 frames/live screens/input APIs; test-only overlay, reference unchanged. |
| Java 25 missing-java.logging startup regression | PASS | Win32/java.base FFM selected, settings/network/3D initialization retained; no startup exception. |
| Java 17 native input compatibility | PASS | Win32/JNA selected and native API regression suites. Physical continuous capture/focus behavior pending. |
| Strict payload/version/protocol checks | PASS | Fingerprint/protocol/readiness/forged/stale/duplicate/reorder tests; JAR/folder live fingerprints match. Changed payload rejection fixtures retained. |
| Physical standalone RC Import menu/controls | NOT TESTED | Automated native API/startup and graphics are separate from owner acceptance. |
| Physical Steam RC Import → restart → F5 | NOT TESTED | No Steam game launched during development. Same reduced runtime tested through standalone, not a Steam session. |
| Physical clean Windows computer | NOT TESTED | New profiles on this PC do not certify a different clean machine. |
| Linux startup/X11/Wayland/mouse capture | NOT TESTED physically | Portable Java tests on Windows exercise Linux fixtures, not Linux native libraries/compositor. WSL inventory returned Access denied; no bypass attempted. |
| FPV/Build APIs, input gates and native unit integration | PASS automated | InputSmokeChecks/UnitMotionSmokeChecks plus CPU camera/input/controller/death/ownership fixtures. No synthesized physical input. |
| Acro/Stabilized, prediction/snapshots | PASS automated | LocalPilotPrediction/PredictionNativeIntegration and bounded pose/receipt/epoch/order tests; remote latency not certified. |
| F3 interruption, resume and priority | PASS automated | ConstructionPauseNativeTest, ConstructionPriorityNativeTest and network-handler fixtures. Owner previously accepted earlier checkpoint. |
| F4 XYZ selection/copy/paste and normal costs | PASS automated | VolumeSelectionNativeTest, VolumeClipboardNativeTest/Liquid/Network, UpperConstructionNativeTest/Network; no new protocol/save format. |
| Structural support/collapse and conflicts | PASS automated | LayerConstruction/Actions/Runtime/Navigation and native adapter tests. Physical combinations still sampled by owner. |
| Ground/air/naval production and reconstruction | PASS automated | UnitProductionSpawn, UnitUpgradeProduction (448 actual fixture upgrade releases), NavalFactoryNative and upper-output obstruction fixtures. |
| Payload loading/transport/unloading | PASS automated | PayloadChainNative, PayloadReleaseRegression, LayerPayloadNative/LogicNative. No universal custom-unit claim. |
| Save/load, campaign generation/sector state | PASS automated | CampaignLifecycleNative, BackgroundCampaignNative, LayerCampaignNative and real SaveIO in GPU mountain fixture. Only disposable data. |
| Physical sector UI/planet transitions/long campaign | NOT TESTED for RC | Owner accepted earlier campaign checkpoint; repeat RC checklist on copied progress. |
| Repair beams/directional ghosts | PASS actual GL 17/25 | 33 scenes/1,584 frames per runtime, 3,654 checks, native HP and real beam pixels; Z0/Z1/Z3 and preview Z0/Z2/Z5. |
| Live logic displays/FPV/Build API | PASS actual GL 17/25 | Three viewpoints/90 frames; real display pixels/orientation and upper display checks. |
| Mountains, settings persistence and SaveIO | PASS actual GL 17/25 | 24 scenes/4,320 frames, 11,565 checks per runtime; low/high views, all three styles, native save/readback. |
| Fog/terrain interaction | PASS actual GL 17/25 | Six scenes/120 frames, 259 checks per runtime. |
| Aircraft weapons/exhaust | PASS actual GL 17/25 | 38 scenes/1,140 frames, 2,310 checks per runtime, multiple unit types/crowd. |
| Utilities/payload/live tiled display/LOD | PASS actual GL 17/25 | 25 scenes/1,125 frames per runtime, upper Z1/Z3/Z5, world/detail distance extremes; 40 extra real HP/display checks. |
| Turret models/aim/upper floors | PASS actual GL 17/25 | 45 scenes/1,350 frames per runtime plus geometry/GPU helpers. |
| Representative GPU gallery total | PASS | 174 viewpoints, 9,729 frames per runtime, zero reported GL errors; fixture overlays excluded from production assets. Not exhaustive cosmetic acceptance. |
| Seven representative performance scenes | PASS measurement | Separate serial baseline/RC comparisons, short 180-frame and longer 600-frame sampling; see readiness tables and CSV. FPS is fixture-loop throughput, not user gameplay/presentation FPS. |
| Full historical server/two-client network fixture | FAIL, diagnostic capacity limit | Stops before connection: 13 production channels + four lab channels exceed configured 16. No gameplay limit/protocol was relaxed. Does not prove a native multiplayer defect. |
| Narrow actual localhost server + two clients | PASS | Native naval production single-release/cost + both-client authoritative XYZ/GPU; protocol 4 READY. One client is compiled Workshop folder, one JAR. Three real processes, one Windows PC. |
| Real two-computer Windows host → Linux client | NOT TESTED | Mandatory owner gate. |
| Real two-computer Linux host → Windows client | NOT TESTED | Mandatory owner gate; distinguish binding/discovery/firewall/native join from FPV handshake. |
| Internet latency/independent third-party session | NOT TESTED | Local fixtures are not internet acceptance. |
| Reconnect/world epochs/save session recovery | PASS fixtures; physical pending | Protocol/readiness/campaign tests; not full cross-platform physical session. |
| Runtime dependencies/developer paths/host classes | PASS audit | No actual developer absolute file path in runtime; diagnostic prose false-positive reviewed. All required mod resources/licenses/JNA included; Arc/Mindustry supplied by host. |
| Artifact/source integrity and old artifacts/reference | PASS final verification | Final verify-release + source rebuild + preservation evidence; exact checkpoint in SOURCE-MANIFEST. |
| GitHub/Workshop/Browser actual new release delivery | NOT TESTED | Local packages ready only; explicit postpublication checklist required. No publication performed. |
| Android | UNSUPPORTED | Outside accepted desktop scope. |

Local evidence: `local/a2-18/`, `local/standalone-runs/rc-gpu17-*`, `rc-gpu25-*`, performance runs and `local/net-lab/a2-18-*`. Raw logs are intentionally ignored and are not player assets. A sanitized machine-readable summary accompanies the source documentation.
