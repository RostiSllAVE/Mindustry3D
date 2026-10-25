# A2-16o polish — test matrix and measurements

Host: clean standalone Mindustry **160.7 release**, Windows x64, 960×600 isolated graphics fixtures; no Steam launch. Complete Java **17.0.20.1** and bundled minimal Java **25.0.1**. Identical candidate JAR SHA-256 `eb672bf4b72c59d7e9a44e8feeae08ad0871f87fe2e64948e2da004a14b1cb92` on both runtimes. Physical acceptance is separate.

| Check | Actual result | Evidence under `local/` |
|---|---|---|
| Full build/preflight + Java17 regressions | **PASS 209/209 programs** | `a2-16o-polish/candidate1-build.log`, `candidate1/build-result.json` |
| Same complete suite on stock standalone Java25 | **PASS 209/209 programs**, 0 failures | `a2-16o-polish/candidate1/standalone25-tests.json` and `.log` |
| Repair Point/Turret, Parallax, Segment, native pixels/yaw/cache/floors + actual repair HP | **PASS 79 checks per runtime**, Z0/Z1/Z5 | `standalone-runs/turret-polish-gpu17/gpu-checks.txt`, `turret-polish-gpu25/gpu-checks.txt` |
| Existing seven-turret GPU suite: native part animation, yaw, +55°/-35° pitch, recoil, Salvo wings, alpha art, depth occlusion, no combat mutation, cache reuse | **PASS 179 checks per runtime**, Z0/Z2 | Same GPU result files and `turret-model-*.png` |
| 14-block top/oblique/side gallery + actual repair platforms Z1/Z3/Z5 | **PASS 45 scenes / 1,350 frames per runtime**, 0 GL errors | `standalone-runs/turret-polish-gpu17`, `turret-polish-gpu25` |
| Flare, Zenith, Antumbra, Mono, Oct, Quell, Disrupt: top/side/rear/bank/FPV-other; own FPV; dead unit; 50-aircraft scene | **PASS 38 scenes / 1,140 frames / 2,310 assertions per runtime**, 0 GL errors; all 50 fleet engines asserted | `standalone-runs/exhaust-polish-final17`, `exhaust-polish-final25-cap` |
| Pure/native new geometry tests | **PASS 8,222 shell checks + 539 engine checks** on each runtime | Full regression logs: `TurretBodyPolishTest`, `FlyingEngineVisualsTest` |
| Native FPV/Build Mode, mouse/input delegation, queue preservation, motion integration; existing live-screen GPU regression | **PASS** API checks and 42 GPU checks, 90 rendered frames, 0 GL errors | `standalone-runs/polish-input-api25/result.json` |
| Construction, upper proxies, 3D selection/copy, campaign/SaveIO, costs, combat, connections, prediction, protocol/identity regressions | **PASS** existing programs in both 209-program suites | Complete test logs; these are automated fixtures, not remote gameplay |
| Existing releases/diagnostics and reference host preserved | **PASS**, 2,237 artifacts + 98 reference files unchanged | `a2-16o-polish/preserved-verification.json` |
| Physical import/restart, controls, subjective appearance | **NOT TESTED** by owner during this pass | Three actions in manual test |
| Physical Linux rendering/mouse; independent two-computer session | **NOT TESTED** | Portable Java code paths and regression tests do not certify physical acceptance |
| GPU timer/FPS comparison | **NOT MEASURED** | CPU measurements below are not FPS or GPU time |

## Geometry comparison

`TurretPolishGeometryProbe` runs the same native definitions/atlas and shipping renderer on accepted A2-16o versus candidate. Each type has a fresh model cache. A second warm render asserts unchanged cache bytes/build count. Submitted vertices include the original base, native overlays and the current head; there are three vertices per triangle. Bytes are cached CPU float arrays, excluding Java object overhead and GPU buffers, not total process memory.

| Block | Submitted triangles before → after | Cached CPU bytes before → after |
|---|---:|---:|
| Arc | 456 → 640 | 47,616 → 90,240 |
| Lancer | 456 → 636 | 47,808 → 90,048 |
| Meltdown | 484 → 704 | 49,920 → 98,688 |
| Wave | 456 → 640 | 56,832 → 97,920 |
| Tsunami | 484 → 680 | 59,520 → 104,448 |
| Hail | 444 → 628 | 45,888 → 87,360 |
| Ripple | 452 → 640 | 46,848 → 89,472 |

Evidence: `standalone-runs/polish-geometry-before/turret-geometry.csv` and `polish-geometry-after/turret-geometry.csv`. The new CPU cache retains the shared original shape plus its alternative lower-shell template; it does not draw both copies. The GPU checks use **one atlas stream**, not a draw call per subpart. Auxiliary and ordinary turret geometry is reused between instances. No new materials/textures.

Engine volumes use the existing cached octagonal template: 96 submitted vertices per section. Near LOD is 3 sections/engine (288 vertices, 96 triangles), far LOD one section/engine. Fifty visible single-engine Flares therefore add **14,400 vertices / 4,800 triangles** in the existing dynamic emissive stream; no new mesh, entity or texture per engine. Existing stream capacity flushes remain applicable.

## CPU comparison

Serial before/after runs, two repetitions, same paused map, 50 visible Flares, same camera and JAR-independent fixture, Java25. Each run has **100 warm-up + 500 measured frames**; only the production `FpvWorldRenderer.render()` CPU interval is timed. No synchronous GPU readback occurs inside that interval. Screenshots occur before samples. No full regression suite or large artifact hash check ran during these final measurements.

| Run | Mean CPU ms | Median CPU ms | P95 CPU ms |
|---|---:|---:|---:|
| Accepted A2-16o, repetition 1 | 0.5874 | 0.5506 | 0.8967 |
| Candidate, repetition 1 | 0.7938 | 0.7324 | 1.1981 |
| Accepted A2-16o, repetition 2 | 0.6019 | 0.5545 | 1.0052 |
| Candidate, repetition 2 | 0.8599 | 0.8182 | 1.3743 |

Measured mean cost is **+0.2064 / +0.2580 ms CPU per frame** for these 50 new near-LOD effects. This is additional visual work, not an optimization/FPS claim or certification for thousands of aircraft. Raw samples and summaries: `standalone-runs/polish-bench-clean-{before1,after1,before2,after2}/{render-times.csv,benchmark.json}`. Earlier exploratory timing runs are retained but excluded because background artifact verification overlapped them.

## Failed/excluded fixtures

The first fleet test encountered the native eight-Flare unit cap. That fixture—not production rules—now sets its disposable map cap to 1,000 and asserts all 50 real actors/engines. Failed `exhaust-fleet-debug*` and `exhaust-polish-final25` evidence remains on disk. Earlier prototype galleries/exhaust runs are exploratory, not final-binary acceptance.

The previously documented broad paused support-effects helper retains its baseline repair-snapshot failure. The focused real HP tests pass with both repaired heads, and no unrelated healing/network change was made to hide the old harness failure.
