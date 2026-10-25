# A2-17a — validation matrix and measurements

Baseline `ff9c21f`, final diagnostic JAR SHA-256 `3e02d130186749bc1a1aaacad10f736e5ff7fd41f67bb3fea795fb9b8474c1c2`. Windows x64 standalone **Mindustry 160.7 release**; complete **Java 17.0.20.1** and stock bundled minimal **Java 25.0.1** (no `java.logging`). Steam is deliberately not launched. Both runtimes use the identical player JAR; explicit developer-only SmokeHarness overlays drive disposable scenes.

| Validation | Result / evidence |
|---|---|
| Full build + Java17 suite | **PASS 212/212 programs**, `local/a2-17a/final/build-result.json`, `local/a2-17a-final-build.log` |
| Complete stock Java25 suite | **PASS 212/212**, 0 failures; `local/a2-17a/final/standalone25-tests.{json,log}` |
| Pure geometry invariants | **PASS 2,237,745 checks**: bounds, canonical tolerance, chunk apron, UV limits, deterministic roofs/risers, hidden-face culling, flat roof merging, world-height texturing of 18-tile cliffs |
| Native style/classification/Rules/3D ray | **PASS 16 checks**: live style, exact unchanged field/revision, artificial footprint, native rules and ray distances |
| Local setting/default/fallback/distance independence | **PASS** `MountainStyleTest`; real settings-file write/readback also passes in each graphics run |
| All styles × four maps × low/high view | **PASS 24 scenes / 4,320 frames / 11,565 assertions per runtime**, zero GL errors; `local/standalone-runs/mountains-verified{17,25}` |
| Native SaveIO with real Z1/Z3 platforms and upper walls | **PASS** after scene cycle on each runtime; world/native tile fingerprint, mountain height and upper component count retained; disposable `.msav` only |
| Actual native fog reset/reveal/hide + terrain rays in all three modes | **PASS 6 scenes / 120 frames / 259 assertions per runtime**, zero GL errors; `mountains-fog-final{17,25}` |
| Runtime visual determinism | Identical cached vertex counts/bytes/chunk counts on Java17/25 for all 24 matching cases; pure geometry also matches independent fresh samplers |
| FPV/Build Mode input and actual native motion API | **PASS**, `mountains-verified{17,25}/input-api.txt`; no synthetic physical keyboard acceptance claim |
| Construction, upper adapters, 3D selection/copy/paste, priority/pause, combat, networking and campaign lifecycle | **PASS** existing programs in both 212-program suites; no protocol/save-format/physics source modifications |
| Original Smooth mesh method and shader strings | Exact source equality against accepted checkpoint for `terrainTile`, all four renderer shader strings unchanged |
| Previous diagnostics/releases and clean host | **PASS** 2,271 previous artifacts + 98 reference files retain hashes |
| Physical owner import/controls/appearance; physical Linux GPU/input | **NOT TESTED** in this pass |
| Independent two-computer multiplayer/campaign acceptance | **NOT TESTED**; automated determinism and regressions are separate |
| GPU time / FPS acceptance | **NOT MEASURED**; CPU submission timings below do not certify GPU frame time or FPS |

## Scene and measurement method

Synthetic maps: sparse **128²**, mountain-heavy **160²**, mixed snow/sand/rock/shore **256²**, artificial-heavy **128²**. Three styles use identical camera positions, map membership, textures and settings. Native artificial walls/factories, ore, trees, liquids and actual upper structures are present. Natural mask holes remain open. Screenshot inspection covers low cliffs, broad tall formations, isolated rocks and mixed materials. The separate native fog fixture exercises cache reset and hidden height protection. These are automated game renders, not owner acceptance.

Viewport **960×600**, existing high-detail distance **80**, fixture cap **120 FPS**. Each case has 60 warm-up frames and 120 timed `FpvWorldRenderer.render()` CPU intervals. Every warm frame asserts zero terrain chunk rebuilds. Screenshot readback is before measurement; no GPU timer or synchronous GPU readback is inserted into the timed interval. Final Java17/25 graphics runs are serial and do not overlap the full regression suite or artifact hashing.

