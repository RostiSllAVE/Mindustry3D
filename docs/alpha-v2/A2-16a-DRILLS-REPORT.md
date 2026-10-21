# A2-16a — Vanilla mining-drill 3D models

Baseline: owner-accepted `57602ce` on `main`. Local diagnostic only; no publishing,
Workshop update, branch switching or previous-artifact replacement.

## Coverage and implementation

Dedicated requested drill coverage increases from **0/4 to 4/4**. The earlier
generic drill cube/plane adapter remains the fallback for unsupported content.
The pre-change family inventory is in `A2-16a-3D-MODEL-AUDIT.md`.

| Native block | New mechanical silhouette |
| --- | --- |
| Mechanical Drill | Low native-textured foundation, exposed corner drive supports, central shaft, four-lobed rotating head and compact copper cap |
| Pneumatic Drill | Raised foundation, four pressure cylinders and moving piston rods, spindle and gray native head/cap |
| Laser Drill | Four taller optical support towers with violet native-color bands, faceted central housing, native spinning assembly and raised cap |
| Airblast Drill (`blast-drill`) | Broader stepped pressure housing, heavy outer drive modules, moving rods, larger native rotor and warmup-dependent native rim |

`DrillModelProfile` holds cosmetic ratios and exact native block identities.
`MechanicalBlockGeometry` builds shared box/column/support templates once per
profile/LOD. `DrillModelRenderer` adds cached native-alpha head/cap volumes and
submits transforms into `NativeAtlasMesh.Builder`, the existing depth-tested
opaque atlas batch. The same renderer is used on Z0 and upper construction floors;
the upper generic chunk body is omitted to avoid rendering it twice. Missing or
unsupported art retains a visible generic fallback.

Original texture proportions matter: Mechanical/Pneumatic use a 64 px base,
58 px rotor, 34 px cap and 12 px item indicator; Laser uses 96/74/50/18 px,
Airblast 128/90/50/18 px. Parts use their native size relative to the base,
rather than stretching every layer to the full multiblock. Roof UVs preserve
the original artwork. Sides borrow cached opaque edge-color samples; accent
bands sample the original base. Existing half-texel atlas insets remain in use.
No host textures or new native dependencies are bundled.

Bodies and their animated strokes stay inside existing footprint/height bounds.
The conservative rotating silhouette is fitted inside its footprint before it
turns. A central shaft connects the head and cap. Native `rotate`/`rotateDraw`
rules are respected; these vanilla drills do not acquire new placement rotation.
Uneven ground around them retains the existing foundation anchoring. Upper
models use their actual construction-plane height. Picking/collision heights,
physical terrain and placement rules are unchanged.

## Animation and gameplay boundary

Rotation reads `DrillBuild.timeDrilled * Drill.rotateSpeed`, including the native
warmup coast-down. Rod displacement reads the same native time and an activity
gate using enabled state, efficiency, warmup, dominant ore/cell count and output
capacity. The native Airblast rim is activity-dependent; the ore indicator uses
the real dominant item color. No wall-clock-driven fake production animation.

The renderer never updates a building, grants an item, changes a consumer or
writes progress/warmup. Native mining speed, power, liquids, health, collisions,
network protocol and save data are unchanged. Native animation phase may reset
on reload as before: no new persisted phase is introduced. The new headless test
also executes real native production, rather than inferring mining from pixels.
The graphics fixture sets native operating/idle fields explicitly for repeatable
images; production is validated separately. Its upper-floor pose does not claim
that ore can be extracted through platforms where existing rules forbid it.

## Geometry, batching and measured costs

Full machinery is used below 28 camera-distance tiles, reduced support/head
geometry from 28–80, and a compact body/native icon beyond 80. Static topology
and alpha silhouettes are cached; they are not tessellated per frame. Existing
stream buffers still transform/upload visible instances each frame. This is
shared CPU template batching, not hardware instancing or a new static-world
mesh framework. Distant pistons and far head animation are intentionally omitted.

Measured with the real loaded 160.7 atlas, active overlays included:

| Model | Near vertices / triangles | Medium vertices / triangles | Far vertices / triangles |
| --- | ---: | ---: | ---: |
| Mechanical | 804 / 268 | 456 / 152 | 36 / 12 |
| Pneumatic | 924 / 308 | 480 / 160 | 36 / 12 |
| Laser | 876 / 292 | 492 / 164 | 36 / 12 |
| Airblast | 966 / 322 | 594 / 198 | 36 / 12 |

