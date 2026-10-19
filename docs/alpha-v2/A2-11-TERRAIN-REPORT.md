# A2-11 — Subtle natural floor surface variation

Baseline: accepted A2-10 `744c52c76a78d350409de048188444a8fd26ccb9`, existing branch `main`. This is an unreleased diagnostic candidate. The owner accepted A2-10 gameplay; A2-11 physical visual acceptance remains pending.

## Renderer audit and implementation

The existing detailed renderer caches 16×16-tile chunks. Each floor already has a textured quad (six unindexed vertices, two triangles), native atlas variant/autotile selection and half-texel UV inset. Natural mountains use a separate height field. The far tier streams stride-sized flat quads instead of retaining a full-map mesh.

`fpv.render.FloorSurfaceRelief` adds read-only **visual corner offsets**, exclusively while a detailed chunk is rebuilt. Two fixed, quintic-blended value-noise octaves at approximately 5.3- and 2.6-tile wavelengths provide coherent variation. Integer coordinate hashing has no time, random generator, player identity, local settings or mutable seed. Java 17 and Java 25 use the same strict floating-point operations. Equal native tiles produce equal corner samples across clients and reloads, without saved or networked relief data.

The hard bound is **±0.065 tile** (±0.52 native world unit). The 48×48 stone fixture reaches 0.058393 tile in absolute height; its largest adjacent-corner difference is 0.036535 tile. Sand, dark sand, snow, mud, salt and ice-snow use 65% amplitude; ice, red ice and crystal floor use 30%; the explicitly supported natural stone/soil/vegetation floors use 100%.

Native Floor definitions do not expose a dependable natural/artificial material flag. This feature therefore opts in known native **content identities**, with conservative flat fallback for unknown/modded floors. This is a separate visual-floor material table; the A2-10 natural-wall classifier, overrides, mountain mask and height field are unchanged. Industrial metal/panels/tiles, authored coloured floors, core zones, vents, space, empty and all liquids stay flat.

The existing `NativeAtlasMesh.Builder.terrainTop` handles the displaced floor and ore overlay with the original texture, orientation, texel inset and matching triangle diagonal. No texture generation, duplication, shader or new draw pass is added. Missing textures retain the map-colour fallback with the same displaced corners. Flat tiles continue through the original flat-quad path, including the existing liquid material flag. No water animation or ore extrusion is introduced.

## Anchors, seams and cache lifecycle

- All four corners touching a native building, static mountain/wall, tree/bush/rock, non-ore decorative overlay, artificial/special floor, liquid, undiscovered cell or map boundary are zero. A short smooth distance envelope blends surrounding relief back in. Multi-tile buildings anchor every actual footprint cell, not just their centre. Existing prop base positions, building foundations and mountain geometry stay aligned.
- OreBlock overlays share the exact floor corner heights plus the pre-existing atlas separation. Other floor markings remain flat instead of being independently displaced. The original mountain wall-ore path is unchanged.
- Every tile uses shared global corner coordinates. Detailed chunks have a smooth **two-tile flat apron** at their edges. This deliberately joins flat far-tier/fog geometry without cracks, skirts, extra tessellation, camera-dependent rebuilding or extra coarse work. Distant relief is intentionally omitted; smooth flattening may become noticeable during close inspection of chunk boundaries and needs owner visual acceptance.
- One reusable 17×17 corner array and 20×20 material halo occupy **2,756 bytes** of primitive scratch storage per world renderer, plus normal array/object headers. No separate world-sized height map or retained relief array per chunk exists. Material lookups and noise run during existing chunk generation, not every frame.
- Existing native tile/floor/overlay events invalidate nearby cached chunks. Their radius is extended by two tiles for the visual anchoring halo, including neighbouring chunk edits. World replacement/full invalidation regenerates from current tiles; no stale relief state crosses worlds. Fog rebuilds use the current visibility predicate. Frustum bounds include the small negative visual offsets.

`TerrainElevation`, collision, surface queries, aiming/picking, unit clearance, movement, support, placement, mining targets, selection and clipboard coordinates continue using their **original physical heights**. No new gameplay elevation exists. Very shallow floor-level gameplay indicators still refer to physical Z0; their physical coordinate is intentionally not displaced. Any low-angle intersection that remains should be reported rather than fixed by moving physics or construction planes.

## Measured cost versus A2-10

Identical Java probe against each actual diagnostic JAR, sequential processes, native 160.7 content, 128×128 stone map, 64 detailed chunks. Five warm-up passes, seven timed passes; medians below. Actual Mesh construction/upload/disposal uses host native buffers and **MockGL**. These are CPU fixture measurements, **not GPU time, displayed draw calls or gameplay FPS**. Full raw median/min/max results are in `A2-11-VALIDATION.json` and the local `performance-comparison.json`.

