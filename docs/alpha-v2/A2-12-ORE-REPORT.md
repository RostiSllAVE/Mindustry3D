# A2-12 — 3D ore deposits and deeper visual floors

Baseline: accepted A2-11 visibility correction `8825029d5697ba176a5adc3f8a717dc1d6630dd7`, existing `main`. Local diagnostic only; no public version/release, Workshop update, protocol or save-format change.

## Scope and floor adjustment

The owner explicitly requested more **geometric depth without stronger dark shadows**, in addition to the attached ore task. This overrides the attachment's default restriction on changing A2-11 amplitude for this focused tuning only.

- Natural visual relief increases from a ±0.12 to a **±0.30 tile cap**, 2.5 times the former depth. The stone fixture reaches 0.269506 tile, with maximum adjacent-corner difference 0.168623 tile. Sand/snow retain their 0.65 material multiplier and ice its 0.30 multiplier.
- Slope lighting is normalized to the old depth, then reduced to **55% of the previous slope contrast**. Its bounds narrow from 0.76–1.18 to **0.86–1.10**. Greater geometry therefore does not amplify the dark overhead slope pattern. No new lights, cast-shadow system or shader is introduced.
- The existing four-triangle floor fan, coherent noise, shared samples, foundation fade, flat chunk apron and native UVs remain. Artificial surfaces, occupied foundations and liquids remain flat. The distant coarse tier remains flat.
- Physical terrain is unchanged. Units, collision, picking, construction/support and projectiles still use the existing authoritative coordinates, not these visual displacements.

## Ore implementation

`OreDepositGeometry` derives cached low-poly contours from the **host game's original ore sprite alpha**. Connected opaque islands are bounded/simplified and clipped to the four planar triangles of the existing floor fan. A template retains up to eight candidate patches; each tile raises at most two, with at most four outline corners each. Other details remain visible in the original conforming ore overlay.

`OreDepositRenderer` selects fragments deterministically using tile coordinates and the native ore name. Selection, height, taper and ridge orientation vary without global random state. Profiles preserve the native palette/texture:

| Native floor ore | Nominal height above local floor, tiles | Taper |
| --- | ---: | ---: |
| Copper | 0.130 | 0.88 |
| Lead | 0.160 | 0.90 |
| Coal | 0.140 | 0.84 |
| Titanium | 0.180 | 0.86 |
| Thorium / crystal thorium | 0.190 | 0.88 |
| Scrap | 0.095 | 0.93 |
| Beryllium / tungsten | 0.170 | 0.87 |

Height varies by 0.82–1.12 times the profile; the upper rim is at 76% of that height. These are shallow mineral fragments, not tall crystals or a new physical resource system. Roofs retain native UVs; sides sample an opaque pixel from the same ore region.

`NativeAtlasMesh.Builder.orePatch` adds actual roof triangles and side skirts directly to the existing atlas batch. Roof vertices sample the **exact barycentric height of the underlying floor triangle**, rather than assuming a flat tile. Each fragment stays inside that triangle, so its sloping roof stays above the local floor. The skirt ends 0.005 tile below the tile's minimum visual sample, deliberately buried in the floor; there is no hidden bottom cap or floating open gap.

All nine native floor-ore identities are supported. Native wall ores, vents, pebbles and non-ore markings are not misclassified. Unknown/modded `OreBlock` overlays retain their native conforming texture without guessed geometry. Unavailable CPU pixmaps likewise keep the existing overlay. A completely missing host texture still has the host's normal missing-art limitation.

Raised fragments are suppressed under occupied footprints and on liquids. Native environmental props, cliffs and artificial walls retain their previous renderer. The coarse tier retains representative flat ore at its actual one-tile footprint, without stretching a glyph over a larger coarse cell. It does not generate detailed rocks every frame.

## Cache, costs and measured limits

Alpha extraction happens once per atlas region, not per tile/frame. Templates use borrowed read-only host pixmaps and are cleared on atlas replacement/disposal. Geometry is baked on existing chunk rebuilds. A per-tile floor-height/lighting cache avoids repeatedly calculating the same five fan samples for each fragment. No new texture files, native libraries, shaders, materials or per-deposit draw calls are required.

Maximum added geometry is **72 vertices / 24 triangles per ore tile**. The existing 64 MiB terrain cache and six uploads/frame are preserved. Its conservative detailed-tile budget increases from 48 to **96 vertices** to account for floor, overlay and rocks. Consequently a large dense field can retain fewer detailed chunks (75 versus 151 under the worst-case estimate), falling back to the existing coarse tier. It does not hide gameplay entities. This is a real memory/detail tradeoff requiring owner performance acceptance.

The same compiled `tools/benchmarks/OreMeshComparison.java` probe runs against the latest accepted A2-11 JAR and the final A2-12 JAR. Fixture: 128×128 stone map,64 chunks, one atlas material page, bounded synthetic native-alpha islands; 5 warm-up and7 timed passes, sequential processes, no concurrent test suite. These are **CPU mesh rebuild/native-buffer measurements through MockGL**, not gameplay FPS or GPU benchmarks.

