# A2-16b — 3D logistics pass 1

Baseline: owner-accepted `19535b19980e50544b1386825242f2dc5a05e0fe` on `main`.
This is an unreleased visual-only diagnostic. It does not replace Alpha assets.

## Completed coverage

| Blocks | Before | This pass |
| --- | --- | --- |
| Conveyor, Titanium Conveyor, Armored Conveyor | Generic Z0 body / separate cached upper box, native animated top and flat cargo | Shared low chassis, real side guards, underside, native direction/turn/frame art and actual cargo |
| Plastanium Conveyor | Generic static presentation | Distinct native green loading/moving/unloading deck, heavier sides, native pallet and cargo position |
| Junction, Router, Distributor | Generic structural box and native roof | Compact resource-bay chassis, recessed tunnel mouths and native routing roof |
| Sorter, Inverted Sorter | Generic box, no dedicated filter presentation | Native distinct roofs over item-color filter backing / unconfigured cross, resource-bay sides |
| Overflow Gate, Underflow Gate | Generic box and roof | Same bounded chassis approach with original distinct native symbols |

All eleven requested/optional native profiles are covered. Bridges, phase belts,
mass drivers, ducts, unloaders, payload and liquid blocks remain unchanged.
Models are selected by exact native block identity; unrelated mod content does
not accidentally inherit them. Accepted drills and other model families remain
unchanged.

## Shared rendering and animation

`LogisticsModelProfile`, `LogisticsModelGeometry` and `LogisticsModelRenderer`
extend the existing native-atlas/depth stream. Z0 and elevated rendering call the
same implementation before generic fallback. Supported upper blocks no longer
also submit a generic cached chunk body. Native world/XYZ fog, distance and
frustum checks remain in effect; nothing renders through opaque obstacles.

Templates are immutable per profile/open-side mask/LOD; material arrays are
cached per owning renderer/type. No geometry, texture or native building is
created per instance/frame. Atlas reload clears the material cache. Side colors
sample an opaque texel of the native sprite once per type, with safe atlas UVs.
The corners of routing models are already part of their arches, so duplicate
coplanar corner posts were removed before final validation.

Conveyor decks use the real blend shape, mirroring and existing native animation
frame helper. Open-side guards use the native blending rule and the building's
floor-local neighbors, converted to local orientation once. Armored/plastanium
guards are wider/higher; titanium keeps its native blue accents. Exact cell-size
belts replace the old Z0 0.93 footprint inset, removing that join gap. The belt
surface is recessed inside the unchanged physical height; cargo sits just above
it. Native item IDs, transverse/longitudinal positions are read without modifying
queues. Item sprites are cropped at the owning belt footprint to avoid drawing
through unrelated neighboring models. Actual occlusion by solid objects remains.

Plastanium cargo reads native state/link/cooldown/count/item. Packed source links
resolve through the owning tile view, preserving the upper floor even with an
identical Z0 layout. A stale/remote/enemy source does not place cargo elsewhere.
Pallet location follows the native cooldown; the pallet's cosmetic rotation uses
the destination heading rather than vanilla's interpolated turn angle. This is
a presentation limitation, not a transport/timing change. Cargo remains native
sprite art; this task does not introduce volumetric item models.

Routing blocks reuse `ResourcePortGeometry`: one body-colored recessed mouth per
footprint cell per side (Distributor has three). Native roof symbols retain
routing readability; sorters additionally expose their actual selected filter
color. Ports are visual apertures, not new transfer restrictions or entities.
Items passing through enclosed routers/junctions may be hidden inside their
chassis; the simulation continues normally.

Near models use four arch facets and protective detail; medium models use two;
far models retain inexpensive sides, original direction art and actual belt
cargo. Existing hologram previews, selection outlines and construction envelopes
remain unchanged and oriented: they describe planned blocks, not operating
machinery or live inventories. The native unfinished building and multi-floor
clipboard preview are exercised separately in the graphical fixture.

## Scope and safety

Only two existing runtime files are integrated: `FpvWorldRenderer` and
`LayerWorldRenderer`. No transport update, block definition, native adapter,
throughput, costs, inventory, power, save format, network protocol, fingerprint
rule, camera, movement or combat code changes. Multiplayer peers still need the
same payload. All new runtime code uses existing Java/Arc/host assets; no extra
native dependencies or platform-specific input APIs.

## Verification

Final measured results appear in `A2-16b-TEST-MATRIX.md` and `A2-16b-VALIDATION.json`.
Scripted Windows SDL/OpenGL checks and mocked native simulation checks are
separate from owner gameplay, physical Linux and remote multiplayer acceptance.
The test fixture uses disposable profiles only. Primary saves and prior release
artifacts are preserved.

## Final measured geometry and performance

Raw cached chassis/guard/tunnel vertices, excluding native roof/deck planes and
cargo (straight belt has two open ends):

