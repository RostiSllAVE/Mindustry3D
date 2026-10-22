# A2-16h — Liquid logistics and Thermal Generator

Baseline: owner-accepted A2-16g `b373e1a033eb759f8039d1c787bc4b6dd4b7b301` on `main`.
Local diagnostic only; no release, Workshop or remote update.

## Audit and implementation

All twelve requested Serpulo liquid blocks exist in installed 160.7, including
Impulse Pump. Together with Thermal Generator, all thirteen receive dedicated
profiles. See `A2-16h-MODEL-COVERAGE.md` for native names, footprints, capacities
and silhouettes. Original liquid bridges already had functional XYZ/native span
rendering; it is reused. No accepted power/factory/unit model was replaced.

Three scoped classes extend the existing mechanical primitives, live native
texture atlas, batched geometry stream and 25/80-tile logistics LOD convention.
Shared immutable templates are keyed by profile, connection mask and LOD;
materialized copies are cached per atlas/type/renderer. Conduit masks use native
Conduit.blends and the building's own nearby tile view, preserving floor identity
and native team/direction rules. No texture generation, external asset, new
native dependency or new rendering framework is introduced.

Straight/corner channels meet exactly at cell edges. Their visible liquid uses a
common world-space channel bottom/top across all three conduit variants. Cached
arms and closing walls follow actual neighboring pipes; reinforcement changes
the housing, not the transport rules. Storage uses open inspection chambers with
industrial frames, rather than costly transparent glass layers. Bodies, rotors,
fluid volumes and indicators share the existing opaque/depth-tested atlas stream.

## Real quantities and activity

Visible liquid is derived from native currentAmount / liquidCapacity, clamped
with non-finite/missing-state fallback to zero. Water, oil, slag, cryofluid and
other native liquid colors are read directly. Empty modules emit no liquid mesh.
Junctions without a liquid module do not invent stored contents. Storage fill
changes actual displayed chamber height; pipe fill stays inside the inspection
channel, below the side protection. Native material paint may retain blue/purple
accents even when empty; it is not an invented liquid quantity.

The native LiquidModule flow-rate observation, where already available, drives
small **non-directional** inspection-light fluctuations. Rendering never calls
updateFlow or starts measurement, removes liquid, advances routing or infers a
direction from fullness. If the host is not tracking native flow, the safe
fallback is static real fill. This deliberately does not pretend that every
nonempty or blocked pipe is flowing.

Pump impellers use native PumpBuild.totalProgress. Operating lights require actual
warmup, available pump surface, efficiency, enabled state and native shouldConsume.
Native coast-down may remain visible. Thermal Generator reads real native
productionEfficiency for restrained heat-window illumination; it has no reliable
independent mechanical clock, so its heat exchangers remain stationary. Global
render time affects cosmetic light variation only while activity is positive and
is guarded against NaN/Infinity. Drawing never updates pumps, native power graphs,
heat, resources or production.

## Bridges and construction

Existing NativeBridgeLinks, native linkValid and accepted LayerVerticalTransfers
edges remain the sole sources of bridge visibility/transport. Only presentation
attachment heights change to meet the new terminals. Ground bridge support spans,
native arrows and existing cross-floor ribbons remain in their established paths;
unrelated item bridges, weapon lasers and power-beam logic are unchanged. Phase
connections do not gain a new transfer mechanic. Existing simulation owns range,
ownership, destination validation, removal and save restoration.

Actual approved sandbox fixtures place all twelve liquid types on Z1–Z5 and
stack Z1/Z3/Z5. An existing authorized phase-liquid bridge is captured/restored by
the current 3D clipboard. Ninety-six original blocks and ninety-six queued copies
remain independent; copied storage starts empty. Existing construction queues,
F3 cancellation, priority, costs, permissions and replication formats are reused.

Pumps are functionally tested on real water-bearing ground; Thermal Generator is
tested on native hotrock. An ordinary upper platform has no liquid-drop/heat
attribute: approved sandbox pump placement is idle there, and rendering creates
no extraction or geothermal production. The graphics gallery does not force a
Thermal Generator onto an elevated platform. Synthetic transform tests of its
model do not establish a valid elevated geothermal site. No new placement bypass
or terrain source is introduced.

## Validation limits and rebuild

Automated native/GL validation is separate from owner physical acceptance.
Windows F5/Tab/mouse acceptance, physical Linux/X11/Wayland, real remote
multiplayer and sustained gameplay FPS require manual testing. The same JAR uses
the existing Windows/Linux paths; this pass changes no input/native libraries,
network protocol, collision, save format, capacity, pump rate or power output.

Tall storage/pump geometry is a presentation envelope; existing physical bounds
and logical blueprint/selection outlines remain unchanged. Standard floor gaps
fit the new models. Extremely tight custom floor spacing can visually intersect
them. Chamber liquids are stylized opaque inspection volumes, not refractive
glass. Native flow observations may be unavailable, in which case flow animation
falls back safely to static fill. No additional Erekir models are included.

Rebuild with `FPV_JDK` (Java17 JDK), `FPV_MINDUSTRY` (separately installed 160.7)
and `python tools/build-diagnostics.py --task a2-16h --run <new-run-name>`.
Older releases, profiles, saves and diagnostic artifacts are preserved.

