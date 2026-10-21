# A2-16d — Manufacturing models, part 1

Baseline: completed A2-16c, `6747d4beac1faea99e1601bb23762ba0175a5139`, on `main`.
The owner deferred physical A2-16c acceptance. This pass does not imply that it was
accepted. Local diagnostic only; no public release, metadata version or protocol
change.

## Audit and coverage inventory

The installed Mindustry 160.7 metadata/classes, native GenericCrafter animation
state and the current Z0/elevated rendering paths were inspected before editing.
All six requested blocks previously used the shared textured body/vents/warmup
cue on Z0 and the structural box plus warmup cue upstairs. None had a dedicated
high-quality manufacturing model to preserve.

| Vanilla block | Size | Native type | New dedicated presentation |
| --- | --- | --- | --- |
| Graphite Press | 2×2 | GenericCrafter | Heavy port foundation, bed, side uprights, overhead beam, descending press head and telescoping rods |
| Multi-Press | 3×3 | GenericCrafter | Larger four-post frame, two crossbeams, two alternating press heads and four rods |
| Silicon Smelter | 2×2 | GenericCrafter / DrawFlame | Enclosed polygonal furnace, framed heated vents, supports and original flame top art |
| Silicon Crucible | 3×3 | AttributeCrafter / DrawFlame | Open thick-rimmed crucible, recessed chamber, four stacks, framed vents and native flame art |
| Pyratite Mixer | 2×2 | GenericCrafter | Open mixing bowl, lower material bed, vertical shaft, motor/support frame and rotating paddle cross |
| Blast Mixer | 2×2 | GenericCrafter | Heavier closed mixing tank, external guards/stacks, raised agitator assembly and motor |

Requested dedicated coverage: **0/6 → 6/6**, using the same renderer on Z0 and
upper floors. Other crafters, unit factories and Erekir production blocks retain
their existing fallback/native overlays. Existing four drills, conveyor/routing
models, advanced item logistics, turrets, units, scenery and water are preserved.
The broader family inventory remains in `A2-16a-3D-MODEL-AUDIT.md`; this table is
its manufacturing update.

## Geometry, texture mapping and animation

`ManufacturingModelProfile` explicitly identifies these six native blocks.
`ManufacturingModelGeometry` generates immutable templates using the existing
mechanical primitives and recessed resource-port foundation. Foundations have
one decorative mouth per footprint tile on each side. Upper components use
cropped local X/Y portions of the original block texture; side materials sample
an opaque native texel once per atlas/type. Atlas half-texel insets and filtering
remain unchanged. No external texture, native dependency, custom shader,
particle system or dynamic light is added.

`ManufacturingModelRenderer` submits those cached templates through the existing
opaque atlas stream on both ground and elevated floors. The old generic body is
excluded for these blocks, preventing duplicate overlapping bodies. Geometry
stays within the existing footprint and height; the underside is closed for
views from below. Three LODs retain mechanical detail nearby, simplify facets at
18 tiles and use the original identifying roof on a cheap body beyond 50 tiles.
The templates/material arrays are reused across instances; animation changes
transforms, not the cached mesh.

Press strokes use native `progress`, gated by enabled/efficiency/`shouldConsume`
and native `warmup`. The Multi-Press heads use opposite cycle phases. Rods span
from each moving head to the fixed beam, and the deepest stroke does not pass
through the bed. Input, power, disable or output-capacity stops cannot leave a
press performing full working strokes. Mixers rotate from native `totalProgress`
and can coast with the native cooling warmup; they do not advance from global
render time. Furnace glow follows native warmup, with restrained progress-driven
pulsing while active. Original DrawFlame color/top regions are reused. Cooling
heat is allowed to fade naturally. Mixer material tint appears only with actual
inventory. No effect creates, consumes, transfers or duplicates resources.

The renderer never calls native update, craft, consume or draw callbacks. Native
simulation remains responsible for recipes, power/liquid use, output and rates.
Paused production consequently freezes mechanical movement. Block rotation flags
remain native: non-rotatable machines are not made rotatable by this pass.

## XYZ construction and scope

The same existing terrain/platform plane anchors the models. Physical heights
remain **1.26 tiles for size 2** and **1.64 tiles for size 3**. At the current
1.4-tile floor spacing and 0.16-tile slab thickness, a platform on the immediately
following floor would intersect either factory size. Tests therefore use a valid
same-X/Y stack at **Z1/Z3/Z5**, and separately offset factories at Z2/Z4. The
adjacent-floor intersection is explicitly tested as rejected. No collision,
headroom, support, resource or placement validation is relaxed to fit the model.

Only the two existing rendering integration classes change at runtime, plus the
three new manufacturing implementation files. No production definition,
construction scheduler, logistics behavior, camera/input, combat, movement,
multiplayer protocol or save format is modified. Holograms and unfinished
ConstructBuilds retain the existing native blueprint presentation, rather than
running completed-machine animations inside a ghost. Selection/picking remains
based on the existing logical bounds. Multiplayer still requires identical mod
payloads.

## Verification boundaries and limitations

The new native tests cover all six definitions, cached finite geometry, every
rotation/LOD, active/blocked/disabled state, read-only serialization, real native
crafting, all upper layers, actual server-authorized placement, valid stacked
floors, invalid headroom, multi-floor clipboard/build completion and restoration.
The full existing regression suite covers the earlier construction, mining,
logistics, campaign, prediction, authority and input systems.

