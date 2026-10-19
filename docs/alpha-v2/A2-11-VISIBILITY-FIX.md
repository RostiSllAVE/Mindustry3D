# A2-11 — Floor relief visibility correction

Baseline: `63396f2dbe56ee548ae2eab7b9764bde82f812e7` on existing `main`. The original A2-11 report and artifacts are preserved as historical evidence. This correction supersedes their floor appearance and geometry measurements, not A2-10 wall classification or physical terrain.

## Confirmed cause

The original floor sampler did generate nonzero offsets (maximum 0.058393 tile in the fixture), but the **actual textured result was almost flat-looking**. A scripted native Mindustry 160.7 / stock Steam Java 25 SDL/OpenGL capture reproduced the owner's observation, using the installed atlas rather than a missing-texture mock.

Three implementation decisions combined:

1. Floor vertices always had white/uniform lighting, so slopes had no lighting cue against the strongly patterned native textures.
2. A tile only had its four corner samples. Anchoring these to adjacent structures could flatten the entire open tile between conveyors/buildings, despite there being empty ground inside it.
3. The ±0.065-tile limit, two-tile foundation envelope and flat chunk apron suppressed an already tiny signal. The ore overlay kept its original 0.018-tile vertical separation, making it read as a separate sheet.

This is not evidence of an A2-10 mountain failure. The mountains and underlying physical terrain were already working and remain untouched.

## Focused correction

- Natural detailed floor tiles now have a **centre vertex and four triangles**, instead of two. The centre follows the same continuous deterministic field as the shared corners. An open cell can therefore have interior relief while its foundation-facing edges stay exactly flat. No rectangular random-height steps or extra wall subdivisions are introduced.
- The visual limit is **±0.12 tile**. The integer-corner stone fixture reaches 0.107802 tile; its maximum adjacent-corner difference is 0.067449 tile. Sand/snow and ice retain their smaller material factors. The continuous foundation fade is shortened to one tile; every building/prop/liquid/artificial footprint remains zero, including its interior.
- A reusable **33×33 half-tile sample grid** provides coherent vertex slope lighting. Its contrast is intentionally amplified for readability against native pixel art, bounded to 0.76–1.18. This is presentation shading, not a lighting/physics simulation. Shared samples and flat edge lighting preserve tile/chunk/fog transitions. Primitive scratch storage grows from 2,756 to **5,956 bytes** per renderer, not per chunk.
- Floors and ore overlays use the **same fan topology, corner/centre heights, slope colours and native UV mapping**. Floor ore separation decreases from 0.018 to **0.006 tile**. Wall ores and non-ore markings keep their previous path. Ore remains native textured surface art: this does **not** implement extruded resource deposits, which were outside A2-11's scope.
- Flat artificial floors, occupied footprints, liquids and fully flat cells retain the existing two-triangle path. The distant coarse tier remains flat; the existing smooth chunk apron joins it without cracks or extra per-frame sampling. Relief is most useful in the near detail tier (currently 80 tiles horizontally from the camera).

Only `FloorSurfaceRelief`, `FpvWorldRenderer` and `NativeAtlasMesh` runtime code changes. Native atlas images, mountain generation/classification, collision/picking, unit movement, cameras/input, layered construction, networking, protocol 4 and save formats are unchanged.

## Tests and actual graphics evidence

| Verification | Result | Boundary of evidence |
| --- | --- | --- |
| Full Java 17 build/regressions | PASS,140 programs | Windows complete Java 17 |
| Full stock Steam Java 25 regressions against final JAR | PASS,140 programs | Windows Java25.0.1, no java.logging |
| Native floor test | PASS,5,614 assertions | Bounds, determinism, seams, material exclusions, native UVs/colours/ore fan correspondence, real CPU mesh counts, foundation interior and open cell between conveyors |
| Campaign SaveIO | PASS,43 assertions | Disposable real native saves, exact relief sample restoration, sector transitions |
| Construction/pause/priority/selection/copy/paste | PASS | Existing native and in-memory network fixtures; no physical input assertion |
| Actual SDL/OpenGL floor captures | PASS,GL error0 before and after | Mindustry160.7, stock Steam Java25, RTX4050, native atlas, fixed low-angle and overhead cameras |
| Candidate JAR/folder fingerprint | PASS on Java17 and Steam25 | Actual final containers; strict identity unchanged |
| Physical owner-map appearance and input | NOT TESTED | Owner acceptance remains required |
| Physical Linux / real two-computer game | NOT TESTED | Same intended Windows/Linux JAR; no new native dependencies |

The graphics probe replaces **only the lab SmokeHarness entry class** in disposable copies of each candidate. It loads a new 48×48 stone/sand/ice map with copper/lead ore, a conveyor row, a factory, water and a natural mountain. Both cameras run through the **production FpvWorldRenderer and actual game atlas**, with identical lab entry bytecode. Production renderer classes/resources are not replaced in the probe. The real distributable retains its original SmokeHarness. Profiles and all generated saves stay under `local/a2-11-visibility/`; primary player saves are not accessed.

