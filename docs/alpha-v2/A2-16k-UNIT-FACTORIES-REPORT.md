# A2-16k — 3D unit-production factories

Baseline: accepted A2-16j `bd1d3d6` on `main`. The owner accepted its core/Radar models and prior turret corrections. The full supplied A2-16k attachment and installed 160.7 definitions were inspected before editing. This is an unreleased diagnostic model pass.

## Scope and native audit

All six requested blocks exist in 160.7 as native `UnitFactory` buildings, each 3×3 with the existing physical height of 1.64 tiles. They previously used a general full-height body plus the native flat assembly layers. No accepted dedicated factory body was replaced. Erekir **Ship Fabricator produces Elude, a flying unit; it is not a naval factory**.

| Block | New silhouette | Original outputs / native build time in ticks |
|---|---|---|
| Ground Factory | Heavy side rails, rear mechanical towers, sliding assembly heads and open exit | Dagger900, Crawler600, Nova2400 |
| Air Factory | High rear gantry, suspended side machinery and clear central launch space | Flare900, Mono2100 |
| Naval Factory | Lower elongated cradle rails, keel-side supports and rear shipbuilding machinery | Risso2700, Retusa2100 |
| Tank Fabricator | Broad armored side banks, heavy rear engineering assemblies and low assembly bay | Stell2100 |
| Ship Fabricator | Tapered diagonal supports, overhead side equipment and open aerial bay | Elude2400 |
| Mech Fabricator | Tall rear supports, front mechanical mounts and side guide rails | Merui2400 |

These are distinct normalized templates, not differently coloured copies. Original floor, output-direction and top-frame artwork is reused from the host atlas. Roof UVs remain in footprint coordinates, with original frame art seated on actual raised surfaces rather than a floating full-height square. Opaque underpaint, inward-contact culling and complete visual envelopes reuse the preceding geometry fixes. Undersides remain visible from below. The shared decorative resource bays retain three openings per side on these 3×3 buildings.

## Animation and reusable architecture

Two lightweight carriages read the real native factory `time` clock in opposite phases. Their amplitude requires a valid plan, live/enabled building, positive native efficiency, native `shouldConsume()` and `speedScl`. Emitters/status details brighten with that same operational state. Starved, disabled, unconfigured and output-blocked factories do not animate at full production power. No separate wall-clock production simulation is introduced.

The existing `NativeBlockAnimationAdapter` supplies the original progress-clipped unit image, real payload location, blocked-output warning and actual-release flash inside the lower assembly bay. It creates no gameplay entities. Its original path for other UnitBlocks/reconstructors is retained. Only the new factory bay path rotates and confines its scanning line correctly with the block; the original world-axis offset otherwise exceeded the rotated footprint. Partial units remain a native sprite preview, not a newly simulated 3D actor.

Three distance LODs and cached normalized templates feed the existing atlas batch on Z0 and upper layers. Static upper chunks skip the new profiles to avoid duplicate bodies. There are no new shaders, rendering pipelines, dependencies, textures, input controls or per-frame mesh generation. Native fog/visibility gates and the complete model-envelope culling remain in force.

## Gameplay and XYZ safety

Production speed, all ten recipes, costs, caps, spawn rules, coordinates, hitboxes, AI, physics, controls, construction, protocols and save formats are unchanged. Render calls are read-only. Actual native production and `moveOutPayload`/release APIs are tested, rather than substituting synthetic `unit.add` calls. Every successful output clears the factory's physical envelope in its original output direction; no decorative collision geometry is added. Renderer state checks prove no additional production, spending or world mutation.

Authorized Free-Mode fixtures exercise eligible factories on Z1/Z3/Z5, same-column floors with sufficient spacing, adjacent structures, previews, real volumetric capture/paste and native save/load. They do not authorize new Structural-Mode locations or bypass span/support rules. Rendering fixtures also cover all six profiles at the same XYZ layers through existing native adapters.

**Naval restriction retained:** dry elevated slabs are not water. The 24 upper naval output attempts correctly retain the actual payload without duplicating units or charging again. Naval release is accepted on valid Z0 water, including all four directions. No elevated ocean, boat levitation or new placement rule is introduced. Existing shore/blocked-output tests remain in the suite.

## Geometry and measured costs

Six ground material caches are built once and reused: **563,712 bytes** of CPU templates, including original top-frame surfaces. The 18 shared raw templates occupy **540,288 bytes**. Layer rendering has its own material cache; these figures do not include it, host textures, cold-generation temporaries or existing stream/GPU buffers. Near/mid/far counts below are vertices, not draw calls; triangles are vertices ÷3. They exclude the variable native assembly/payload sprite and per-view omission of inward bottom faces.

