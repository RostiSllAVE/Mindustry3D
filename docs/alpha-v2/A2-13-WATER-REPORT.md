# A2-13 — Water surfaces, shores and naval waterlines

Baseline: accepted A2-12 `0fa3d5d3c6bbb88d95085c8fdfcfc16d3cf03210`, existing `main`. Local diagnostic only. No public release, Workshop update, protocol, gameplay or save-format change.

## Investigation and implementation

The previous atlas shader modulated liquid brightness but left all liquid vertices at physical Z0. Native 160.7 liquid content and the existing floor/ore/naval render paths were inspected. Native shallow/deep water, tainted water, sand/darksand water, tar (oil), slag, pooled cryofluid and arkycite retain their original textures and palette. Deep/shallow distinctions remain in native art and gameplay; their visible free surfaces share a level.

`LiquidSurfaceVisual` supplies a **presentation-only −0.16 tile interior water level**. Each native liquid tile uses a cached four-triangle fan. A shared corner is lowered only if all four adjoining native tiles contain open liquid. Land, solid/occupied footprints and world edges anchor it to Z0. The tile centre is lowered only for open liquid. Thus shore cells form a restrained sloping basin, including isolated pools, rather than exposing holes between a floating flat sheet and land. A subtle up-to-4.5% native-colour edge highlight adds a shallow-water cue; no foam textures or transparency layers are introduced.

A2-11 natural relief is already anchored beside liquid/structures and remains unchanged. Artificial walls, geological cliffs and native placed buildings retain their existing passes. Mixed coarse shoreline cells keep actual per-tile surfaces and structures. Homogeneous open liquid cells plus a one-tile halo may use the existing larger coarse quad. Ore overlays on liquid conform to the same fan; land ore meshes and relief are unchanged. Liquid remains visible under elevated platforms.

## Waves and fallback

Two coherent world-space sine waves displace only liquid vertices in the **existing opaque atlas shader**. Maximum displacement is ±0.025 tile, with blended long wavelengths, no per-tile random phases, simulation/collision updates, water-tile draw calls or per-frame rebuilding of cached meshes. Material/wave weights are baked into the existing vertex colour alpha attribute; output framebuffer alpha remains1. No new per-vertex attributes, render target, native dependency or fluid simulation is added.

Water/tainted/shallow-water motion weight is1; cryofluid0.40; tar0.16; slag/arkycite0.22. These preserve each liquid's native identity instead of recolouring every liquid blue. Mixed-liquid corner weights use the lowest adjacent weight, sharing exact edge positions. Unknown/modded liquid floors retain their native texture and a static lowered fan. Missing atlas regions fall back to the native map colour and static fan.

Wave weights fade over two tiles at detailed-chunk boundaries. The distant coarse tier has static waves. This quiet apron joins arbitrary near/coarse chunk mixes without wave-driven T-junction cracks or camera-dependent mesh rebuilds. Small quiet strips are an intentional quality/performance compromise. Native per-tile floor textures remain visible; this does not redesign their tiling or implement continuous texture splatting.

**Setting:** Settings → FPV flight → **Animated 3D liquid waves (disable for lower-end GPUs)** (`fpv-water-waves`, defaultON). OFF immediately disables wave displacement, animated light modulation and matching ship bobbing, while preserving lowered surfaces/shore geometry. Shader branches skip the sine work when disabled. Mesh topology and its memory cost remain.

Animation uses Arc `Time.time / 60`. Given the same coordinates and time, the result is deterministic; it adds no saved or networked state. Local visual clock phases are **not synchronized between computers** and are not persisted as gameplay state. Static shoreline geometry is derived identically from native tiles. A fixed-time phase and surface hash reproduce after full native campaign SaveIO.

## Naval presentation