The baseline near generic adapter submits 96 vertices / 32 triangles per drill;
its distant body/roof path submits 42 / 14, without optional team layers. New
geometry is deliberately richer nearby. The measured native parts all share
one texture batch. One complete renderer-owned four-type model cache uses
**180,864 bytes** of vertex arrays, plus **96,768 bytes** of shared unmaterialized
templates. Z0 and elevated renderers own separate bounded material caches.
There is no mesh, texture or model cache per placed drill. Cache-build counts
remain unchanged through steady camera/animation frames, reload and large fields.

Paired actual SDL runs on the same Windows PC, Mindustry 160.7, stock Steam Java
25, 960×600. Each scene warms for 48 frames and measures 64 render calls. GPU
times use available asynchronous timer-query results; no synchronous GPU timing
readbacks. Screenshot capture frames are outside timing. GL calls/vertex-index
submissions are counted in a separate single instrumented frame. Tests were
sequential, without a simultaneous regression-suite workload.

| Full rendered scene | CPU before → after (ms) | GPU before → after (ms) | GL draw calls before → after |
| --- | ---: | ---: | ---: |
| Mechanical close-up | 0.480 → 0.431 | 0.065 → 0.064 | 17 → 16 |
| Pneumatic close-up | 0.416 → 0.403 | 0.067 → 0.066 | 18 → 17 |
| Laser close-up | 0.427 → 0.448 | 0.068 → 0.067 | 18 → 17 |
| Airblast close-up | 0.537 → 0.470 | 0.067 → 0.069 | 20 → 19 |
| Four upper-floor drills and platforms | 0.678 → 0.692 | 0.072 → 0.073 | 28 → 28 |
| 100-drill field | 1.062 → 1.918 | 0.178 → 0.212 | 45 → 45 |
| 1,024-drill overview | 2.922 → 3.180 | 0.662 → 0.643 | 66 → 66 |
| 1,024-drill field, low close camera | 1.303 → 3.401 | 0.238 → 2.686 | 36 → 36 |

These are whole-renderer measurements, including terrain, ore, horizon and
composition, not isolated drill costs or display frame times. Small improvements
are measurement variation, not an FPS claim. The dense near-field case has a
confirmed extra cost of about **2.1 ms CPU / 2.45 ms GPU**; FPS parity is **not
certified**. It remains bounded in this fixture, but physical stress acceptance
is still required. More aggressive LOD/static-body batching can be assessed
later if real gameplay needs it; this pass does not hide the measured cost.

Allocation profiling is unavailable in the stock Steam runtime. Complete Java
17 after-only render-thread samples are approximately 24,939 B/frame (100),
113,547 B/frame (1,024 overview), 37,499 B/frame (close field). These include the
existing renderer and lab measurement overhead; they are not zero-allocation
claims or a before/after allocation comparison.

## Validation and limitations

See `A2-16a-TEST-MATRIX.md`, `A2-16a-VALIDATION.json`, and test-kit evidence.
The final build passes 167 test programs on Java 17 and 167 on stock Steam Java
25. Two actual final-JAR SDL runs render 1,792 frames each with zero GL errors;
the baseline comparison renders another 1,792. Operating-frame comparisons
change 1,698 pixels; idle comparisons change zero, on both runtimes. Model
statistics match across Java runtimes. Four named types, top/oblique/low views,
upper floors, unequal terrain around foundations, actual disk save/load and
100/1,024 fields are exercised. Campaign, construction, copy/paste, cancellation,
priority, mouse contracts, combat and network regressions remain passing.

This is a stylized procedural model pass, not an exact mechanical reconstruction
of sprite internals. Existing face/ambient lighting is retained; no new dynamic
shadow system. Falling structural debris and placement ghosts retain their
existing simpler representations. Other mining families and unrelated models
are unchanged. Physical owner gameplay, Linux rendering/input and two-computer
multiplayer acceptance are **NOT TESTED** by these Windows scripted fixtures.
No separate clean-machine installation or performance guarantee is claimed.

## Local artifacts

`local/a2-16a/test-kit-ready/mindustry-fpv-a2-16a-diagnostics.jar` is the common
Windows/Linux diagnostic, with existing Java 17/Steam 25 compatibility paths.
JAR SHA-256: `23c424df6b3f02da671516a17005ab162f6a83f8ba825ea52137a13782e5f80c`.
The matching source ZIP, GPL/third-party notices, sanitized evidence and checksums
are beside it. Earlier Alpha and diagnostic artifacts remain unchanged. Peers
must use the same JAR; strict payload validation is retained. Owner steps are
in `A2-16a-MANUAL-TEST.md`.