| Factory | Near vertices | Mid | Far |
|---|---:|---:|---:|
| air-factory | 1554 | 1194 | 108 |
| ground-factory | 1554 | 1194 | 144 |
| mech-fabricator | 1674 | 1314 | 144 |
| naval-factory | 1698 | 1122 | 108 |
| ship-fabricator | 1638 | 1278 | 108 |
| tank-fabricator | 1554 | 1122 | 108 |

Serial baseline/final runs used stock Steam Java25, the same fixture/cameras, 960×600, two warmed 64-frame timing blocks per scene and screenshots outside the measurement windows. No compilation, other game instance or regression suite ran during these paired samples. Native activity/progress snapshots are scripted in a paused world to isolate **renderer** cost; this does not measure full AI/production simulation or OS presentation. The scene includes upper replicas as well as six foreground factories; the dense variant adds 256 factories. Culling/LOD still applies, so not all256 are necessarily visible.

| Scene | CPU before → after (ms) | GPU before → after (ms) | Draw calls | Submitted vertices |
|---|---:|---:|---:|---:|
| Six foreground factories + elevated replicas | 0.810 → 1.065 | 0.570 → 0.429 | 51 → 50 | 111,695 → 129,431 |
| Same fixture + 256 factories | 1.098 → 2.113 | 0.570 → 0.365 | 56 → 55 | 146,675 → 194,375 |

The final pair adds about +0.256 ms CPU in the close fixture and +1.015 ms CPU in the dense fixture. An earlier serial pair with the same geometry measured +0.220 / +0.702 ms CPU. GPU sample blocks vary substantially: the final close blocks span0.114–0.744 ms and the earlier dense pair was0.541→0.734 ms, so the lower GPU average in the final pair is not evidence of a speedup. The earlier pair predates only the outgoing-payload culling guard (inactive in these payload-free benchmark scenes). These short samples establish geometry cost, not sustained frame performance; exact blocks are preserved in the evidence. One previous coloured-body draw is removed by using the shared atlas batch. This is a measured rendering cost increase, not an FPS improvement. Short block averages vary; see the raw timing blocks in validation JSON. Steam Java25 lacks the optional per-thread allocation counter, so its allocation measurement is unavailable, not zero. Model-cache counts remain stable after warmup; no allocation-improvement claim is made.

## Validation and limitations

- **196 regression programs PASS on Java17 and stock Steam Java25**. Existing campaign, connection/prediction, mouse/input, construction priority/cancellation, 3D selection/clipboard, save/network and previous model regressions remain included.
- **17,136,473 native geometry/UV/state/cache checks / 1,440 poses** cover all six factories, four rotations, Z0/Z1/Z3/Z5, three LODs and active/disabled/starved/unconfigured/payload-blocked states. No native balance or serialized state changes occur during rendering.
- **136 real native releases PASS** across all ten plans, four directions and the allowed floors. **24 dry upper naval outputs correctly held**. Original costs are consumed once and repeated output calls do not create another unit.
- **35 actual SDL/OpenGL scenes / 4,480 frames on each runtime**, 0 GL errors. All six original outputs are created by native factory calls in each gallery; top/side/oblique/low views, disabled/partial production, stacked floors, below-plane view, dense fields, adjacency, floor relief, previews, real copy/paste and disposable SaveIO all pass. Original shader/pass-state probes also pass.
- Independent fresh compilation matches all **1,047 runtime classes**. The actual ModFingerprint agrees between JAR/folder payloads on both runtimes. 1,151 prior runtime classes remain byte-identical; changed existing classes are limited to render integration/shared animation extraction and their debug-line-dependent nested classes. Metadata, notices and dependencies are unchanged. All 2,185 protected earlier artifact files remain intact.

Physical Windows/Linux gameplay acceptance, current campaign transitions and two-computer multiplayer are **NOT TESTED** in this pass. Existing automated campaign/lifecycle regressions pass. No Linux-specific API is added; the same Java17-compatible JAR serves Windows/Linux, but Windows-hosted Java/SDL checks are not a physical Linux test.

Transient native clocks/efficiency on **remote elevated replicas** are not fully represented by existing snapshots; remote upper carriage/pulse behavior is not certified and may be static/dim. This task deliberately adds no packets or client-side production simulation. The native sprite-based partial unit remains flat. Large-scale full-simulation/FPS performance and very low-end GPUs need owner testing; the measured additional rendering cost is disclosed above.

An initial graphical save assertion incorrectly counted only awake `Groups.build` entities. The corrected fixture enumerates native tile anchors as well as upper entries, proving all six sleeping ground factories and 15 upper factories survive actual SaveIO. No save/gameplay change was needed. No primary profile/save was used or modified. A final conservative culling check also covers large real payload sprites overhanging the exit, without changing picking/collision bounds.

Reconstructors, advanced assemblers and other block families are deferred. No push, publication, Workshop update or previous-release overwrite occurred.
