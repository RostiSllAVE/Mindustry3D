# A2-16l — Assembly clipping fixes and 3D unit-upgrade facilities

Baseline: accepted A2-16k `ab5bd5d` on `main`. The owner reported overlapping factory textures and detached cyan indicators, and requested the next factories that upgrade units. This unreleased diagnostic pass fixes the six factory presentations and adds eight explicit vanilla Reconstructor profiles. Assemblers and unrelated models are outside this pass.

## Confirmed factory defects and corrections

The old normalized bay roof was at `0.189 × height`; the native output-direction layer was at `0.18 × height + 0.014`. At the native 3×3 factory height of 1.64 tiles that layer was **0.00076 tiles below the roof**. Their nearly coincident surfaces caused visible overlap. The shared assembly surface is now `base + 0.189 × height + 0.025`, with output art another 0.002 above it: **0.027 tiles of positive roof clearance**, independent of building size. Native unit preview, scanner and release-flash layers have separate positive clearances. No depth buffer override or global texture-sampling change is used.

The cyan nozzles previously used fixed normalized heights unrelated to each profile's arm height. Each nozzle now overlaps its metal shank by 0.003 normalized units and receives **the same translation as its own left/right carriage**. Stationary status strips sit on actual housings; the Ship Fabricator's strips sit on its front-post caps. Small profile-specific arm-origin/anchor corrections keep the moving metal away from fixed supports throughout the stroke. The six existing factory bodies otherwise retain their accepted silhouettes and artwork. A triangle-level sweep checks 476 carriage poses across all fourteen profiles.

A completed factory payload could also overlap the next partial-unit image while the selected plan remained active. The dedicated assembly-bay path now shows **one unit picture at a time**: a native progress preview while constructing, or the real payload otherwise. A reconstructor similarly avoids drawing the old input unit underneath the partial upgraded result. These images create no actors. Native progress-clipped art remains intentional; it is not missing geometry. The legacy adapter path for other/modded UnitBlocks is retained.

## Native scope and new models

All eight blocks below are native `Reconstructor` definitions in the installed Mindustry 160.7. Native footprint, physical height, recipes, health and timing are only read. The audit found 34 supported upgrade pairs: seven per Serpulo tier, plus one each for Tank/Ship/Mech and three for Prime. **Ship Refabricator upgrades Elude → Avert, a flying unit; it is not a naval factory.**

| Block | Footprint | Existing height, tiles | Native ticks | New mechanical silhouette |
|---|---:|---:|---:|---|
| Additive Reconstructor | 3×3 | 1.64 | 600 | Compact open gantry and side engineering banks |
| Multiplicative Reconstructor | 5×5 | 2.40 | 1800 | Double gantry, broader corner workstations |
| Exponential Reconstructor | 7×7 | 3.16 | 5400 | Heavy corner machinery and side tracks |
| Tetrative Reconstructor | 9×9 | 3.50 | 14400 | Massive tiered side machinery and twin cross tracks |
| Tank Refabricator | 3×3 | 1.64 | 1800 | Armored side banks and low central assembly opening |
| Ship Refabricator | 3×3 | 1.64 | 3000 | High aerial frame with open central work area |
| Mech Refabricator | 3×3 | 1.64 | 2700 | Mechanical posts, guide rails and mounted tools |
| Prime Refabricator | 5×5 | 2.40 | 3600 | Broad upper armor and substantial assembly tracks |

Both the native input and output corridors remain open. Original host atlas floor/output/top-frame textures and color identity are preserved, with footprint-space UVs on raised surfaces, opaque underpaint and the existing contact/underside handling. Decorative resource openings scale with native footprint. No new texture assets, shaders, dependencies or rendering pipeline were added.

## Animation, integration and authority

The reusable `UnitAssemblyParts` helper defines assembly-layer spacing and attached arm/nozzle geometry. `UnitUpgradeProfile`, `UnitUpgradeGeometry` and `UnitUpgradeRenderer` feed the existing atlas batch. Each native block type shares immutable normalized geometry across instances and has three distance LODs. Ground and upper renderers own their material caches; upper static chunks skip these new profiles to prevent doubled bodies. Complete bounds preserve culling, preview and existing visibility gates.

Opposing carriages read the real native `time` clock. Motion and tool brightness require a live/enabled building, finite positive efficiency, an arrived upgradeable payload, native `shouldConsume()` and native `speedScl`. Disabled, resource-starved, empty or in-transit machines do not pretend to be producing. No production clock, recipe, item, liquid, power or payload state is advanced by rendering. There are no synthetic outputs, completed-unit clones or extra costs. Native scheduling/output restrictions remain authoritative.

All 34 real upgrade pairs were exercised in four rotations on Z0/Z1/Z3/Z5: **448 actual native upgrade/release cases** passed; **96 dry upper naval outputs were correctly held**. A dry platform does not become water. Tall bays still require existing physical headroom; stacked graphics fixtures use Z1/Z4/Z7 where appropriate rather than forcing invalid adjacent floors.

## Geometry and performance measurements

Model material-template vertex counts below include body, lining, status details, carriages/tools and raised roof art; they exclude variable payload/progress images and view-dependent bottom omission. They are not individual draw-call counts. Triangles equal vertices / 3.