| Profile | Near | Medium | Far |
| --- | ---: | ---: | ---: |
| CONVEYOR | 90 | 90 | 30 |
| TITANIUM | 90 | 90 | 30 |
| PLASTANIUM | 90 | 90 | 30 |
| ARMORED | 90 | 90 | 30 |
| JUNCTION | 318 | 222 | 30 |
| ROUTER | 318 | 222 | 30 |
| DISTRIBUTOR | 942 | 654 | 30 |
| SORTER | 318 | 222 | 30 |
| INVERTED_SORTER | 318 | 222 | 30 |
| OVERFLOW | 318 | 222 | 30 |
| UNDERFLOW | 318 | 222 | 30 |

One roof/deck is normally six vertices, one cargo icon at most six. Plastanium
can also use native edge/pallet planes. No full sprite stack or per-item mesh
is generated. In the warmed overview fixture, eleven Z0 types consume 168,576
bytes of material arrays (5,268 cached vertices across used variants). The upper
renderer owns a separate bounded cache. Counts stay stable during steady scenes.
This is observed cache usage, not a maximum for every possible neighbor mask.

Actual Windows SDL/OpenGL comparison at 960×600, stock Steam Java 25, accepted
A2-16a.1 → final A2-16b. Runs are sequential without a concurrent regression suite;
each scene warms 48 frames and times 64 renderer calls. GPU queries are
asynchronous. Screenshots are outside the timed render section; draw calls are
counted on a separate instrumented frame. Explicit native fixture updates are
outside these renderer timings. Numbers include terrain, horizon, other blocks
and upper structures; they are not isolated-model cost or display FPS.

| Scene | CPU before → after (ms) | GPU before → after (ms) | Draw calls |
| --- | ---: | ---: | ---: |
| low | 0.559 → 0.594 | 0.483 → 0.450 | 28 → 27 |
| corners | 0.623 → 0.730 | 0.739 → 0.405 | 41 → 40 |
| stacked | 0.512 → 1.083 | 0.772 → 0.453 | 40 → 39 |
| floor1 | 0.621 → 1.467 | 0.813 → 0.792 | 44 → 43 |
| floor3 | 0.532 → 1.133 | 1.028 → 1.014 | 38 → 37 |
| floor5 | 0.611 → 1.508 | 1.131 → 1.166 | 46 → 45 |
| field100 | 0.525 → 0.862 | 0.606 → 0.638 | 31 → 30 |
| field1024 | 1.129 → 3.656 | 0.678 → 1.917 | 61 → 63 |
| routing-area | 0.641 → 0.636 | 0.669 → 0.660 | 36 → 36 |

The mixed 1,024-block field adds about **2.53 ms CPU / 1.24 ms GPU**, with two
extra stream-buffer flushes. The 100-block field adds about 0.34 / 0.03 ms.
Some close scenes lose one draw call by removing the generic colored body batch;
small timing improvements are variation, not an FPS claim. Higher density
naturally costs more geometry; full gameplay FPS parity is **NOT TESTED**.

Stock Steam cannot expose the allocation counter. Complete Java 17 after-only
whole-renderer samples, including the existing renderer and fixture measurement:

- stacked: 63246 bytes/frame.
- floor3: 86046 bytes/frame.
- field100: 12435 bytes/frame.
- field1024: 69071 bytes/frame.

This is not an isolated before/after allocation comparison or a zero-allocation
claim. No new per-frame sprite/pixmap/template generation is present.

## Final validation and local artifact

**171 programs pass on Java 17 and 171 on stock Steam Java 25.** Dedicated tests
perform 944,915 geometry assertions, 459,226 native/read-only/render-state checks
and 208 authorized platform/3D clipboard/build/save checks. Five-floor clipboard
capture/paste completes 55 new logistics blocks through the existing queue,
retains the 55 originals and preserves sorter configurations.

The final JAR runs 3,840 scripted actual-renderer frames per runtime (7,680 total),
with zero GL errors; the baseline runs another 3,840. All eleven profiles,
connected native cargo lines on Z0 and Z1–Z5, stacked copies, a nearby large drill,
corners, Junction crossing, routing area, underside views, full clipboard ghosts,
placement hologram, a real native unfinished ConstructBuild, actual disposable
SaveIO save/reload and 100/1,024 fields are exercised. The fixture calls actual
native transport updates, while independently checking that drawing does not
change native tile/entity invariants. It is not a throughput benchmark or physical
FPV/mouse test; closed endpoints can correctly develop native backpressure.

Fresh compilation reproduces 973 project classes/resources byte-for-byte.
1,080 existing runtime class entries remain byte-identical; changed existing
classes belong to the two integrated renderers (including debug line shifts in
nested classes), plus five new logistics classes. GPL/notices/JNA and 2,065 Alpha
files / 84 earlier diagnostic assets retain their hashes. Strict JAR/folder
fingerprints match on both runtimes. No primary saves were opened or altered.

Common local Windows/Linux diagnostic:
`local/a2-16b/test-kit-final/mindustry-fpv-a2-16b-diagnostics.jar`.
SHA-256: `edab4d5440f20d724bd3413722c804ffd15bdd6a54d779d42b78438ca0b89bda`.
Corresponding source, checksums, report and sanitized PNG/JSON/lab evidence are
in the same kit. No push, publication or Workshop update. Physical owner checks,
Linux graphics/input and real remote multiplayer remain **NOT TESTED**.
