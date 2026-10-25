# A2-18 — Release readiness

**READY WITH KNOWN LIMITATIONS — local candidate; owner acceptance and publication approval pending.**

Candidate: **Mindustry³ Alpha 2.0.0 RC1 [v160.7]**, internal version **0.19.0**, proposed tag **v2.0.0-rc.1**. Feature freeze: no gameplay/model/network/camera/physics change was made. Baseline `3fc6f3ea6d8f2dda8931559f2498938dae51d420` on main includes accepted A2-16 families, A2-17a mountain styles and the latest repair-beam/placement-arrow fix. All **1,081 FPV classes are byte-identical** to the accepted build; only runtime mod.hjson changed. Protocol 4 and saves are retained. Different payloads still cannot bypass strict identity checking.

The final release checkpoint is recorded by hash in the adjacent distribution `release-manifest.json` and corresponding-source `SOURCE-MANIFEST.json`. A document cannot contain its own future commit hash. No push, publication, tag, Workshop update or history rewrite is performed. Previous Alpha artifacts and the reference standalone game are preserved.

## Confirmed release-preparation findings and corrections

- Old packaging injected Alpha 1.0.4 title/tag/browser notes into new versions. It now accepts an explicit title/tag, copies current A2-18 notes and hashes the release manifest as well as assets. Existing directories/versioned builds cannot be overwritten.
- Player README/installation/version/controls were stale; they now describe Alpha v2, normal import/restart, all distribution boundaries and current acceptance status.
- The cross-runtime test runner assumed diagnostic filenames and an existing temp directory. It now accepts the actual production JAR and creates its own temp directory. Its first Java 25 attempt failed two campaign fixtures solely on missing temp-directory creation; the full corrected rerun passed 214/214.
- The new import observer initially assumed native import retained a .jar extension. Mindustry correctly normalizes to .zip without changing bytes. The observer was corrected and fresh import/restart tests passed. No runtime installation fix was needed.

## Build, independence and licenses

The production JAR is 4,754,805 bytes, 1,261 entries. SHA-256: `050cdf7b6b8c5f59969be89c53411a0cc92787e535a28385344ec973406610a3`.
Canonical payload: `payload1-c159bad5421a1651d25065dbb65adb2ea075ed9e0d6f747c5925960c6cc53979`.

Root/source/JAR/Workshop metadata are identical. No host Arc/Mindustry classes, game executable/JRE, saves, profiles, credentials or development binaries are packaged. No tracked file exceeds 50 MiB. Runtime path search found no actual developer directory dependency (one diagnostic text ending in `mounts:\n` was a regex false positive). Required mod classes/resources/design descriptors and GPL/JNA/Apache/libffi notices are included. Textures/assets and Arc/SDL/native window libraries come from the separately installed supported game. JNA 5.19.1 is bundled; complete Java17 uses it, reduced Java25 selects reflected java.base FFM. Linux reuses the already mapped host SDL bridge; unsupported capture retains a bounded visible HOST MOUSE fallback. Players need no compiler, Python, JDK, external texture folder or second SDL instance.

Repository-history privacy note: 6 existing GitHub-created commits have a real-name author label with a noreply email. No personal email was observed in the identity audit. This task does not rewrite that history; the corresponding-source ZIP excludes .git. The owner should review whether the already existing public identity label is acceptable before a future push. The new checkpoint uses the approved public username/noreply identity.

Existing main-source diagnostic classes remain in the payload because production references their opt-in gates. Network fixtures return immediately with no explicit role; other smoke/profile harnesses require explicit fpv.* properties. Test overlays under tools/java and their screenshots/logs are **not** in the player JAR. Normal-startup tests used no fpv.smoke flags. Fresh profiles cannot prove independence from every possible clean-machine driver; that physical boundary remains explicit.

## Actual validation