| Model | Near vertices | Medium vertices | Far vertices |
|---|---:|---:|---:|
| additive-reconstructor | 1602 | 1242 | 180 |
| exponential-reconstructor | 3018 | 2130 | 180 |
| mech-refabricator | 1710 | 1278 | 180 |
| multiplicative-reconstructor | 2454 | 1710 | 216 |
| prime-refabricator | 2382 | 1734 | 216 |
| ship-refabricator | 1674 | 1314 | 180 |
| tank-refabricator | 1686 | 1230 | 180 |
| tetrative-reconstructor | 3678 | 2598 | 216 |

The 24 shared raw templates occupy **1,010,880 bytes**. The eight material-cache builds in the ground gallery occupy **1,055,616 bytes**, including roof arrays. This is CPU geometry storage, not total memory/GPU allocation; the upper renderer has its own material cache and the host atlas/temporary batches are excluded. Cache-build counts remain stable after warmup: static geometry is not regenerated each frame.

Matched parent/final captures used stock Steam Java 25, 960×600, serial runs without competing compiler/test/game processes, two warmed 64-frame measurement blocks per scene, and screenshots outside those timed frames. The fixture pauses simulation and supplies native operation snapshots to isolate rendering. The close field contains eight main upgrade facilities, 24 upper copies and six previous factories; the dense field adds 64 upgrade facilities. Culling/LOD remain enabled, so population is not a promise that every object is visible in every camera frame.

| Scene | CPU render ms, before → after | GPU ms, before → after | Draw calls | Submitted vertices |
|---|---:|---:|---:|---:|
| Upgrade close field | 1.111 → 1.274 | 0.518 → 0.556 | 84 → 83 | 141,185 → 152,345 |
| Dense field | 0.800 → 1.188 | 0.596 → 0.579 | 89 → 88 | 183,533 → 193,625 |

The measured extra CPU rendering cost is about **0.163 ms / 0.388 ms**, respectively. Two-block CPU variation is substantial (all raw blocks are in the validation JSON). The small GPU difference is not evidence of a speedup. These are short local renderer measurements, **not full gameplay FPS or a cross-platform performance guarantee**. Stock-runtime allocation counters were unavailable (`-1`, not zero); no allocation-reduction claim is made. Java 17 gallery allocation samples are available as ancillary data, not a matched before/after allocation benchmark.

## Validation and artifact correspondence

- **199 test programs PASS on Java 17, and the same 199 PASS on stock Steam Java 25.** This includes existing campaign, construction pause/priority, XYZ clipboard, network identity/connection/prediction, combat, unit rendering, mouse/input and save regressions.
- New/extended native tests: 17,095,463 factory geometry/state checks in 1,440 poses; 31,931,505 upgrade geometry/state checks in 1,920 poses; 57 mounted-light/layer/mesh checks with 476 swept poses; 4,864 real native production assertions.
- **44 actual SDL/OpenGL scenes / 5,632 frames on each runtime, zero GL errors.** Top/side/oblique and low views, every new block, repaired six factories, native completion/output, inactive/progress, Z1/Z3/Z5, stacked/below/adjacent/uneven terrain, authorized placement, real clipboard and disposable SaveIO reload were exercised. Each gallery recorded one real ground completion for all eight new block types.
- Three matched old/new factory scenes (384 frames per build) separately document top, low and moving-light corrections. Screenshots are scripted graphics evidence, not owner acceptance.
- Independent compilation of the frozen final source matches **1,054 packaged runtime classes byte for byte**; resource correspondence passes. Artifact audit finds **1,155 previous runtime classes unchanged**, no removed entries, unchanged metadata/notices/dependencies, and Java 17-compatible class versions.
- Actual JAR/folder fingerprint matches on both runtimes. All **2,188 protected previous artifact files** retain their hashes. Primary saves/profiles and published releases were not edited.

See `A2-16l-TEST-MATRIX.md`, `A2-16l-MANUAL-TEST.md`, complete model inventory and machine-readable `A2-16l-VALIDATION.json`. The matching JAR, clean corresponding-source archive, notices, sanitized evidence and hashes are packaged locally. Source is exported from the final checkpoint; no full development folder, profiles, saves, host-game binaries or raw Steam logs are included. Nothing is pushed, published or sent to Workshop.

## Limitations and remaining acceptance

Physical control/gameplay acceptance on Windows, actual Linux gameplay, current native import/restart, current campaign-sector UI transitions and two-computer multiplayer were **NOT TESTED** in this pass. Automated host launches are not those tests. The owner can use the same Java 17-compatible JAR on the currently supported Windows/Linux hosts.

Existing upper-layer multiplayer snapshots do not carry every transient native animation clock/efficiency field. Remote elevated upgrade machinery may therefore appear static/dim; remote transient animation is not certified and no protocol expansion or guessed production state was introduced. Gameplay remains server-authoritative. Native water, payload, unit-cap, resource, range and structural/headroom rules still apply. Progress/payload unit art remains a native flat assembly preview inside the modeled bay. Assemblers/basic module and third-party factories retain their previous render path.
