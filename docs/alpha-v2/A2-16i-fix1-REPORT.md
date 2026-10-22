# A2-16i-fix1 — clipping, support contact and transfer polish

Baseline: accepted commit `2619bf3` on `main`. Visual-only changes; no balance, placement, physical heights, network protocol, save format, input or transfer-accounting changes. Published releases and earlier diagnostics remain intact. Final physical visual acceptance remains with the owner.

## Confirmed causes and seven-block audit

| Block | Evidence and correction |
| --- | --- |
| Illuminator | No missing outer crown was reproduced in the baseline full view. Its coincident foundation underside produced contact streaks; its reverse face is rejected from above while its outward underside is retained. Procedural roofs now keep opaque metal beneath decorative sprite alpha. No height/pivot change. |
| Impact Reactor | Full-sprite alpha punched holes in solid roofs, rails and rotor details. Solid mechanical roofs retain their sampled metal beneath transparent art. Frame/open spaces remain actual geometry. Ground contact is cleaned; native progress and glow are unchanged. |
| Overdrive Projector | The native cap coincided with the shaft roof at .85h; it is separated by .008 tile, like the previously accepted Force cap. Ring/body roof material and foundation contact are corrected; native charge rotation remains. |
| Phase Weaver | Transparent sprite portions cut away solid frame/deck/shuttle surfaces. Opaque roof underpaint and foundation contact fix the presentation without filling the intentional spaces between struts. |
| Wave, Tsunami, Cyclone | Coarse sprite coverage discarded thin opaque features; non-divisible sprite/grid boundaries could also miss pixel centres. Conservative sampling preserves those features on the same 24-cell maximum grid and 16,380-vertex budget. Pitched gun extrema could lie outside old upper-floor culling bounds; the seven audited blocks now use a separate visual envelope. Guns keep their original pivots, aim, pitch and projectile behavior. |

A real native-drawer capture test covers 72 yaw/pitch poses and reproduces 66 old narrow-frustum failures with a diagnostic 98-pixel sprite. This is automated geometry evidence, not physical player aiming. Other families retain their prior culling path and default silhouette sampling.

Actual host-atlas measurements (opaque pixel centres omitted by the cached silhouette):

| Native art | Missed before → after | Cached vertices before → after |
| --- | --- | --- |
| cyclone | 12 → 0 | 828 → 732 |
| tsunami | 33 → 0 | 852 → 816 |
| wave | 13 → 0 | 780 → 732 |

## Grounding strategy

Models keep their exact original support plane. The existing per-view geometry stream omits inward down-facing foundation triangles only when the camera is above their plane: local Y=0 for mechanical templates and Y=-.5 for centred turret-base sprites. Side edges still touch the support, so there is no global lift or floating gap. Below-plane views (including unsupported Free Mode blocks) retain the complete underside; offline builders without a viewpoint retain complete geometry too. No extra shader discard, GL culling state or render pass is needed. Ordinary/airborne meshes remain two-sided; moving parts are not treated as foundations. Drill, conveyor, manufacturing, chemical, power, liquid, support and utility families share this presentation helper. No model dimensions or immutable templates change.

The existing natural-floor sampler already flattens occupied footprints. Covered ore-floor overlays are now omitted rather than drawing slightly above a foundation; ore identity, deposits outside footprints and mining logic stay unchanged. Elevated and copied models use the existing construction plane and platform/column support geometry. No support or picking bounds are altered.

## Shield, wall projection and transfer effects

Force Projector's floor-coplanar bottom fan and bottom outline are omitted. A .025-tile visual gap and a fade strip spanning the existing maximum floor micro-relief plus .08 soften the lower side. The real XY perimeter, top, authoritative volume, health and recharge remain unchanged; the small hexagonal activity sphere is retained. Shield-wall/other shield variants keep their existing closed-shell behavior.

Uncovered wall projections use a continuous height gradient in the block's native map color. The repeating wall texture is replaced by a shared white atlas swatch and color/opacity interpolation. Cached surfaces, neighbor-face removal, damage height, coverage suppression, depth occlusion and distance fading remain in use.

Launch/landing pulse width parameters increase from .16/.045 to .27/.085. Native progress drives 64 tiles of visual travel instead of 25 (2.56× travel speed for the same actual event duration); pulse length is 12 instead of 7. The emitter/receiver endpoint is .82h+.10 instead of .46h+.04, above the existing gantry. Direction, pause/progress, cargo evidence, end conditions and resource accounting are unchanged. Idle pads cannot create a pulse.

## Validation

