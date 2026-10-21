# A2-16e — Advanced manufacturing models

Baseline: owner-accepted A2-16d, `d3f87cc2910957405c0cc8045e7349a3d24f36be`,
on `main`. The owner confirmed the prior models, animations and elevated
placement. This is an unreleased local diagnostic; public releases are preserved.

## Audit and coverage inventory

The installed 160.7 native class APIs/content were checked against the existing
Z0/upper renderer, native drawers and layered building adapters. The six requested
blocks had no dedicated models. Kiln, Compressor, Weaver and Surge used generic
crafter bodies/warmup cues; Separator and Disassembler used generic structural
bodies. The latter are **Separator**, not GenericCrafter, with their own weighted
outputs, RNG seed, native consumers and serialization.

| Actual block | Size | Native class / craft time, ticks | Dedicated presentation |
| --- | --- | --- | --- |
| `kiln` | 2×2 | GenericCrafter / 30 | Chamfered tunnel furnace, framed hot mouths, chimney and native flame top |
| `plastanium-compressor` | 2×2 | GenericCrafter / 60 | Green industrial cage, opposed horizontal rams, attached telescoping rods and pressure core |
| `phase-weaver` | 2×2 | GenericCrafter / 120 | Open loom frame, sliding shuttle, native textured rotating weave assembly and restrained phase-colored strands |
| `surge-smelter` | 3×3 | GenericCrafter / 75 | Large stepped smelting basin, heated chamber, ribbed sides, twin stacks and native flame art |
| `separator` | 2×2 | Separator / 35 | Open centrifuge bowl, shaft, rotating vanes/native spinner and actual liquid indication |
| `disassembler` | 3×3 | Separator / 15 | Larger twin counter-rotating toothed drums, bearing housings, slotted guards and native spinner assembly |

New-family dedicated coverage: **0/6 → 6/6** on both ground and upper floors.
Manufacturing passes together now cover **12 explicit vanilla blocks**. Existing
six A2-16d profiles, drill/logistics models and all other families are retained.
Remaining factories, including Cryofluid Mixer and broader Erekir industry, keep
their existing presentation and are not silently included. This inventory updates
the broader family audit in `A2-16a-3D-MODEL-AUDIT.md`.

Native recipes remain lead/sand → metaglass; titanium/oil → plastanium;
thorium/sand → phase fabric; copper/lead/titanium/silicon → surge alloy.
Separator consumes slag and uses its original weighted copper/lead/graphite/
titanium result table; Disassembler consumes scrap/slag and retains its weighted
sand/graphite/titanium/thorium table. No probability, consumer, rate or output
definition is changed or reimplemented by the renderer.

## Model and animation implementation

Three new implementation files contain explicit profiles, cached geometry and
read-only presentation. They reuse `MechanicalBlockGeometry`,
`ResourcePortGeometry`, `SpriteExtrusionGeometry`, the accepted manufacturing LOD
distances and the existing opaque native-atlas streams. The A2-16d implementation
files are untouched. Integration adds only an explicit supported-family path in
the two existing ground/layer renderers; it excludes the old duplicate structural
body for these six blocks.

Static templates are shared by profile/footprint/LOD. Native UV materials and small
alpha-silhouette weave/spinner meshes are cached per type/atlas, not per building
or frame. Foundations have the accepted one recessed decorative resource mouth
per footprint cell on each side. Native roof art is cropped onto the mechanical
surfaces; opaque native texels supply side colors. Stretched piston rods use a
material texel, so they cannot pull UVs into neighboring atlas regions. White
energy/liquid inserts also use valid local UVs. No new texture, shader, dynamic
light, particle system, native library or rendering framework is introduced.

All animation reads native `Building.progress()`, `warmup()` and
`totalProgress()`, supported by both crafter subclasses. Compression follows the
actual cycle and activity. Ram rods span from fixed housings to moving heads;
the heads stop before crossing the central die. The phase shuttle follows the
native accumulated clock and activity, with the native weave texture turning at
its original clock rate. Separators retain the native spinner rate of three
degrees per accumulated tick; Disassembler's paired visual drums counter-rotate.
Their circular cross sections use consistent radial dimensions, keeping them
above the tray and inside their guards throughout rotation. Native warmup allows
appropriate cooling/coasting; disabled, input/power-starved or full machines
cannot appear to perform full working strokes. No animation advances solely
because global render time advances.

Kiln/Surge heat uses their original DrawFlame colors and top regions, with small
native-clock pulses during activity and cooling warmup afterward. Phase strands
use the original phase-fabric palette. Separator liquid indication reads the
actual liquid and amount, not a fabricated batch or resource token. Drawers are
inspected once for cached regions; rendering never invokes their draw callbacks,
simulation updates, consumers, random selection or resource transfer.

