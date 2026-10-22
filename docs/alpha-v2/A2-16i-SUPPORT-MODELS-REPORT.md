# A2-16i — Support, shields, boost and storage models

Baseline: owner-accepted A2-16h `77480dd30aebfd09b75890d3315448da74f55122` on `main`.
Local diagnostic only; no publication, remote push or Workshop update.

## Scope and shared machinery

All eleven prioritized models are implemented. The coverage inventory records
their installed native names, classes, footprints and optional barrier audit.
Existing Force Projector field rendering was already suitable and is retained.
Optional editor-only Erekir BaseShield models are deferred; their semantics are
not recast as ordinary player shield generators.

The new exact-identity profiles extend the existing mechanical primitives,
original Mindustry atlas materials, resource-port foundation and 18/50-tile
manufacturing LOD. Immutable normalized templates are shared by profile/size/LOD;
atlas-dependent copies are cached per type and owning renderer. Ground and upper
renderers share this implementation and omit duplicate generic bodies.
No textures are generated or loaded from developer files. No new renderer,
particle simulation, functional drone or native dependency is introduced.

Mechanical rings have real radial connections to the central stems. The Build
Tower head uses native base/head art and native buildRotation, with its indicator
transformed with the same head pivot. Storage uses distinct housings, original
roofs, reinforced corners and reusable loading arches proportional to cell size.
All geometry stays inside existing physical envelopes. Existing selection and
construction preview mechanisms retain their native footprint/height.

## Honest native states

Mender/Mend Projector have native readiness/charge/optional-booster state, but no
reliable public per-frame flag proving an actual heal occurred. Their local
illumination and mechanism show **readiness/charging**, not a fabricated outbound
healing pulse. No target scan or new healing beam/area is added. Actual native
healing is verified independently in the native behavior fixture.

Overdrive Projector/Dome use native heat, smoothEfficiency, charge and phaseHeat.
Their moving emitter assemblies are body-local. No sphere, projected range or
outbound field claims that unrelated upper floors receive a speed boost. Existing
native/XYZ targeting and stacking rules are untouched.

Regen Projector uses native **didRegen** for the active indicator, guarded by
enabled/efficiency, and native totalTime for mechanism pose. anyTargets or residual
warmup alone does not claim a heal. Optional efficiency is only a booster status
indicator. Build Tower uses native warmup with an outstanding, nonremoved native
lastPlan or a live following builder with queued work; it creates no work/drone.
Its native block-unit rotation is read without invoking a mutating control API.

Force Projector hardware uses native broken, warmup, radscl, buildup, phaseHeat
and native capacity for its available-shield indicator. Broken/depleted hardware
is dark; the real field remains governed by existing authoritative volume data.
No renderer calls a repair, boost, damage, resource-consumption or construction
method to make an animation appear active.

Storage indicators use the largest actual `items.get(item) /
getMaximumAccepted(item)` fraction. This is the **most-filled item slot**, not
total shared capacity. Native linked-core limits are respected. Empty, half,
full and linked-core inventories are tested. The display never exposes fictitious
items, modifies resources or duplicates inventory through a schematic.

## Shield boundaries and transparent effects

The existing ForceProjectorSystem/ForceProjectorRenderer are byte-preserved.
The field is a closed polygonal prism using the same native radius, sides,
rotation, actual building base and configured finite height multiplier used by
authoritative projectile contact. It is not a decorative spherical dome.
Native broken/recovery and actual hit-flash data drive its presence and color.
The existing depth-tested, faint additive shell and state restoration remain in
place. Opaque structures occlude it; geometry is frustum culled and batched into
the existing bounded streaming buffer.

No extra translucent layer, overlapping dome, expensive optional effect or
graphics toggle is added. The existing field is the lightweight presentation.
Its legacy allocations/transparent ordering are not rewritten as part of this
model pass. All shield collision, health, cooling, recharge and replication rules
remain unchanged.

## Construction and validation boundaries

Approved sandbox fixtures build all eleven types on Z1–Z5 and stack Z1/Z3/Z5,
then queue 88 independent copies of 88 source blocks. Copied support/storage
inventory is empty. Occupied locations remain invalid. Existing construction
queues, costs, native authority, F3 interruption, priority, 3D clipboard and
save/replication formats are reused.

Native tests separately demonstrate actual Mend healing, eligible-building
acceleration and Regen healing under unchanged existing behavior. Force shield
contact/removal/same-XY different-floor and Build Tower reconstruction remain
covered by existing regressions. Render tests confirm that inventory, clocks,
native readiness, shield state and work state are not mutated by drawing.

Graphics gallery model scenes use explicitly prescribed native snapshots to
exercise poses and levels, including a no-repair state. These are presentation
fixtures, not autonomous production/repair-throughput tests. Dedicated shield
scenes use actual native updateTile recovery/depletion and a real authoritative
trace/hit call outside timed drawing. SaveIO uses a disposable profile/map.

Physical owner Windows/Linux visual/input acceptance, remote two-computer
multiplayer and sustained gameplay FPS remain separate from automated native and
Windows SDL/OpenGL checks. No Linux hardware performance certification is made.
The same portable JAR preserves existing Java17/Steam25 and Windows/Linux code.