Cold generation is cumulative CPU time constructing/uploading the visible terrain chunks after a style change, not a single frame and not just the mountain algorithm. Cached vertices/bytes include the terrain batch's floor, ore and vegetation payloads. Bytes are logical vertex buffer payload, excluding Java/driver overhead and other world caches. Distant tier stays Smooth. Mode selection does not increase visible chunk count or create a draw call per cube; existing per-texture chunk batches/streams remain in use.

Single ordered trials include JIT/cache noise: lower CPU values in a later style are **not an FPS improvement claim**. The meaningful tradeoff is additional near geometry/cache payload and cold rebuild work, with stable warm caches. Raw mean/median/P95, chunk counts and both runtimes are in `A2-17a-PERFORMANCE.csv` and each run's `measurements.csv`.

## Java25 median renderer CPU (ms)

| Map / view | Smooth | Blocky | Hybrid |
|---|---:|---:|---:|
|sparse / low|0.684|0.376|0.362|
|sparse / high|0.453|0.525|0.662|
|mountain-heavy / low|1.718|1.699|1.782|
|mountain-heavy / high|2.245|2.227|2.200|
|mixed-biome / low|1.207|1.189|1.246|
|mixed-biome / high|3.088|3.065|3.113|
|artificial-heavy / low|0.767|0.974|0.709|
|artificial-heavy / high|1.124|1.005|0.916|

## Java25 cache and cold generation (Smooth / Blocky / Hybrid)

| Map / view | Cached vertices | Vertex payload MiB | Cold chunk generation ms |
|---|---:|---:|---:|
|sparse / low|77,592 / 144,918 / 121,521|2.66 / 4.98 / 4.17|16.99 / 16.66 / 11.18|
|sparse / high|83,454 / 131,691 / 119,310|2.87 / 4.52 / 4.10|5.45 / 5.42 / 6.08|
|mountain-heavy / low|77,460 / 313,956 / 248,418|2.66 / 10.78 / 8.53|5.32 / 13.67 / 17.32|
|mountain-heavy / high|102,420 / 345,654 / 271,953|3.52 / 11.87 / 9.34|4.36 / 15.46 / 16.76|
|mixed-biome / low|90,666 / 320,487 / 240,234|3.11 / 11.00 / 8.25|5.61 / 12.53 / 13.63|
|mixed-biome / high|158,490 / 192,810 / 176,214|5.44 / 6.62 / 6.05|7.96 / 7.92 / 7.63|
|artificial-heavy / low|79,956 / 180,630 / 164,823|2.75 / 6.20 / 5.66|3.61 / 14.23 / 10.37|
|artificial-heavy / high|85,218 / 155,058 / 143,775|2.93 / 5.32 / 4.94|3.42 / 7.94 / 8.53|

Eight authentic side-by-side panels are in `local/a2-17a/comparisons/` and the local test kit, with original screenshots retained. `tools/terrain-comparison.py` only arranges those images and labels; it does not synthesize game imagery. PIL is optional developer tooling for that contact sheet, not a player/runtime dependency.

## Excluded early attempts and remaining limits

An early test source was in the wrong package for the existing package-private native bootstrap; compilation stopped and was corrected before final builds. The first upper-platform fixture put a platform inside the mountain-heavy map's real terrain and was correctly rejected; moving only the disposable fixture to open ground fixed it. Early fog fixtures needed native team/fog initialization after reset and initially rejected the valid flat-ground ray fallback. No runtime rules or picking changes were made to force those fixtures to pass. Prototype and failed-run evidence is retained and excluded from final measurements.

The 0.35-tile visual/collision discrepancy, canonical chunk apron, Smooth far LOD and unchanged Background are intentional scope limits. Pixel coverage on physical Linux and subjective texture/ledge quality still need owner testing. Stronger steps, fully voxel collision, excavation and a whole-map/background conversion are not implemented.
