# A2-16f — Chemical processing and liquid-production models

Baseline: owner-accepted A2-16e, `195cb152c66679f5a46c8850ca85abf2c3e85a92`, on `main`.
Unreleased local diagnostic; existing public releases and earlier test kits are preserved.

## Audit and updated model coverage

The eight requested blocks had no dedicated mechanical hulls in the ground or
layer renderer. They used the existing generic structural presentation. The
native mutable-layer adapter handles conveyors, filters, liquid-transport blocks
and unit payloads; it does not provide purpose-built models for this group.
The installed Mindustry 160.7 class APIs, initialized content, native drawers,
physical heights and existing layered adapters were inspected before integration.
Cultivator is an **AttributeCrafter**, not a separate Cultivator controller;
Water Extractor is **SolidPump**, and Oil Extractor is **Fracker**, a SolidPump
subclass. Their clocks differ from GenericCrafter clocks.

| Native block | Footprint / unchanged physical height in tiles | New presentation | State source |
| --- | --- | --- | --- |
| Cryofluid Mixer | 2×2 / 1.26 | Twin open tanks, bridge, two rotating mixing paddles | Crafter clock; separate water and cryofluid inventories |
| Melter | 1×1 / 0.88 | Open crucible, raised feed throat, heated front and small stack | Crafter warmup; actual slag level |
| Spore Press | 2×2 / 1.26 | Four-post press, moving platen, telescoping shaft and collection cup | Native activity/clock; oil inventory |
| Coal Centrifuge | 2×2 / 1.26 | Guarded open bowl, rotating central drum and peripheral weights | Crafter clock; oil inventory |
| Cultivator | 2×2 / 1.26 | Wet growing tray, supported spore clusters and overhead frame | Native progress/activity; water inventory |
| Water Extractor | 2×2 / 1.26 | Exposed pump stroke, flywheel, bridge and small reservoir | SolidPump `pumpTime`, `warmup`, `lastPump`; water inventory |
| Oil Extractor | 3×3 / 1.64 | Larger pumpjack, pivoted beam, attached reciprocating rod and reservoir | Fracker's inherited pumping clock/state; oil inventory |
| Pulverizer | 1×1 / 0.88 | Slotted crusher cage, spinning hammers and reciprocating crusher plate | Crafter clock/progress; native sand output remains unchanged |

Dedicated coverage for this requested group: **0/8 → 8/8**, on Z0 and eligible
upper floors. Combined A2-16d/e/f coverage is **20 explicit production blocks**.
Accepted drills, logistics, the previous twelve factories, units and turrets
retain their implementations. Optional **Incinerator is deferred**: its native
heat is a powered readiness indicator, not an extraction/production cycle. It
keeps its existing presentation; Erekir industry and liquid transport are outside
this pass. This table updates the broader A2-16a coverage inventory.

## Geometry, materials and animation

Three new chemical-model files reuse the accepted mechanical primitives, recessed
resource-port convention, distance LOD and native-atlas batch. The two existing
world renderers gain exact supported-block dispatch and cache disposal; upper
static chunks omit the generic duplicate body for these eight blocks. Previous
factory implementations are untouched.

Geometry is immutable and shared by profile, footprint and LOD. Atlas-dependent
material meshes are cached per type and owning renderer, invalidated with the
atlas. Roof UVs use native sprite art; opaque texels sampled inside that sprite
provide side materials. Material UVs stay within their atlas region with existing
half-texel insets. Liquid and spore inserts use the host white region and native
liquid/item colors. There are no external assets, new shaders or native libraries.

Near detail is used below 18 tiles, reduced geometry to 50 tiles, then the existing
cheap body/roof convention. Existing opaque streams batch by atlas texture and
retain depth/fog/frustum behavior. There is no per-frame geometry generation or
per-building persistent mesh. Cache/template memory and actual submitted geometry
are measured separately below; stream buffers are additional existing infrastructure.

Generic/AttributeCrafter animation reads `progress()`, `warmup()` and
`totalProgress()`. Pumps read their actual `pumpTime`, `warmup` and `lastPump`;
the native Water Extractor rotation multiplier is retained. The pumpjack beam
uses a bounded ±12° visual arc about a fixed bearing. Its rod reaches the moving
beam while retaining a fixed lower socket. Press and pump strokes stay inside
the existing physical envelope. Disabled, unavailable-input/power, output-blocked
and non-pumping states cannot perform full working strokes. Native warmup/clock
may visibly coast as the native simulation stops. Rendering never advances those
clocks from global render time, and calling it while paused cannot manufacture motion.

Fluid volumes are small opaque cosmetic inserts in bounded open vessels. Their
height follows **the named liquid's actual module amount / native capacity**.
Cryofluid Mixer uses two independently filled vessels; unrelated last-selected
liquids cannot recolor them. Empty/missing/non-finite state gives an empty vessel,
not invented output. The renderer neither fills nor drains modules, invokes native
draw callbacks, consumes resources, updates production, samples RNG nor spawns entities.

## Gameplay and construction boundaries

Recipes, native consumers, rates, power, inventories, transfer, collisions,
selection bounds, terrain, save formats, network protocols, controls and camera
are unchanged. Native update methods remain responsible for water/oil pumping,
cryofluid/slag/oil manufacture, spores, coal and sand. The runtime tests invoke
those real methods outside rendering and separately assert that repeated rendering
does not mutate resources or clocks.