| Runtime / ore coverage | Rebuild64 chunks median before → after | After per chunk |
| --- | ---: | ---: |
| Java17 / none | 12.039 → 12.275 ms | 0.192 ms |
| Java17 / 6.25% | 11.866 → 12.904 ms | 0.202 ms |
| Java17 / 100% | 13.268 → 20.117 ms | 0.314 ms |
| Steam25 / none | 8.937 → 8.392 ms | 0.131 ms |
| Steam25 / 6.25% | 7.657 → 7.992 ms | 0.125 ms |
| Steam25 / 100% | 7.968 → 17.561 ms | 0.274 ms |

| Coverage | Vertices before → after | Triangles before → after | Mesh bytes before → after | Material batches |
| --- | ---: | ---: | ---: | ---: |
| None | 196,608 → 196,608 | 65,536 → 65,536 | 7,077,888 → 7,077,888 | 64 → 64 |
| 6.25% | 208,896 → 278,016 | 69,632 → 92,672 | 7,520,256 → 10,008,576 | 64 → 64 |
| 100% | 393,216 → 1,499,226 | 131,072 → 499,742 | 14,155,776 → 53,972,136 | 64 → 64 |

Java17 measured warm rebuild allocation stays **5,458.9 / 5,714.9 / 9,554.9 bytes/chunk** for none/sparse/dense, respectively, matching baseline after removing a per-patch captured lambda. This includes probe/reflection/wrapper allocations and excludes cold template construction, retained/growing mesh buffers and native memory. Steam25's optional management allocation counter is unavailable. Detailed min/max and coarse timings are in `A2-12-VALIDATION.json`. These measurements show the added dense-field cost; no FPS improvement is claimed.

## Actual Windows graphics evidence

A scripted **Mindustry160.7 / stock Steam Java25 SDL/OpenGL** fixture uses the real installed atlas and production renderer, with identical low-angle and overhead cameras before/after. Only the lab `SmokeHarness` entry class is replaced in disposable copies; the distributable's renderer and original smoke class are not replaced. Profiles stay under `local/a2-12/`; primary saves are never accessed.

The 48×48 gallery contains all nine ore types, isolated deposits, stone/sand/ice transitions, a conveyor row, drill, factory, shoreline, artificial wall and natural mountain. Both runs pass with **GL error0**. The final run extracts **27 native sprite templates** and raises **325 ore tiles**, adding **23,400 vertices**. Direct unedited captures are included in `gpu-evidence/` in the test kit. They show shallow faceted fragments from low angles and recognizable native ore fields from above.

Native Gamma player mining and Mono AI mining each advance their actual copper mining timer in the graphics fixture. Separately, the native mechanical-drill regression actually produces copper. This is not a fake visual/resource grant. Physical clicks, multi-ore owner mining sessions and Linux performance remain separate manual checks.

Short warmed fixture measurements,25 CPU samples and30/24 asynchronous GPU samples for low/overhead views:

| Metric | Latest A2-11 | Final A2-12 |
| --- | ---: | ---: |
| Low CPU submission mean | 1.359 ms | 1.747 ms |
| Overhead CPU submission mean | 1.836 ms | 2.640 ms |
| Low GPU elapsed mean | 0.060 ms | 0.064 ms |
| Overhead GPU elapsed mean | 1.346 ms | 1.292 ms |
| Retained mesh bytes | 1,113,480 | 1,955,880 |

This is a small capped-window scripted sample on the development Windows GPU, not owner-map FPS or weaker-Linux-laptop certification. GPU queries only poll available results; no synchronous readbacks occur inside timing brackets. The two screenshots occur outside those brackets. Small apparent GPU differences are noise, not evidence of acceleration.

## Validation and distribution

See `A2-12-TEST-MATRIX.md` and `A2-12-MANUAL-TEST.md` for explicit automated/manual boundaries. Final diagnostic:

`local/a2-12/test-kit/mindustry-fpv-a2-12-diagnostics.jar`

SHA-256: `d81b0ce2f16e7a145c1da5d06efc19e266a115589d1e0949bbfc45203a73b3fb`

Payload: `payload1-7a7e912f3b427c2998936d4e6bedd0a1c329a7fbfa5023feed1da78427672feb`

Only three existing runtime modules change: `FloorSurfaceRelief`, `NativeAtlasMesh`, `FpvWorldRenderer`; two ore modules are added. All993 other existing class files match the accepted A2-11 JAR byte for byte, including input/camera, unit movement, mining/gameplay, combat, layered construction, networking and physical terrain. License/JNA/platform resources remain included and unchanged. Existing metadata stays at the diagnostic baseline version: distinguish this candidate by filename/checksum, not a newly published release number. The kit contains corresponding source, notices, checksums and sanitized evidence. Prior2,065 Alpha snapshot files and42 earlier diagnostic artifacts pass preservation hashes.

## Remaining visual limitations

- Only two selected low-poly fragments per near ore tile are raised; the full original overlay remains underneath. This is intentionally not a full extrusion of every pixel or wall ore.
- Distant/coarse ground and representative ore remain flat; the current detail/cache policy still controls transition distance. Cold atlas extraction and dense rebuilds have a cost.
- Stronger visual ground can intersect feet, floor-level effects or an extremely low camera because physical Z0 deliberately stays unchanged. Foundation anchoring limits this around buildings. No movement/physics compensation is added.
- Source-derived determinism and campaign SaveIO are tested; a physical multiplayer visual comparison, owner-map acceptance and real Linux input/GPU/performance checks are **NOT TESTED** in this pass.

The natural/artificial classification introduced in A2-10, geological heights, mining rates/resources, placement/support rules, protocol4 and save formats are unchanged.
