# A2-14 — Vegetation variety and placement

Baseline: accepted A2-13b `38518b5b1d9a22b16c8b3a46564798b73b4a550a`, current `main`. Local diagnostic only. No release, Workshop update or remote operation.

## Findings and scope

The old vegetation renderer already chose eight cached variants and a yaw, but its width range was only0.91–1.00 and its height did not vary. The accepted5× presentation multiplier made adjacent crowns intersect severely. All bushes, spores and seaweed-like props shared the same rounded shrub silhouette; red-diamond flora used the generic pine shape. Roots were at native Z0.

This pass changes presentation only. Native `NaturalProps`/terrain classification, tiles, support, harvesting, pathfinding, hitboxes, physics, unit models, liquid behavior, construction state, saves and protocol4 remain unchanged. An artifact comparison restricts modified existing runtime classes to four renderer families;1,001 existing classes are byte-identical, including networking, combat, unit motion and liquids.

## Native content and identity

- Pine, snow pine and spore pine retain their layered conifer identity and native palettes.
- White trees retain their branching leafy crown; dead white trees keep bare branches.
- Shrubs and pur-bush use compact, rounded multi-lobed crowns. Tall native shrubs use a0.48 height-profile multiplier to read as shrubs rather than tree-sized towers.
- Spore clusters use several stalks with small rounded caps.
- Redweed uses branching fronds; yellow coral uses angular branching fans.
- Red-diamond biome flora uses a faceted three-spire profile rather than a generic cone.

These are eight render profiles with at most eight reusable templates each. Native textures supply sampled opaque colours/materials, including snow/spore/desert identities; no host texture files are redistributed. Rocks stay on the original transform/template path. Crystal blocks/orbs/spires outside the existing vegetation family are not reclassified or redesigned.

## Stable instance generation

`VegetationInstances` derives a seed from integer tile X/Y and the native content **name**, using fixed integer mixing. Numeric content IDs, frame time, player identity, session RNG and camera angle do not determine the new pose. Name hashing also avoids mod-load-order ID differences.

For the established5× size envelope, isolated width varies0.82–1.12 and height0.82–1.20. Depth is0.88–1.00 of width, yaw covers360°, and the cached eight shape variants remain. Visual roots can nudge at most0.16 tiles per horizontal axis; a small crown bend grows quadratically with height and leaves the root anchored. No logical tile moves. Roots keep the existing native Z0/relief-flattened prop base; liquid floors/beds are not changed or used to relocate plants.

Native tests reproduce all poses after world replacement and in reverse evaluation order, representing two independent client-side computations. Actual SDK SaveIO/load also preserves the map and derived pose fingerprint. This is deterministic local presentation, not a new save chunk or packet, and not proof of independent multiplayer gameplay.

## Packing and obstacles

A3×3 neighbourhood counts nearby vegetation with cardinal/diagonal weights1/0.5. Crown width/depth receive `max(0.34, 1 / (1 + 0.35 × density))`; height receives only a small additional density adjustment. Every native plant remains represented. Restrained nudging separates centres and biases them away from neighbours/solids. This preserves a forest rather than deleting most of it.

A bounded three-tile neighbourhood checks tall solids/cliffs during pose generation, reducing the envelope where a canopy would run into them. Low walls can retain natural overhang. An indexed query of existing elevated structures caps the visual crown below intersecting platforms/ceilings; it does not move platforms, change support or remove plants. Neighbour edits invalidate nearby pose/chunk caches. A read-only layer listener invalidates affected areas for geometry changes, ignores inventory/health-only replacements, and rebinds when campaign/save loading replaces the layer world.

Dense81-pine native fixture: a two-dimensional crown-envelope overlap proxy falls6,791.980→110.510 while all81 tiles/plants remain. This is a conservative layout metric, **not a measured98% reduction of actual triangle intersections**. Small residual overlaps are intentional/possible. Crowns close to high cliffs can become narrow; low ceilings can compress a tree considerably. This is not a general mesh/mesh packing solver, and native collisions still match the original map rather than the enlarged visual foliage.

## Rendering and costs

Templates and colour sampling remain cached. Derived per-tile instances are cached and reused on both detailed and coarse submissions; cache hits do not rescan neighbours or issue structure queries. Detailed plants stay baked into the existing terrain atlas chunks: no per-plant GL draw, new texture, shader, framebuffer or dynamic collision test. Existing distant representative/stride LOD remains, so far forests can still look simplified. Bounds include the maximum varied height, canopy width, root nudge and lean. The original configured64MiB mesh-cache budget and six uploads/frame remain. The inherited detail-selection estimate can undercount very dense prop geometry; this pass does not retune that LOD/cache policy or certify its hard retained-memory bound on huge forests.

Each cached instance stores eight floats, a variant and a profile reference, plus the map entry/object overhead. This is additional derived heap memory, not saved/networked state. Exact retained JVM heap is not measured. Cold scene timings below include first instance/template generation; warm timings reuse it. Native cache tests verify repeated lookups and chunk rebakes do not regenerate instance poses. Construction edits near vegetation can trigger local rebaking.