All eight retain existing catalog eligibility and ordinary construction validation.
Size-2/3 machines need legitimate headroom; for same-X/Y stacks use Z1/Z3/Z5,
not platforms intersecting their roofs at adjacent floors. Tests use host-authorized
Free Mode for fixture platforms **without** disabling footprint, range or headroom
checks. Native terrain/attribute and resource requirements remain necessary for
actual production. The Oil Extractor fixture has native shale under its footprint;
upper adapters continue sampling the existing world attributes. No new permission
or resource exception is introduced.

## Limits and evidence scope

These are lightweight stylized mechanical models, not fully sculpted replicas.
Liquid inserts are opaque and do not simulate glass, flow, pipes or fluid physics;
from a low side angle their enclosing vessel can hide the surface. Far LOD retains
native roof art rather than full moving internals. Existing construction ghosts
and unfinished builds retain their own presentation, not operational machines.

Scripted Windows OpenGL captures, native-state tests and physical owner acceptance
are separate. Linux uses the same portable Java rendering path, but no physical
Linux device is exercised here. Remote two-computer multiplayer and real input/F5/
Tab acceptance remain manual. No measured renderer sample establishes gameplay
FPS parity or broad platform certification.

The new native test performs 10,519,663 assertions, including actual submitted
Oil Extractor rod vertices against the pitched beam attachment in both directions.
The construction test passes 513 checks, including 64 original machines and 64
queued copies. Final rod length uses the same orientation basis as the beam,
avoiding a reversed roll sign and differences between trigonometric approximations.

To rebuild from corresponding source, provide `FPV_JDK` (complete Java 17 JDK)
and `FPV_MINDUSTRY` (a separately installed Mindustry 160.7 directory), then run
`python tools/build-diagnostics.py --task a2-16f --run <new-local-run-name>`.
The normal builder compiles the project, runs all 180 regression programs and
packages a diagnostic JAR without replacing older outputs. Test-kit evidence
includes the graphics fixture, per-program results, mesh statistics, comparison
frames and binary/source/payload verification.


## Final measured results

The final source passed **180 regression programs on Java17 and the same 180 against the packaged JAR on stock Steam Java25**. Independent compilation verifies **997 project classes** and all packaged resources against the final JAR. Per-program evidence and the new native/construction assertions are preserved in the test kit.

The identical 27-scene graphics fixture completed **3,456 frames on each runtime**, plus 3,456 baseline frames, with zero GL errors, stable warmed caches, read-only drawing and an actual disposable disk SaveIO reload. Both final runtime fixtures observed native/upper production for each of the eight types and actual items on nearby ground/upper conveyors. Production is advanced outside timed rendering, with controlled stock and power. This is not a throughput or long-term capacity benchmark. Native substrate/upper placement rules remain active.

| Scene | CPU before → after, ms | GPU before → after, ms | Draws before → after | Submitted vertices before → after |
| --- | --- | --- | --- | --- |
| cryo | 0.540 → 0.745 | 0.063 → 0.096 | 24 → 24 | 54545 → 63581 |
| melter | 0.506 → 0.831 | 0.066 → 0.113 | 25 → 25 | 58793 → 72047 |
| press | 0.639 → 1.026 | 0.070 → 0.126 | 30 → 30 | 68267 → 85529 |
| centrifuge | 0.509 → 1.053 | 0.078 → 0.182 | 31 → 31 | 72323 → 95579 |
| cultivator | 0.622 → 1.177 | 0.074 → 0.199 | 32 → 32 | 76523 → 105353 |
| water | 0.685 → 1.197 | 0.083 → 0.204 | 34 → 34 | 80459 → 109001 |
| oil | 0.631 → 1.606 | 0.088 → 0.229 | 39 → 39 | 88367 → 119819 |
| pulverizer | 0.809 → 1.274 | 0.085 → 0.190 | 39 → 39 | 88865 → 116327 |
| stacked | 0.660 → 1.518 | 0.080 → 0.217 | 35 → 35 | 81653 → 109955 |
| field100 | 1.729 → 2.325 | 0.309 → 1.792 | 29 → 29 | 120011 → 150809 |
| field1024 | 1.747 → 1.677 | 0.227 → 0.166 | 68 → 67 | 216875 → 205733 |

These measurements cover the entire renderer, including terrain, platforms, belts, power and other machines. Dense fields contain eight new/eight accepted machine types. The same camera activates different distance LODs across the field. Additional geometry can have a measured cost; one sequential sample does not establish FPS parity or statistically reliable CPU differences. Stock Steam25 allocation counters are unavailable (`-1`); Java17 counters, where available, are included in JSON. No claim of zero whole-frame allocations is made.

The new materialized cache is **15,456 vertices / 494,592 bytes per owning renderer**, in addition to retained earlier-family caches. Raw templates are shared and measured separately; existing streaming buffers are additional. Cache statistics agree across runtimes, with no camera/time-only regeneration.

Final JAR SHA-256: `c759acd80c4aecdb7756175b6010c9db39e71525470c98a16601bf7713d26587`. Only two existing rendering source files and diagnostic task registration change, alongside the new scoped renderer/tests/docs. 13 existing class entries differ (including nested debug line metadata), 7 are new and 1,102 old class entries remain byte-identical. Native dependencies, notices, protocol, recipes, physical heights and input implementations are unchanged. Strict payload fingerprints agree for JAR/folder installations on both runtimes. Hash audits preserve 2,065 Alpha files and 99 previous diagnostic assets. Corresponding source is exported from the final local Git checkpoint.

The prior-only comparison changes 1374 pixels across the whole frame, with 0 differences in the image region x0–960/y200–600. See validation JSON for difference bounds; previous implementation classes remain byte-identical.