The ten existing native naval profiles keep their body geometry, textures, proportions, mount/recoil animations and physical poses. `NavalUnitRenderer` adds the sampled liquid presentation offset to the hull's existing waterline and attached world-rendered guns **only when afloat and within0.10 tile of its normal physical support**. Lifted/airborne ships keep their previous height. Their physical XYZ position, hitboxes, aiming, authoritative projectile origins, navigation and FPV camera are untouched. The small visual gun translation does not move the real muzzle; viewmodels/camera are not reworked in this pass.

The offset uses the same floor fan and wave function as the shader. Native Risso, Minke and Omura are present in actual graphics captures; all ten profiles have automated afloat/lifted/airborne checks. Unknown naval unit types retain their existing model dispatch. The distant static-wave tier can differ from a nearby ship's tiny visual wave sample; this is at most the0.025-tile ripple, not new buoyancy physics.

## Defects found during validation

The first actual graphics run found an invalid mixed-shore artificial-wall lookup: packed `Tile.pos()` had been passed to a linear tile-array callback. It is fixed to `x+y*worldWidth`, with a regression exercising a real metal wall inside a mixed liquid coarse cell. The repeat actual game run succeeds. The failed prototype JAR/logs remain isolated local diagnostics and are **not** the packaged candidate.

The first CPU benchmark also exceeded the mock streaming buffer while collecting all64 coarse chunks without a shader. The probe now clears CPU streams between chunks on **both** versions; production still uses its existing bounded streaming flush. This is a benchmark-fixture correction, not a gameplay change. Timings below come from the final candidate and corrected identical probe.

## Measured cost

`tools/benchmarks/LiquidMeshComparison.java`: identical compiled probe before/after, native128×128 map,64 chunks; full ocean,11-tile-wide river (1,408 wet tiles), and mixed4×6 patches (5,464 wet tiles), one synthetic atlas page. Five warm-up and seven timed passes, median/min/max; sequential processes without concurrent tests. Actual native Mesh counts/buffers through MockGL. **These are CPU rebuild/stream-assembly measurements, not FPS or GPU wave timings.**

| Runtime / map | Detailed rebuild64 before → after | After per chunk | Coarse assembly64 before → after |
| --- | ---: | ---: | ---: |
| java17 / ocean | 6.556 → 8.316 ms | 0.130 ms | 0.273 → 0.440 ms |
| java17 / river | 12.401 → 13.525 ms | 0.211 ms | 0.311 → 0.635 ms |
| java17 / mixed | 20.953 → 13.376 ms | 0.209 ms | 0.323 → 0.719 ms |
| steam25 / ocean | 5.137 → 7.642 ms | 0.119 ms | 0.324 → 0.633 ms |
| steam25 / river | 8.965 → 7.902 ms | 0.123 ms | 0.324 → 0.280 ms |
| steam25 / mixed | 8.012 → 7.920 ms | 0.124 ms | 0.198 → 0.649 ms |

| Map | Vertices before → after | Triangles before → after | Mesh bytes before → after | Material batches |
| --- | ---: | ---: | ---: | ---: |
| ocean | 98,304 → 196,608 | 32,768 → 65,536 | 3,538,944 → 7,077,888 | 64 → 64 |
| river | 188,160 → 196,608 | 62,720 → 65,536 | 6,773,760 → 7,077,888 | 64 → 64 |
| mixed | 163,824 → 196,608 | 54,608 → 65,536 | 5,897,664 → 7,077,888 | 64 → 64 |

Near liquid geometry doubles from6 to12 vertices/tile; dry land geometry is unchanged. The64MiB cache budget, six uploads/frame and A2-12 conservative96-vertex/tile selection estimate remain unchanged. Homogeneous distant oceans still aggregate; intricate shores need additional coarse CPU geometry. Existing distant geometry streams are assembled as before, but waves do **not** regenerate them.