Near geometry is used below 18 tiles, simpler medium detail to 50 tiles, and a
cheap body/native roof beyond 50 tiles. Depth testing and existing platform/fog/
frustum rules remain active. Completed machines alone use this path; unfinished
ConstructBuilds, holograms and selection outlines retain the existing construction
presentation rather than pretending to be operational factories.

## XYZ and gameplay boundaries

Models use the actual native size and existing terrain/platform anchor. No visual
or physical height override is added: size-2 bodies retain 1.26 tiles and size-3
bodies 1.64 tiles. The current 1.4-tile floor spacing and 0.16-tile slab thickness
mean an immediate next-floor slab would intersect these factories. Valid tests
therefore stack at **Z1/Z3/Z5**, with separate offset fixtures on Z2/Z4. The invalid
adjacent ceiling remains explicitly rejected.

Upper placements use normal validated actions with explicitly host-authorized
Free Mode, real footprint slabs and builder range. No direct grid injection,
support bypass or collision relaxation is used. The clipboard test captures 48
factories, preserves all source plans, checks rotation/encoding/preview, builds
48 copies through the existing scheduler, and restores all 96 placements. Native
inventory, production progress and separator RNG streams are not cloned.

Only presentation integration changes at runtime. Native production, liquid/power
handling, item routes, costs, construction speed/priority/cancellation, hitboxes,
physics, combat, camera/input, multiplayer protocol and save format are unchanged.
Cross-floor Mass Driver transport is explicitly outside this pass and remains
unchanged. Identical mod payloads are still required for multiplayer.

## Validation and known limits

The new native model test exercises actual content, finite bounded transformed
geometry/UVs, all stored rotations/LODs, Z0/Z1/Z3/Z4/Z5, operating stops, attachment
bounds, read-only serialization and real native outputs. Separator serialization
includes its seed; repeated drawing cannot advance that RNG or its unsaved native
animation clock. The construction test checks approved Z1–Z5 placement, valid
stacked floors, invalid headroom, actual multi-floor copying/building and state
restoration. Existing A2-16d and complete earlier regressions are retained.

The graphics fixture uses disposable profiles, actual SDL/OpenGL and the stock
host textures. Native update methods advance production outside timed drawing,
with predefined input stocks/power/efficiency. Every new type must produce actual
output on Z0 and upper floors; native output must reach neighboring belts. This
is a controlled functional/rendering fixture, not a factory throughput, power-
network endurance or physical control acceptance benchmark. SaveIO operates only
on disposable maps/profiles.

The first graphics review found Disassembler's original full side guards hid its
rollers too much. Those guards now have openings and bearing frames, preserving
the same footprint/height and showing its mechanical motion from lower angles.
Normalized ram UVs have an explicit material-texel guard and submitted-UV checks.
These are visual corrections, not changes to the physical building.

Physical owner visuals, Linux GPU/input behavior and two-computer multiplayer
remain manual acceptance items. Small mechanical details intentionally vanish
with LOD. Decorative ports are not new logical sockets. Native animation clocks
may restart on reload as in vanilla; no new save fields are introduced to persist
purely visual phase. Existing blueprint ghosts remain native textured previews,
not detailed animated factory meshes. Dense close views add geometry; no gameplay
FPS improvement or parity is claimed.

Final full-suite results, geometry budgets and measured before/after rendering
costs are recorded below and in the accompanying validation JSON/test matrix.

## Cached geometry budgets

| Profile | Near triangles | Medium triangles | Far triangles | All-LOD raw bytes |
| --- | --- | --- | --- | --- |
| KILN | 282 | 206 | 10 | 47,808 |
| COMPRESSOR | 340 | 276 | 10 | 60,096 |
| WEAVER | 338 | 262 | 10 | 58,560 |
| SURGE | 466 | 318 | 10 | 76,224 |
| SEPARATOR | 340 | 252 | 10 | 57,792 |
| DISASSEMBLER | 560 | 458 | 10 | 98,688 |

Shared raw templates total **399,168 bytes**. Materialized UV arrays and cached native weave/spinner silhouettes are additional per owning renderer and are measured separately below; existing reusable GPU streams and accepted-family caches are separate. Far roof quads/flame regions are additional to raw template counts. No per-building GPU mesh is created.

One cold CPU sample took 5.883 ms for the first template (including class initialization) and 0.007–0.177 ms for later templates/LODs. These one-time values are not stable throughput/FPS measurements; no frame regenerates these templates.

## Final measured results