Captures are provided in the test kit's `gpu-evidence/` folder: `before-low.png`, `after-low.png`, `before-top.png`, `after-top.png`. These are direct, unedited game captures. Comparing fixed bare-ground overhead regions gives mean absolute RGB differences of approximately 7.81/7.81/8.27 on stone and 13.08/10.98/8.89 on sand, confirming that actual rasterized floors change, not just CPU height data. Those pixel differences are not a substitute for the owner's visual judgement.

## Measured cost

The reusable `tools/benchmarks/FloorMeshComparison.java` now **reads actual Mesh vertex counts** instead of printing a fixed topology assumption. Same 128×128 stone fixture,64 chunks,5 warm-up passes,7 timed passes, medians; MockGL/native buffers, sequential before/after processes:

| Runtime / path | Generate64 chunks before → after | After per chunk | Mesh bytes before → after |
| --- | --- | --- | --- |
| Java17 fallback | 2.717 →10.569 ms | 0.1651 ms | 2,752,512 →5,505,024 |
| Java17 atlas | 2.323 →10.795 ms | 0.1687 ms | 3,538,944 →7,077,888 |
| Steam25 fallback | 3.610 →6.377 ms | 0.0996 ms | 2,752,512 →5,505,024 |
| Steam25 atlas | 2.585 →6.547 ms | 0.1023 ms | 3,538,944 →7,077,888 |

For this all-natural map, vertices/triangles rise from **98,304/32,768 to196,608/65,536**. Atlas material batches remain64. Flat/occupied cells do not double. The existing 64MiB mesh-cache cap and six chunk uploads/frame remain unchanged. Relief generation happens on rebuilds only. Java17 measured rebuild allocations (including unchanged probe/reflection/mesh-wrapper overhead) remain1,178.9 bytes/chunk for fallback and5,458.9 for atlas; Steam25's optional management allocation counter is unavailable. Raw min/max/coarse timings are in the validation JSON; variations of the unchanged coarse path are not improvements claimed by this patch.

Short **actual GL** fixture measurements after warm-up (30 low-view and24 top-view asynchronous GPU samples,25 CPU submission samples/view):

| Metric | Old A2-11 | Final correction |
| --- | --- | --- |
| Low-angle CPU submission mean | 2.127 ms | 1.800 ms |
| Overhead CPU submission mean | 1.732 ms | 1.384 ms |
| Low-angle GPU elapsed mean | 0.668 ms | 1.663 ms |
| Overhead GPU elapsed mean | 0.633 ms | 0.650 ms |
| Retained mesh bytes in fixture | 549,504 | 999,864 |

This is a small, scripted, capped-window fixture. GPU queries poll available results only and do not block for completion. Results are noisy, include pipeline work inside the renderer, and do **not** establish owner gameplay FPS or a broad performance improvement. There is a real geometry/generation/memory cost; the short low-angle GPU sample is slower. No synchronous GPU readback is used in timing loops; the two screenshot readbacks occur outside GPU timing brackets. Owner fullscreen performance checks remain necessary.

## Candidate and preservation

Import the distinctly named file:

`local/a2-11-visibility/test-kit/mindustry-fpv-a2-11-visibility-diagnostics.jar`

SHA-256: `dfeb396ce603500e14af8721e93a5585930d14fa0119530ed9794a9a06fbbe1a`

Payload: `payload1-a6cfa838ca957ea7091f725f4ab191b606f6d6a91995fe336ba3ea8047718f58`

The local kit contains matching source/checkpoint manifests, checksums, reports and graphics evidence. Metadata remains the existing diagnostic baseline version; the filename/hash distinguishes this candidate. All996 other existing class files match A2-11 byte for byte, including production input, combat, construction, network and physical terrain classes. All2,065 earlier Alpha snapshot files and39 previous diagnostic assets, including the original A2-11, pass preservation hashes. No published release, Workshop item or primary save is changed. No push/publication occurs.

## Owner check

1. Import the **visibility** JAR above through Mods → Import Mod, confirm replacement and restart. Use Fullscreen and a backed-up/disposable save. The old A2-11 file still exists and is not the corrected candidate.
2. Look closely along open stone/sand/ore ground, then from above. Check small open gaps between conveyors, foundations, shorelines and chunk transitions. Expect readable shaded irregularities, flat building bases and ore following the surface; distant coarse ground remains flat. Compare with the supplied captures if useful.
3. Place/remove a factory, use Tab/F3/priority/3D selection/copy-paste, save/reload, and compare FPS on the same map/camera/settings. Report any extra stutter, visible seams, ore flicker or excessive floor/foot intersections. Unit feet and gameplay effects still use physical Z0, so small purely visual intersections remain possible; this correction deliberately does not move physics.