- Full Java17 regression build: **214/214**. Same production JAR under stock reduced Java25.0.1: **214/214**, zero final failures. Both campaign fixtures also pass after the runner fix.
- Four normal startup checks: JAR native import plus real JVM restart and compiled Workshop folder, each under Java17/25. One mod only, installed byte-identical payload, class origins inside isolated profiles, verified identity, no fatal errors. Folder native publisher prerequisites and 512×512 preview decoding pass. Parent classpath contains the host and a test observer only, no FPV/JNA/developer classes; this is not a physical menu click test.
- A disposable copy of the original Packr EXE also launched the RC renderer fixture on stock Java25: three views/90 frames, native input/movement and live-screen checks, zero GL errors. Original EXE/JRE remain unmodified.
- Actual standalone OpenGL: **174 viewpoints / 9,729 frames per runtime**, zero errors; native repair HP/beams, directional previews, live logic displays, terrain styles/fog/save/readback, aircraft/exhaust, utility/payload/LOD and turrets. Native input/movement APIs pass; physical capture/keyboard acceptance is separate. Not every cosmetic combination was freshly rendered; CPU fixtures and accepted unchanged-bytecode history complement the galleries.
- A real dedicated server and two local clients (JAR + Workshop folder) pass the narrow naval production fixture: READY, server-owned single release/cost, both-client XYZ and GPU state. This is one Windows PC, not two-computer or internet acceptance.
- Full historical network smoke FAILS **before connection**: 13 production channels plus four test channels exceed the configured 16. This is an opt-in diagnostic capacity defect, not a demonstrated gameplay packet failure. No channel bound/protocol relaxation was shipped; full scenario acceptance remains pending.
- Existing construction/priority/pause/volumetric clipboard/production/reconstruction/payload/save/campaign/identity/prediction tests pass. Campaign tests use disposable data and native SaveIO, not the owner's primary progress or a physical planet-transition UI.
- Current official indexer methods plus installed 160.7 release-selection classes: **9 checks pass**, including prerelease/draft/revision handling. Standalone resolution/safety Python tests: **6 pass**. Public index still points to the prior 0.18.31; this RC is not advertised as indexed.

## Performance: measured, not an FPS claim

Windows RTX4050 Laptop GPU, NVIDIA617.42, actual OpenGL4.6, standalone960×600, full Java17. Identical deterministic128×128 scenes; 120 warmup +600 samples per scene in the longer runs, 599 available asynchronous GPU queries. An initial 60+180-frame pair is retained as well. Both orders were tested serially without the regression suite or multiplayer clients running concurrently. Baseline and RC use identical gameplay/renderer classes and fixture source. Native Logic.update is timed separately; render CPU measures the production renderer submission, GPU queries never block. Loop FPS includes the host/fixture cadence and settings cap and **is not physical presentation FPS or an uncapped gameplay benchmark**.

| Scenario | Render CPU ms baseline → RC | GPU ms baseline → RC | Native update ms baseline → RC | Loop FPS baseline → RC | RC alloc KiB/render | RC load ms |
| --- | --- | --- | --- | --- | --- | --- |
| empty | 0.620 → 1.424 | 0.071 → 0.106 | 0.342 → 0.379 | 236.4 → 238.5 | 19.4 | 76.7 |
| industrial | 0.676 → 0.689 | 0.125 → 0.130 | 0.274 → 0.273 | 145.4 → 149.6 | 22.9 | 56.2 |
| dense-factory | 1.736 → 1.977 | 5.205 → 4.895 | 0.226 → 0.244 | 120.0 → 120.0 | 36.4 | 31.5 |
| mountains | 1.052 → 1.117 | 0.220 → 0.218 | 0.198 → 0.222 | 120.0 → 120.0 | 105.6 | 37.0 |
| flying-256 | 2.696 → 2.728 | 4.332 → 4.373 | 0.483 → 0.510 | 120.0 → 120.0 | 50.9 | 22.4 |
| multi-floor | 0.462 → 0.523 | 0.070 → 0.070 | 0.180 → 0.183 | 120.0 → 120.0 | 25.1 | 106.6 |
| power-liquid | 8.428 → 9.122 | 7.500 → 8.141 | 0.178 → 0.188 | 94.5 → 88.8 | 134.6 | 40.5 |