## Final measured results

The final packaged binary passed **185 regression programs on Java17 and the same 185 on stock Steam Java25**. Independent compilation verifies **1016 project classes** plus packaged resources byte-for-byte. The new native tests include actual water extraction/geothermal output, native corner/floor/team identity, 96 original/96 queued copied blocks and real internal bridge restore; copied liquid stores are empty. Existing input, prediction, networking, campaign, priority/cancel, power and manufacturing regressions pass.

The identical 41-scene actual Windows SDL/OpenGL gallery completed **5,248 frames per final runtime**, plus 5,248 A2-16g baseline frames, with zero GL errors. Drawing preserves native state and warmed caches. Each final run observes ground production from all three pumps and Thermal Generator, no upper pump production on dry platforms, 97 cumulative authorized upper placements and one accepted existing XYZ liquid bridge. A real disposable SaveIO disk reload, ghosts/clipboard/unfinished builds, several liquids, empty/full storage, pipe corners, bridge target removal and low/below views are included. Native updates and prescribed test quantities occur outside timed drawing. They are not a throughput/startup/physical acceptance benchmark.

| Scene | CPU before → after, ms | GPU before → after, ms | Draws before → after | Vertices before → after |
| --- | --- | --- | --- | --- |
| conduit | 0.318 → 0.311 | 0.247 → 0.223 | 27 → 26 | 58163 → 62213 |
| plated | 0.377 → 0.379 | 0.248 → 0.215 | 33 → 32 | 68441 → 75191 |
| tank | 0.604 → 1.016 | 0.750 → 0.888 | 46 → 45 | 88757 → 108749 |
| mechanical | 0.351 → 0.665 | 0.759 → 0.907 | 46 → 45 | 91841 → 108107 |
| rotary | 0.401 → 0.967 | 0.782 → 0.913 | 52 → 51 | 95813 → 119657 |
| impulse | 0.530 → 0.948 | 0.780 → 0.905 | 53 → 52 | 96299 → 121139 |
| thermal | 0.591 → 0.991 | 0.798 → 0.922 | 52 → 51 | 98723 → 119093 |
| stacked | 0.490 → 0.853 | 0.856 → 0.876 | 53 → 52 | 101963 → 122015 |
| pipes | 0.551 → 0.944 | 0.820 → 1.176 | 34 → 34 | 97607 → 133175 |
| field100 | 0.975 → 1.107 | 0.877 → 1.569 | 40 → 39 | 107135 → 126305 |
| field1024 | 1.561 → 2.781 | 1.479 → 4.838 | 118 → 119 | 246347 → 331313 |
| bridge-links | 0.526 → 0.550 | 0.810 → 0.810 | 36 → 37 | 92351 → 93845 |

These timings cover the entire renderer and native terrain/material mix, not isolated model cost. Dense fields alternate the thirteen new profiles and thirteen retained power profiles; camera distance selects LOD per instance. Extra geometry has a measurable cost, and one sample cannot establish gameplay FPS parity or reliable CPU differences. Full Java17 allocations are retained in JSON; Steam25 provides no allocation counter here. No zero-allocation whole-frame claim is made.

The bounded worst-case raw cache has **174 unique templates / 2,441,664 vertex-data bytes** (all pipe masks ×3 LODs, with non-pipe masks shared). A single cold generation sample is included, not an FPS result. The final gallery materialized cache is **16,266 vertices / 520,512 bytes per owner**; both runtimes agree. Ground and upper owners reuse immutable templates but have separate atlas material caches. Native buffers/driver overhead are additional. Unchanged per-instance liquid/clock state does not regenerate geometry.

Final JAR SHA-256: `9e72ec58c1bc7545dd14e01bc3d14cf8e660b784d464f1c3dd6661f5d5e1f8e8`. The audit finds 1,122 pre-existing class entries byte-identical, 13 changed existing entries from the two renderer integration sources/nested debug metadata, and 6 new scoped entries. Previous power models, native dependencies, notices and gameplay implementations remain byte-identical. Strict folder/JAR fingerprints agree on both Java runtimes. Hash checks preserve 2,065 Alpha files and 105 prior diagnostic assets. Corresponding source is exported from the final local commit.

Prior-only screenshot comparison changes 2009 pixels with bounds (294, 105, 960, 338); no cause is inferred from those pixels. Physical Windows/Linux visual acceptance, remote two-computer gameplay and sustained gameplay FPS remain NOT TESTED.

## Focused thermal-state check

A separate same-camera scene places only Thermal Generator on native hotrock and prescribes normal native enabled/efficiency flags while native updateTile calculates production. It verifies the orange working windows against the dark disabled/idle windows. This resolves a limitation of the broad gallery, whose direct native production updates did not initialize the generic efficiency flag for generator presentation. No gameplay source was changed for this follow-up. Three scenes /384 additional frames on each final Java runtime pass with zero GL errors and unchanged native state while drawing. These focused scenes do not perform or claim a save/reload. Their screenshots, full result JSON and pixel-difference bounds are included in the test kit.