Java17 warm ocean rebuild allocation drops from5,458.9 to1,362.9 bytes/chunk by avoiding a captured per-tile atlas lookup lambda in the new liquid path. River/mixed values and min/max are in the validation JSON. This excludes cold/growing mesh storage and native allocations; it is not a whole-frame allocation/FPS improvement. Steam25's optional management allocation counter is unavailable. Timings are noisy; apparent wins in individual mixed/river cases are not claimed as general optimization.

## Actual graphics evidence

Identical lab entry bytecode, cameras and settings run through **actual Mindustry160.7 SDL/OpenGL on stock Steam Java25** before/after. Only `fpv/SmokeHarness.class` is replaced in disposable copies; all production renderer classes and native atlas textures remain real. The distributable retains its original smoke entry. Both140-frame runs finish with **GL error0**.

The64×48 scene includes shallow/deep/sand water, four other liquid pools, land relief/copper ore, occupied foundations, natural mountains on a liquid border, an artificial wall,5 server-authorized lab-only Z2 platforms over water, and Risso/Minke/Omura. Low-angle and overhead images plus a second wave phase and waves-OFF image are direct, unedited screenshots in the kit's `gpu-evidence/`. Wave phase changes actual water pixels; the baseline already had brightness shimmer, so pixel differences alone do not establish geometric displacement. CPU/native tests, shader displacement and the actual rendered views are separate evidence.

Short warm samples:25 CPU submission samples/view,60 low and29 overhead asynchronous GPU samples; one small capped-window scene:

| Metric | Accepted A2-12 | A2-13 candidate |
| --- | ---: | ---: |
| Low CPU submission | 2.225 ms | 2.938 ms |
| Overhead CPU submission | 2.421 ms | 4.030 ms |
| Low GPU elapsed | 0.649 ms | 0.563 ms |
| Overhead GPU elapsed | 1.563 ms | 2.345 ms |
| Retained scene mesh bytes | 1,064,448 | 1,554,336 |

The overhead GPU and CPU sample is slower. GPU queries poll available results without synchronous waits; screenshot readbacks occur outside timing brackets. This is a Windows development-GPU fixture, not owner-map FPS, weak-Linux-GPU certification or a claim of improved performance.

## Validation, artifacts and limits

See `A2-13-TEST-MATRIX.md` for exact boundaries and `A2-13-MANUAL-TEST.md` for owner checks. Java17 and stock Steam Java25 each run142 regression programs. Liquid native tests contain13,746 checks; campaign SaveIO44; floor relief6,064; ore301. Actual packaging fingerprints match JAR/folder on both runtimes. Physical Windows input/gameplay acceptance, Linux graphics/performance and independent multiplayer acceptance remain **NOT TESTED**.

Final local JAR: `local/a2-13/test-kit/mindustry-fpv-a2-13-diagnostics.jar`

SHA-256: `9fdb08bafa98e88ff82b619dd368c3ccad295967a070bf8d03c9012078981861`

Payload: `payload1-a18e6ade5f2123d14ba4cb6aa4aa98ef09b1fb014a0bd90f3c2c7695df36c66e`

All1,000 other existing class files match A2-12, including physical terrain, mining, combat, cameras/input, networking, construction, natural floor relief and ore deposit modules. New code is limited to liquid presentation, atlas/world rendering, the naval presentation offset and the local visual setting. Existing metadata version, GPL/JNA notices and Windows/Linux native resources remain intact. Prior2,065 Alpha snapshot files and45 previous diagnostic assets pass preservation hashes. Matching source/checksums and sanitized graphics evidence accompany the JAR; no host game binaries/textures, primary profiles, saves or private Steam logs are distributed.

Remaining limits: opaque stylized water, no refraction/underwater transparency/foam simulation; small quiet chunk aprons and static far waves; visible native texture tiling; geometry cost persists with wavesOFF; unknown liquids use static fallback; authoritative waterline/camera/real muzzle remain unchanged. Physical Linux and owner-map visual/performance acceptance are still required. No push, publication or Workshop update occurs.