The final source passed **178 regression programs on Java17 and the same 178 against the packaged JAR on stock Steam Java25**. The new native manufacturing test passed 3,181,238 assertions; the authorized layered construction test passed 489. Independent compilation verifies **990 project classes** and all packaged resources against the final JAR.

The identical 25-scene graphics fixture completed **3,200 frames on each runtime**, plus 3,200 baseline frames, with zero GL errors, stable warmed model caches, drawing that leaves native world state unchanged, and an actual disposable disk SaveIO reload. Both final runtime fixtures observed real native/upper crafting output and output items on ground/upper conveyor belts. Factory/platform creation used normal validated actions under explicitly host-authorized Free Mode; no grid injection or collision relaxation. These are scripted Windows render checks, not owner controls, remote gameplay or physical Linux acceptance.

| Scene | CPU before → after, ms | GPU before → after, ms | Draws before → after | Submitted vertices before → after |
| --- | --- | --- | --- | --- |
| kiln | 0.493 → 0.784 | 0.064 → 0.099 | 24 → 24 | 55949 → 66023 |
| compressor | 0.549 → 0.881 | 0.068 → 0.111 | 27 → 27 | 62501 → 78221 |
| weaver | 0.604 → 0.970 | 0.069 → 0.154 | 30 → 30 | 69239 → 89489 |
| surge | 0.676 → 1.184 | 0.078 → 0.190 | 31 → 31 | 75437 → 102611 |
| separator | 0.629 → 1.186 | 0.084 → 0.234 | 32 → 32 | 77297 → 107291 |
| disassembler | 0.568 → 1.164 | 0.084 → 0.184 | 33 → 33 | 80831 → 109745 |
| stacked | 0.752 → 1.192 | 0.089 → 0.203 | 34 → 34 | 82283 → 110735 |
| field100 | 1.688 → 2.170 | 0.233 → 4.224 | 29 → 29 | 112481 → 153197 |
| field1024 | 1.646 → 1.502 | 0.184 → 0.147 | 68 → 67 | 210359 → 200543 |

These are entire-renderer submissions, including terrain, platforms, nearby belts and other blocks. The 100/1,024 fields contain a mixture of the six new and six accepted factories; distance LOD deliberately changes their detail with the same camera positions in each build. Additional geometry has a measured cost. One sequential sample does not establish FPS parity or a statistically reliable CPU improvement/regression. Stock Steam25 allocation counters are unavailable (`-1`); Java17 measurements remain in the evidence JSON. No claim of zero whole-frame allocations is made.

The materialized cache is **13,626 vertices / 436,032 bytes per owning renderer**, additional to the prior six-family cache retained in A2-16d. Raw immutable templates are shared; existing streaming buffers are separate. Both runtimes produce identical cache statistics, with no camera/time-only regeneration.

Final JAR SHA-256: `4f68ecbbfd5ee2e6a1b4cb58c732c0d6e4a457092486203cff1bcba37cc76dbd`. Two existing rendering source files change; 13 existing class entries differ (including nested debug line metadata), 6 are new and 1,096 old class entries remain byte-identical. Native dependencies, notices, protocol, recipes, physics and input implementations are preserved. The payload fingerprint agrees between JAR and folder installations on both runtimes. Hash audits preserve 2,065 Alpha files and 96 earlier diagnostic assets. Corresponding source is exported from the final local Git checkpoint.

The old-factory image region (pixels x100–850/y220–390) changes **0 pixels**. The whole frame changes 907 pixels, confined to the distant unit/construction-beam area above those factories. Accepted manufacturing implementation/classes remain byte-identical. Native output observations cover each new factory individually on ground and elevated floors.

## Dense-field repeat and performance limit

The elevated GPU cost in the first 100-factory sample prompted a separate two-scene repeat with identical before/after fixture code. Each build completed another 256 frames with zero GL errors; save/load is **not** exercised by this short repeat. The original 25-scene suites separately test actual disk save/load.

| Scene | Repeat CPU before → after, ms | Repeat GPU before → after, ms | Draws before → after | Vertices before → after |
| --- | --- | --- | --- | --- |
| field100 | 1.771 → 2.827 | 0.211 → 2.211 | 29 → 29 | 112481 → 153197 |
| field1024 | 1.832 → 2.161 | 0.200 → 0.163 | 68 → 67 | 210359 → 200543 |

The near-field cost is reproduced, although its magnitude varies between runs. This is a remaining detail/performance tradeoff, not evidence of FPS parity. No cause is assigned to driver or presentation behavior from these samples. Distant LOD keeps the 1,024-factory submission bounded and below the baseline vertex count. Actual gameplay FPS and acceptance on the owner's hardware still need testing; a broad renderer optimization is outside this model pass. The installed host identifies itself as official Steam build 160.7 in its version.properties.