- Java 17: **192/192 PASS**; stock Steam Java 25: **192/192 PASS**, against the packaged final JAR. Includes two new presentation regressions, native construction/priority/selection/3D clipboard, campaign/save, combat/defense, networking identity and input regressions.
- Independently recompiled **1035** runtime classes are byte-identical to the JAR; packaged resources match. **1127** existing runtime/dependency classes are unchanged. Only the listed render classes changed.
- Actual SDL/OpenGL on Windows: **6528 frames on Steam 25 and 6528 on Java 17**, 51 scenes each, no GL errors or observed render-state/native-world mutation. Scenes include all seven blocks, top/oblique/low views, Z1/Z3/Z4/Z5, structures-on-structures, an actual authoritative clipboard paste, dense/mixed fields, previews, save/reload, actual native shipments and a real shield hit. JSON/PNG and fixture sources are in the kit.
- JAR/folder payload fingerprints agree on both VMs; JNA, public metadata, notices and licenses are unchanged. **2176** previous artifact files were rehashed unchanged.
- Physical owner gameplay, Linux GPU/input and real remote multiplayer are **NOT TESTED** in this pass. No physical acceptance or sustained FPS improvement is claimed.

## Measured rendering cost

Separate scripted Steam 25 SDL runs, 960×600, 48 warm-up frames then 64 CPU/allocation samples per scene, asynchronous GPU query samples, one draw-call/vertex probe. Run after compilers and CPU regressions finished, with identical scene/camera settings and original versus final JAR. These are short diagnostic renderer measurements, not sustained game FPS or a clean-machine benchmark.

| Scene | CPU ms before → after | GPU ms before → after | Draw calls | Submitted vertices | Allocated bytes/frame |
| --- | --- | --- | --- | --- | --- |
| oblique | 1.937 → 1.786 | 0.707 → 0.639 | 51 → 51 | 144161 → 143057 | -1 → -1 |
| field100 | 2.428 → 2.573 | 1.112 → 1.125 | 34 → 34 | 185915 → 181349 | -1 → -1 |
| field1024 | 6.448 → 5.442 | 5.680 → 4.462 | 70 → 70 | 415343 → 404927 | -1 → -1 |
| force-close | 0.524 → 0.528 | 0.242 → 0.288 | 27 → 27 | 69863 → 69689 | -1 → -1 |
| send-close | 0.565 → 0.498 | 0.330 → 0.363 | 38 → 38 | 85259 → 85235 | -1 → -1 |
| receive-close | 0.520 → 0.511 | 0.278 → 0.429 | 42 → 42 | 97649 → 97625 | -1 → -1 |
| projections | 0.621 → 0.504 | 0.277 → 0.440 | 33 → 33 | 82251 → 82251 | -1 → -1 |

Read negative/unavailable allocation/GPU values as unavailable, not zero cost. Scene-to-scene differences include truthful visibility and the deliberately taller beam; cache reuse is verified after warm-up. No static mesh is regenerated per frame and no new textures, dependencies or rendering pass are added.

In this final pair the 1,024-block scene improved from 6.448 to 5.442 ms CPU and 5.680 to 4.462 ms GPU, with 70 draw calls in both builds. Smaller cases were mixed: the 100-block CPU mean rose by .145 ms, and receiving/projection GPU means rose by .150/.163 ms. These short samples do not establish a universal FPS improvement or driver-independent parity.

An intermediate per-fragment front-face-discard variant measured a dense-scene regression (1,024 blocks: CPU 4.549 → 7.306 ms, GPU 4.029 → 6.437 ms). It was rejected. The final build performs the contact-side rejection in the existing per-view CPU stream; the intermediate measurement is retained separately and is not the final performance result.

## Artifacts and owner checks

Importable JAR: `local/a2-16i-fix1/test-kit-view/mindustry-fpv-a2-16i-fix1-diagnostics.jar`

SHA-256: `cf297c0d7003770f2f4b2b26c25b53626036ec508e0ed31a88ee1b3428b7d915`

The same directory receives the matching Git corresponding-source ZIP, notices, sanitized evidence and SHA256SUMS. The source manifest identifies the final local commit. This is an unreleased diagnostic build, not a replacement for published Alpha assets. Multiplayer peers must use the identical payload.

The three owner checks are in [A2-16i-fix1-MANUAL-TEST.md](A2-16i-fix1-MANUAL-TEST.md): (1) seven models/support/copy on multiple floors, (2) real shield hit/recovery and covered/uncovered wall gradients, (3) real launch/landing pulses with pause and save/reload.

Remaining visual limits: stylized coarse contour sides are retained; original painted dark markings remain painted dark, not holes; intentional framework openings remain. Opaque world structures continue to hide effects naturally. Physical visual approval, driver-specific behavior and long-session performance still need owner testing.