| Scenario | Heap MiB baseline → RC (no forced GC) | Cached terrain MiB baseline / RC |
| --- | --- | --- |
| empty | 96.4 → 139.7 | 3.06 / 3.06 |
| industrial | 150.4 → 87.8 | 3.01 / 3.01 |
| dense-factory | 83.0 → 143.8 | 2.86 / 2.86 |
| mountains | 75.3 → 71.5 | 3.51 / 3.51 |
| flying-256 | 149.4 → 147.5 | 3.06 / 3.06 |
| multi-floor | 108.6 → 126.9 | 3.16 / 3.16 |
| power-liquid | 129.4 → 116.6 | 3.89 / 3.89 |

The dense power/liquid test contains hundreds of router/pump/node/battery instances; it is the slowest case in both builds. CPU submission and per-render allocation remain similar overall, but the long RC empty scene measured 1.424ms vs0.620ms baseline while the short pair measured 0.741ms vs0.688ms. The short multi-floor loop also varied ~236→180 FPS; both long runs settled at ~120. These unsuccessful/inconsistent cases are retained, not removed. GPU values differ across short/long capped runs as well; no controlled GPU-clock/thermal or statistically significant speedup claim is made. The identical class bytes provide no evidence of an introduced renderer algorithm regression. This release does not optimize the existing ~135KiB/render dense liquid allocations.

Heap samples are instantaneous post-scene values without forced GC and cannot establish a leak. Terrain cache bytes match exactly in each comparison. World load is fixture generation plus renderer creation, excluding warm mesh creation; warm/rebuild timing and cache tests are separate. Normal whole-game initialization logs: 5,682ms Java17 JAR and 4,715ms Java25 JAR in this run; these are not isolated mod-init timers and were recorded outside the performance comparison. Dedicated mod initialization time, total draw-call/triangle instrumentation, VRAM and long-duration leak measurements are NOT AVAILABLE / NOT TESTED.

World/reset hooks clear pilot, combat/projection/altitude state; shutdown releases input/cursor, network, construction, renderer/chunk/model/display caches. Actual world transitions, settings persistence, native save/load and normal process exits were exercised. A multi-hour campaign/reconnect/GL resource soak is not certified. No confirmed severe leak/crash was found in these tests; no speculative optimization was added.

## Findings by severity

| Severity | Finding | Disposition |
| --- | --- | --- |
| BLOCKER | None confirmed in the RC runtime/package validation | Any new crash/save corruption/critical construction/production/state-corruption failure found by owner blocks publication. |
| MAJOR, known hardware limitation | Severe Windows windowed-mode presentation stalls from earlier owner reproduction | Fullscreen is the accepted workaround on that PC. A2-18 does not claim a windowed fix or universal hardware acceptance. |
| Validation gate | Physical Steam RC import/input; Linux native capture/compositor; both Windows/Linux host directions; long campaign | NOT TESTED for RC. Follow OWNER-ACCEPTANCE before release approval. WSL inventory returned Access denied; no bypass used. |
| MINOR / diagnostic infrastructure | Full historical network fixture exceeds test channel allowance | Preserve failure; narrow actual network fixture passes. No gameplay capacity change made. |
| MINOR / known Alpha presentation | Residual clipping, LOD/material/background approximations, unsupported third-party block details | Owner cosmetic review; no critical geometry obstruction confirmed here. |
| DEFERRED | Alpha v3 UI, blueprint paper, shaders/filters/seasons, Alpha v4 FPV expansions | Feature freeze; not implemented. |
| UNSUPPORTED | Android | Not a release requirement or inferred from a Linux launch. |

## Readiness boundaries and owner actions

GitHub JAR/ZIP/corresponding-source and compiled Workshop packages are locally prepared from one clean checkpoint, with SHA-256 and fresh source-class rebuild verification. See DISTRIBUTION-CHECKLIST for exact files/provenance and postpublication checks. Root metadata already permits Browser discovery and references the correct repository; actual new-release indexing/download cannot be verified before publication. Neither package readiness nor this status authorizes publishing.

Owner's next three actions: (1) clean Windows standalone and Steam import/restart + controls/build/save smoke; (2) Linux capture/focus/menus/save with the same JAR; (3) two laptops on LAN, each hosting in turn, including combat, multi-floor building/copy, reconnect and host save/rehost. Detailed steps and failure-stage diagnostics are in A2-18-OWNER-ACCEPTANCE.md. No third-party tester or physical remote session is claimed.