### Native CPU/mesh comparison

Identical compiled probe on64² native maps,3,277 props,16 detailed chunks,5 warm-up and7 measured rebuild passes; median shown. Real Mesh buffers/uploads through MockGL and a synthetic atlas. No concurrent regression/game benchmark runs. This is **not game FPS**, and the first cold pass includes JIT/template/instance initialization. Coarse numbers measure request/vertex assembly only. Optional per-thread allocation is available on Java17 and unavailable on the stock stripped Steam runtime.

| Runtime/map | Warm rebuild before → after | First cold pass before → after | Coarse assembly before → after |
| --- | ---: | ---: | ---: |
| java17/forest | 12.820 → 12.003 ms | 51.794 → 46.693 ms | 0.448 → 0.424 ms |
| java17/mixed | 16.691 → 17.423 ms | 24.502 → 32.382 ms | 0.368 → 0.314 ms |
| java17/rocks | 3.983 → 5.405 ms | 9.307 → 9.799 ms | 0.171 → 0.307 ms |
| steam25/forest | 11.930 → 12.134 ms | 50.854 → 65.635 ms | 0.721 → 0.358 ms |
| steam25/mixed | 15.287 → 14.557 ms | 22.638 → 32.788 ms | 0.379 → 0.319 ms |
| steam25/rocks | 3.126 → 3.182 ms | 5.496 → 6.248 ms | 0.182 → 0.195 ms |

Forest totals remain756,984 vertices/252,328 triangles/27,251,424 mesh bytes/16 material batches. Mixed flora rises1,083,834→1,114,683 vertices,361,278→371,561 triangles and39,018,024→40,128,588 bytes (about2.85%), still16 batches. Warm Java17 probe allocation remains9,064.5 bytes/chunk; this excludes already retained instance caches. Rocks retain344,082 vertices and the identical actual mesh CRC32 `f88de74f` on both Java versions before/after. Timing differences in that unchanged control case illustrate noise; no FPS speedup is claimed.

### Actual stock Steam graphics

The real160.7 SDL/OpenGL renderer and native atlas run in disposable profiles, with only the lab smoke entry replaced in copies. The distributed JAR retains its original entry. Baseline and candidate use the identical440-frame gallery/cameras. GPU queries are asynchronous; screenshots are outside timing brackets. Both complete without GL errors and pass real native save/load. Additional exterior forest-edge captures use a second identical helper with GPU timing disabled and are not performance samples.

| Metric | A2-13b | A2-14 |
| --- | ---: | ---: |
| Low CPU,25 samples | 1.257 ms | 1.737 ms |
| Top CPU,25 samples | 1.935 ms | 1.874 ms |
| Low GPU,44 samples | 1.632 ms | 1.265 ms |
| Top GPU,60 samples | 1.692 ms | 1.564 ms |
| Retained scene meshes | 7,079,400 bytes | 7,093,656 bytes |

These short Windows samples are noisy and scene-specific. The low camera deliberately enters a very dense canopy; exterior captures provide a clearer forest-edge view. They do not certify performance on owner maps, large Linux forests or remote multiplayer. No synchronous GPU readbacks are introduced in production code.

## Validation and distribution

See `A2-14-TEST-MATRIX.md`, `A2-14-VALIDATION.json` and `A2-14-MANUAL-TEST.md`. The diagnostic build runs the complete145-program suite on Java17 and stock Steam Java25. New native tests cover196 profile/packing/obstacle/cache checks; geometry tests cover134,640 finite vertex checks. Existing construction/priority/copy/pause/input/campaign/combat/liquid suites remain. Fresh recompilation matches all898 runtime classes and resources. JAR and extracted-folder payload identity are checked using the native Java implementation on both runtimes.

Use `local/a2-14/test-kit/mindustry-fpv-a2-14-diagnostics.jar`. Multiplayer participants/servers must use the identical candidate under the existing strict identity rules. The diagnostic filename identifies the candidate; published metadata/releases are not overwritten.

Physical Windows play/import/FPV input, Linux graphics/input/performance and independent two-computer acceptance remain **NOT TESTED** here. The owner should inspect forest density, narrow crowns near cliffs, platform clearance, biome silhouettes, original collisions and save/load on a disposable map. There are no new controls or settings in this pass. Existing windowed-mode performance limitations remain; use Fullscreen.

## Artifact identity

JAR SHA-256: `afa0d8d7ac5dc57d7661b1bda5803e361dadf5ccb0356f6cba6d5ce71cffe197`. Canonical JAR/folder identity: `payload1-c42c5febbb3c749335ad962ab2ffee2ebfc6fa5230af6530a3d48211a367a14c`. Corresponding tracked source, notices, checksums and sanitized capture evidence are included in the local test kit. All2,065 previous Alpha snapshot files and51 prior diagnostic JAR/source/kit assets were verified unchanged. No primary saves/profiles or private raw Steam logs are packaged.