The actual SDL/OpenGL fixture uses disposable profiles only. Native update methods
run outside timed drawing with predefined input stocks/power/efficiency; progress
and outputs are advanced by the installed crafter implementation, never by the
renderer. This is a controlled functional/render fixture, not a manufacturing
throughput, power-network endurance or physical user-input acceptance test.
Owner gameplay, physical Linux GPU/input behavior and two-computer multiplayer
remain separate manual checks. Tiny details intentionally disappear with LOD;
decorative mouths are not new logical item sockets. No detailed animated ghost
meshes are introduced. Existing environment shadows, platform occlusion and
atlas batches are reused.

The test matrix, manual procedure and validation JSON accompany this report.
Final measured before/after rendering results are recorded below after validation.

## Cached geometry budget

| Profile | Near triangles | Medium triangles | Far triangles | All-LOD template bytes |
| --- | --- | --- | --- | --- |
| GRAPHITE | 280 | 216 | 10 | 48,576 |
| MULTI | 442 | 346 | 10 | 76,608 |
| SMELTER | 304 | 208 | 10 | 50,112 |
| CRUCIBLE | 472 | 352 | 10 | 80,064 |
| PYRATITE | 348 | 260 | 10 | 59,328 |
| BLAST | 328 | 252 | 10 | 56,640 |

All six immutable CPU templates across three LODs total **371,328 bytes**.
An owning renderer additionally caches 371,328 bytes of materialized UV arrays;
the ground and layer renderers each own their material cache, while the raw
templates are shared. Existing reusable GPU streaming buffers are separate.
Far roof quads/native flame planes are emitted in addition to these template
counts. There is no GPU mesh per individual factory.

A single cold generation sample took 6.180 ms for the first Graphite template
(including class initialization), and 0.005–0.129 ms for subsequent
templates/LODs in that run. These one-time samples are not stable throughput or
FPS measurements. Generation is not repeated per frame or per instance.

## Corrections found during validation

A preliminary test used an exact decimal comparison at the press-bed boundary;
it now uses a small floating-point tolerance. Early graphics fixtures attempted
construction before entering a game state and then with the builder occupying
the target volume. The fixtures were corrected to enter their disposable
editor/sandbox state and move the builder clear; placement checks were not
bypassed. Final render review also caught unmaterialized side-UV sentinels on
white furnace/material inserts. Those now resolve to the white region center,
with explicit submitted-UV bounds assertions on both Java runtimes. The final
artifact and benchmark use this correction; preliminary builds remain local.

## Final measured results

The final source passed **176 regression programs on Java17 and the same 176 against the packaged JAR on stock Steam Java25**. The new native manufacturing test passed 2,441,166 assertions; the authorized layered construction test passed 473. Independent compilation verifies **984 project classes** and all packaged resources against the final JAR.

The identical 24-scene graphics fixture completed **3,072 frames on each runtime**, plus 3,072 baseline frames, with zero GL errors, stable warmed model caches, drawing that leaves native world state unchanged, and an actual disposable disk SaveIO reload. Both final runtime fixtures observed real native/upper crafting output and output items on ground/upper conveyor belts. Factory/platform creation used normal validated actions under explicitly host-authorized Free Mode; no grid injection or collision relaxation. These are scripted Windows render checks, not owner controls, remote gameplay or physical Linux acceptance.

| Scene | CPU before → after, ms | GPU before → after, ms | Draws before → after | Submitted vertices before → after |
| --- | --- | --- | --- | --- |
| graphite | 0.535 → 0.842 | 0.067 → 0.087 | 24 → 24 | 57275 → 67973 |
| multi | 0.600 → 0.899 | 0.074 → 0.120 | 28 → 28 | 64985 → 80225 |
| smelter | 0.663 → 1.159 | 0.076 → 0.146 | 30 → 30 | 69947 → 88265 |
| crucible | 0.611 → 1.252 | 0.079 → 0.187 | 31 → 31 | 74609 → 98033 |
| pyratite | 0.640 → 1.172 | 0.083 → 0.224 | 32 → 32 | 77951 → 102233 |
| blast | 0.575 → 1.147 | 0.085 → 0.185 | 33 → 33 | 80771 → 104051 |
| stacked | 0.585 → 1.199 | 0.088 → 0.202 | 34 → 34 | 82475 → 105611 |
| field100 | 0.869 → 2.164 | 0.102 → 2.819 | 29 → 29 | 84593 → 146447 |
| field1024 | 1.880 → 1.626 | 1.840 → 0.153 | 68 → 66 | 225461 → 198077 |

These are entire-renderer submissions, including terrain, platforms, nearby belts and other blocks. The 100/1,024 fields contain a mixture of the six factories; distance LOD deliberately changes their detail with the same camera positions in each build. Additional geometry has a measured cost. One sequential sample does not establish FPS parity or a statistically reliable CPU improvement/regression. Stock Steam25 allocation counters are unavailable (`-1`); Java17 measurements remain in the evidence JSON. No claim of zero whole-frame allocations is made.

The materialized cache is **11,604 vertices / 371,328 bytes per owning renderer**, additional to the generic baseline. Raw immutable templates are shared; existing streaming buffers are separate. Both runtimes produce identical cache statistics, with no camera/time-only regeneration.

Final JAR SHA-256: `e17635068c0d23c54a645773e30cbba8a20d31fe03bbccfeafde31afe46dfa68`. Two existing rendering source files change; 13 existing class entries differ (including nested debug line metadata), 5 are new and 1,091 old class entries remain byte-identical. Native dependencies, notices, protocol, recipes, physics and input implementations are preserved. The payload fingerprint agrees between JAR and folder installations on both runtimes. Hash audits preserve 2,065 Alpha files and 93 earlier diagnostic assets. Corresponding source is exported from the final local Git checkpoint.