| Runtime / path | Generate 64 chunks, before → after | Per chunk, before → after | Coarse 64 chunks, before → after |
| --- | --- | --- | --- |
| Java 17, missing-atlas fallback | 0.911 → 2.596 ms | 0.0142 → 0.0406 ms | 0.199 → 0.212 ms |
| Java 17, textured atlas | 2.456 → 2.388 ms | 0.0384 → 0.0373 ms | 0.424 → 0.270 ms |
| Stock Steam Java 25, fallback | 0.864 → 4.113 ms | 0.0135 → 0.0643 ms | 0.217 → 0.305 ms |
| Stock Steam Java 25, textured atlas | 1.197 → 2.482 ms | 0.0187 → 0.0388 ms | 0.251 → 0.258 ms |

New sampling has a real generation cost. Java 17's small textured timing decrease and variations in the unchanged coarse path are process/JIT/noise effects, not an optimization claim. No cached-frame CPU/GPU improvement is claimed. The identical probe source is retained at `tools/benchmarks/FloorMeshComparison.java`; compile it against the host JAR and existing test classes, then run `fpv.input.FloorMeshComparison` separately with the A2-10 and A2-11 JARs on the classpath. It requires the host's native buffer library and uses MockGL, not a game window. The existing upload-per-frame limit remains six; unchanged cached chunks do not resample relief.

For all 64 chunks, before **and** after:

| Quantity | Result |
| --- | --- |
| Vertices / triangles | 98,304 / 32,768 |
| Textured mesh bytes | 3,538,944 |
| Fallback mesh bytes | 2,752,512 |
| Material batches | 64, one per chunk in this single-atlas fixture |
| Java 17 measured heap allocation per textured chunk rebuild | 5,282.9 bytes, unchanged |
| Java 17 measured heap allocation per fallback chunk rebuild | 1,074.9 bytes, unchanged |
| Steam Java 25 allocation counter | Unavailable in bundled module set; not inferred |

Heap counters include the probe/reflection/mesh wrapper overhead, exclude native memory and were measured after warm-up. Scratch allocation is once per renderer. Real GPU frame time, FPS and hardware draw calls are **NOT TESTED** in this pass; owner fullscreen Windows/Linux checks are required.

## Validation and distribution

Full build/test run: **140 programs PASS on complete Java 17; 140 PASS against the packaged JAR on stock Steam Java 25.0.1**. The new native floor fixture has 5,595 assertions covering bounds, determinism, cached/shared seams, material policies, shores, fixed footprints, fog, native UVs, overlay alignment, actual CPU mesh buffers, unchanged topology/coarse geometry and neighbouring cache invalidation. The strengthened campaign fixture has 43 checks, including real disposable disk SaveIO reproduction of visual samples. Existing construction/pause/priority, multi-floor selection/copy/paste, gameplay and network regressions pass.

Artifact comparison restricts changed existing classes to FpvWorldRenderer and its nested classes; the only new runtime class is FloorSurfaceRelief. **995 existing classes remain byte-identical to A2-10**, including terrain classification/physics, construction, units, combat, input and networking. Protocol 4, save formats, metadata version and compatibility validation are unchanged. Native input libraries and GPL/third-party notices are preserved. All 2,065 earlier Alpha snapshot files and 36 prior diagnostic release assets passed hash preservation checks.

Local common Windows/Linux candidate:

`local/a2-11/test-kit/mindustry-fpv-a2-11-diagnostics.jar`

SHA-256: `547e2459dd08e1c2886480a2cfdc00140c4ed7176efd163e3578c28cdb272db2`

Payload identity: `payload1-5370d204a6739906d669a3a92459cd2c34e9657062be658406043cff16aa58de`

The matching corresponding-source ZIP, source checkpoint manifest, checksums and test-kit ZIP are prepared locally after the Git checkpoint. Public Alpha artifacts and ordinary player profiles are not modified. No push, publication or Workshop update occurs. This is not a certification of physical Linux visuals or remote multiplayer.

## Remaining visual limitations

Two triangles per tile create a gently faceted low-poly surface, not sub-pixel smooth terrain. Unknown/modded and special vent floors deliberately remain flat. Far-tier floors remain flat with the seam-safe detail apron. Real units still stand on their original physical surface; a maximum 0.065-tile visual intersection/gap is possible away from fixed structures. Buildings and static props are anchored; camera, collision and build planes are intentionally unaffected. Physical low-angle appearance, chunk-apron visibility and real frame performance need owner review using `A2-11-MANUAL-TEST.md`.