Rebuild using `FPV_JDK` (Java17 JDK), `FPV_MINDUSTRY` (separately installed160.7)
and `python tools/build-diagnostics.py --task a2-16i --run <new-run-name>`.
Old releases, diagnostics, primary profiles and saves are preserved.

## Final measured results

The final packaged JAR passes **187 regression programs on Java17 and the same 187 on stock Steam Java25**. Independent compilation verifies **1022 project classes** and packaged resources byte-for-byte. Native tests cover all eleven blocks, real Mend/Overdrive/Regen behavior, linked-core capacity, 88 source buildings/88 queued copies and empty copied inventories. Existing shield contact, tower reconstruction, input, networking, campaign, cancellation/priority, selection/clipboard, power, manufacturing and liquid regressions pass.

The identical 44-scene actual Windows SDL/OpenGL gallery completes **5,632 frames per final runtime**, plus 5,632 accepted A2-16h baseline frames, with zero GL errors. Drawing preserves sampled native state and warmed caches. Each run has 89 cumulative authorized upper placements, one real authoritative shield trace/hit and a real disposable SaveIO disk reload. Model scenes use prescribed native readiness/work/storage snapshots; dedicated shield scenes use native updateTile recovery/depletion. They show 54 shield faces for three active generators and zero when all are depleted. No new XYZ connection is needed in this fixture. These are scripted checks, not owner input acceptance or autonomous repair-throughput benchmarks.

| Scene | CPU before → after, ms | GPU before → after, ms | Draws before → after | Vertices before → after |
| --- | --- | --- | --- | --- |
| mender | 0.430 → 0.559 | 0.062 → 0.063 | 26 → 26 | 59249 → 63491 |
| mend | 0.428 → 0.586 | 0.063 → 0.065 | 29 → 29 | 64283 → 72485 |
| overdrive | 0.395 → 0.591 | 0.067 → 0.070 | 32 → 32 | 70373 → 81605 |
| dome | 0.453 → 0.742 | 0.070 → 0.076 | 34 → 34 | 74969 → 88667 |
| force | 0.447 → 0.781 | 0.072 → 0.100 | 35 → 35 | 78095 → 99383 |
| container | 0.478 → 0.828 | 0.076 → 0.104 | 38 → 38 | 83885 → 105299 |
| vault | 0.442 → 0.812 | 0.089 → 0.116 | 43 → 43 | 92495 → 116333 |
| regen | 0.476 → 0.850 | 0.079 → 0.118 | 43 → 43 | 93173 → 117167 |
| build | 0.437 → 0.963 | 0.079 → 0.141 | 44 → 44 | 96251 → 127481 |
| reinforced-vault | 0.393 → 0.866 | 0.176 → 0.234 | 47 → 47 | 104909 → 135917 |
| stacked | 0.515 → 1.140 | 0.178 → 0.208 | 48 → 48 | 108125 → 137231 |
| field100 | 0.999 → 1.369 | 0.152 → 0.177 | 33 → 32 | 101459 → 126095 |
| field1024 | 3.050 → 2.753 | 2.376 → 1.432 | 67 → 67 | 276545 → 286109 |
| storage1024 | 1.307 → 2.039 | 0.160 → 0.197 | 66 → 65 | 203903 → 215183 |
| support512 | 0.925 → 1.521 | 0.117 → 0.120 | 66 → 65 | 199667 → 209699 |
| shield-overlap | 0.434 → 0.454 | 0.084 → 0.084 | 35 → 36 | 90323 → 91811 |

Timings cover the complete renderer, terrain and visible material mix; draws/vertices are one actual proxied frame. One sample cannot establish sustained gameplay FPS or reliable CPU parity. Detailed per-scene Java17 allocations are retained in JSON; Steam25 provides no allocation counter. Body-only dense scenes omit fields intentionally; the overlap scene measures the unchanged real field separately. No FPS improvement or zero-allocation whole-frame claim is made.

The near templates contain **354–1308 vertices** per model. The 33 shared profile/LOD templates total **497,280 raw vertex-data bytes**. A cold generation sample is recorded per template, not an FPS result. Each warmed gallery owner caches **11 types / 15,540 vertices / 497,280 bytes** across all LODs; both runtimes agree. Ground/upper owners share immutable templates and have separate atlas material copies. Stream/native/driver buffers are additional; per-instance changing states do not rebuild meshes.

Final JAR SHA-256: `640922ae77f0b1552cdc665354caac5cfec2aea7f7527c4709f541df5a5c4554`. The audit verifies 1,128 pre-existing class entries byte-identical, 13 changed entries generated by the two renderer integration sources/nested metadata, and 6 new scoped entries. ForceProjectorSystem/Renderer, prior models, native dependencies and notices are unchanged. Strict folder/JAR payload identities agree on both runtimes. Hash checks preserve 2,065 Alpha files and 108 prior diagnostic assets. Corresponding source is exported from the final local checkpoint.

Prior-only screenshot comparison changes 0 pixels, bounds None; no cause is inferred. Physical Windows/Linux visual and input acceptance, real remote multiplayer and sustained gameplay FPS remain NOT TESTED. Optional editor-only BaseShield geometry remains deferred.
