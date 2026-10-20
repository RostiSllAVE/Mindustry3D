# A2-13b — Layered liquids, immersion and environmental exposure

Baseline: accepted A2-13 `0e69d10acdd312dded07e932156baf486bf933a8`, current `main`. This extends the visual pass with the owner's explicitly requested gameplay statuses/idle-flight avoidance and approved **controlled-flyer diving**. Local diagnostic only; existing Alpha releases remain intact.

## Two separate layers

The original native-textured liquid fan becomes an **opaque bed**. Shallow sand/darksand water has a0.55-tile centre depth, ordinary liquids0.95, and native deep floors1.80. Shared corner depths average adjoining open liquid depths; shore/occupied corners stay at Z0. The surface remains near−0.16, independently animated above the bed. Native textures, material palettes, shallow/deep distinctions and the existing terrain/ore models remain recognizable.

Each detailed liquid chunk caches a second atlas mesh for the surface. It is drawn after opaque world geometry with alpha blending, depth testing and no depth writes, allowing the bed and submerged hull parts to remain visible. Detailed water has24 vertices/tile across both layers, versus12 previously. This is one additional batched surface pass, not one draw call per tile. Far homogeneous water still aggregates; intricate coastlines retain per-tile fans. Far surface requests use a retained primitive queue and are replayed **inside the transparent pass**, including overflow flushes, rather than drawing prematurely during opaque generation. Cache bytes include both meshes; the64MiB budget and six uploads/frame remain. Blend functions/equations and depth state are restored.

Chunk culling includes the deepest bed, with a regression and actual submerged downward capture. Native observer/projectile terrain queries otherwise retain their previous rules; this is not a full underwater combat or observer-camera conversion.

No terrain editing, flooding, new liquid blocks or new save format is introduced. Foundation heights and original ground/naval support remain unchanged. Unknown liquids keep their native palette, bed and a static generic surface; missing atlas art retains a static colour fallback. Native per-tile art and low-poly facets can remain visible.

## Material animation

- **Water:** coherent waves increase from a0.025 to0.12-tile bound, with a smooth spatial amplitude envelope. View-dependent highlights use a simple sun direction and the current ambient brightness, plus moving small glints. This is stylized lighting rather than full sky/reflection simulation.
- **Oil/tar and slag/lava:** separate viscous shading, deterministic cell sizes/phases, actual vertex dome inflation and a spreading/fading burst ring. Bubbles are cosmetic and do not explode for damage. Lava uses bright orange/yellow highlights; oil remains dark.
- **Cryofluid:** angular pale ice patches grow, drift/converge and shrink/disperse with independent seeded sizes and phases. They are visual surface ice, not new physical platforms or frozen map tiles.
- Material-specific motion weights are retained. Coordinate hashes vary bubble/detail sizes and timing without per-frame random allocations. Wave amplitude varies smoothly rather than giving every tile an unrelated random height. Mixed-material corners and detailed-chunk aprons are motionless to avoid cracks. The distant surface has reduced geometric animation.

Settings → FPV flight → **Animated 3D liquid waves** controls wave/detail animation and matching visual naval bobbing. OFF keeps the separate bed/surface and their geometry cost. No new native dependency or replacement game binary is needed. Clock/hash variation is cosmetic; animation phases are not synchronized as multiplayer gameplay state.

## Diving and lens effects

The owner approved diving only for the player-controlled native flying unit. `LiquidBody.flightFloor` supplies the **same interpolated sloping bed used visually** to offline/server FPV collision, Build Mode sweeps and local prediction. Ground units, boats, building coordinates and ordinary AI physical floors keep their previous support. Shore/occupied tiles stop underwater travel into land or buildings. The existing impact solver still handles a hard bed collision. There is no new buoyancy, drag or oxygen simulation. Creator-flight ground units are not converted into submarines.

The camera detects crossing the actual liquid surface and applies a material-coloured underwater tint. On exit the tint clears promptly, followed by a fading lens film: water droplets/refraction, dark oil residue, a warmer short slag film or cold-coloured residue. Film durations are approximately4 seconds,7 for oil and2.5 for slag. The filter follows the **camera lens**, so an external camera above water is not submerged merely because its unit is. A heat-edge tint uses the same connected-area exposure query near lava. The post shader replaces the existing scene blit; it does not add another full-screen render target or alter aiming/input.

Underwater firing/targeting still uses the existing combat terrain rules. This pass does not promise a new underwater weapon/ballistics system. Physical diving/input and the final artistic appearance remain owner acceptance checks.

## Authoritative native statuses

`LiquidExposureSystem` indexes connected **open liquid regions by material**. Disconnected puddles do not count as one giant lake. The derived cache rebuilds on world/tile changes, not every unit/frame. Building-covered cells do not emit; elevated platforms do not change the underlying native liquid tiles.

For connected area A, horizontal radii in tiles are:

| Effect | Radius |
| --- | --- |
| Lava burning | `0.6 + min(5, 0.28 × sqrt(A))` |
| Lava melting / close contact | `0.16 + min(1.2, 0.08 × sqrt(A))` |
| Water/oil/cryo/other native contact effect | `0.08 + min(0.18, 0.018 × sqrt(A))` |

The hull's proximity to actual source tile footprints is tested with a **5× upward reach**: `sqrt(horizontalDistance² + (heightAboveSurface / 5)²)`. Water remains close-contact horizontally. For example a one-cell lava puddle burns within about0.88 tiles horizontally/4.4 above, while a64-cell lake reaches2.84/14.2. Melting requires a closer approach than burning. Large-source radii are bounded, not proportional without limit to map size.

Only the authoritative offline host/server calls native `Unit.apply`. Lava adds native burning in the outer volume and the native floor's melting status close to it; other floors apply their own status, such as wet, tarred or freezing. Native immunity, status interactions, damage ticks and expiry remain native. There are no fabricated direct damage calls, duplicated projectiles or new packets. Clients cannot apply these authoritative effects. Checks target roughly10Hz per unit and are staggered across frames, rather than processing the entire battle in a periodic burst. Pausing stops application; native status duration governs persistence after leaving.

The heat/contact model is bounded proximity, **not thermal occlusion**: walls/roofs do not insulate against nearby exposed liquid. Visual bubbles and ice do not expand damage independently of these rules. This behavior is new gameplay requested by the owner; weapon damage/rates and resource rules were not rebalanced.

## Idle flying AI

A cached clearance-height grid conservatively covers liquid-effect volumes. The existing altitude solver samples current/ahead positions and the hull footprint, waits at the last safe XY while clearance is insufficient, then climbs **over the volume** and resumes native movement. It does not replace native XY pathfinding. If a ceiling prevents clearing the volume, an idle flyer can wait rather than inventing a new lateral route; an active task releases this hold. Boundary expansion is performed when indexing the liquid region, not by scanning an entire lake per flyer/frame.

Avoidance applies to free native FlyingAI and movement commands. Player control, active shooting/targets, mining, building, repair and other task commands retain their ordinary approach. A cached native AI-target accessor fails closed if unsupported. Native160.7 fixtures confirm both recognition of free AI and an actual altitude-solver climb; attacks/mining are excluded. This is not new prediction, networking or autonomous player steering.

## Validation and measurements

See `A2-13B-TEST-MATRIX.md` and `A2-13B-VALIDATION.json` for measured results and exact evidence boundaries. The final JAR runs143 regression programs on Java17 and stock Steam Java25; additional diagnostic probes check negative-Z protocol4 round-trip and CPU cost. Native campaign save/reload, construction, priority, selection/copy, input, combat and previous ore/relief checks remain in the suite.

The actual Steam160.7 renderer is exercised using the real host atlas and production runtime. Only a lab smoke entry is replaced in disposable copies. Captures cover low/overhead water, a second phase, animationOFF, underwater view, exit film, oil, slag and cryofluid, three ship sizes and platforms. The packaged JAR retains its normal entry. These scripted captures are not physical mouse/gameplay or Linux acceptance.

The identical geometry probe compares128² ocean/river/mixed maps,64 chunks,5 warm-up/7 timed passes, real Mesh buffers through MockGL. It counts **both** bed and surface meshes. Ocean retained mesh storage rises7,077,888→14,155,776 bytes, vertices196,608→393,216 and material batches64→128. These are total scene-generation measurements, not per-frame regeneration or FPS. Far CPU timings include request assembly but exclude its transparent replay and GPU work.

A separate Java17 CPU fixture with a2,304-cell lava lake measured full exposure sweeps: median1.156 ms at100 units,3.233 ms at1,000,9.762 ms at4,096; index times9.116/1.968/2.301 ms respectively (first run includes cold JIT). It drives0.11-second updates, so every unit is eligible; ordinary shorter frames stagger checks. Native unit damage ticks and the full game/AI/render loop are excluded. These numbers do not certify large-battle FPS or remote multiplayer latency. Weaker Linux hardware remains untested.

## Distribution and remaining checks

Use only `local/a2-13b/ready/mindustry-fpv-a2-13b-diagnostics.jar`. The kit contains matching source, checksum/source manifests, notices and sanitized evidence; no host binaries/textures, saves, profiles or private Steam logs are included. All multiplayer participants and the server need the identical candidate; existing strict payload checks remain. The existing diagnostic metadata version is retained, so identify this build by filename/checksum.

All prior releases/checkpoints are preserved. No push, publication or Workshop update occurs. Actual Windows play/import, Linux rendering/input/performance and independent multiplayer acceptance remain **NOT TESTED** by the automated runner. The owner should particularly judge wave visibility, lens-film style, hot-lake ranges and sailing/diving transitions before treating this candidate as accepted.

## Final measured comparison

Same probe/settings, no concurrent regression runs during measurements. CPU detailed rebuild64 chunks; coarse request assembly excludes transparent replay/GPU. No FPS improvement is claimed.

| Runtime/map | CPU before → after | Coarse assembly before → after |
| --- | ---: | ---: |
| java17/ocean | 8.821 → 12.238 ms | 0.467 → 0.588 ms |
| java17/river | 13.825 → 15.943 ms | 0.790 → 0.883 ms |
| java17/mixed | 13.023 → 15.375 ms | 0.707 → 0.752 ms |
| steam25/ocean | 5.800 → 13.121 ms | 0.685 → 0.715 ms |
| steam25/river | 7.886 → 13.656 ms | 0.687 → 0.940 ms |
| steam25/mixed | 7.843 → 9.701 ms | 0.837 → 0.844 ms |

Actual Windows stock Steam25 GPU/CPU sample:25 CPU samples per low/overhead view,60 low GPU samples and29 overhead GPU samples, in a small scripted scene. Screenshots are outside timing brackets; polling is asynchronous. Timed scenes/cameras match; the final helper adds a deep downward view after the common timing window.

| Metric | A2-13 | Final candidate |
| --- | ---: | ---: |
| Low CPU | 5.604 ms | 2.625 ms |
| Overhead CPU | 2.456 ms | 2.724 ms |
| Low GPU | 0.070 ms | 0.166 ms |
| Overhead GPU | 1.059 ms | 2.247 ms |
| Retained scene mesh | 1,554,336 bytes | 2,534,112 bytes |

The additional transparent layer/FX has a real cost. These short samples are noisy, do not measure owner-map performance and do not certify Linux or multiplayer. Looking straight down close to the native deepwater bed can still show little texture detail; no new bed texture assets were introduced.

## Final artifact identity

Diagnostic JAR SHA-256: `b4a0215f938ffcc72fd39d8ce8c2cdb4e1c3a73f2f8692c3ff3411cd98f22369`. Canonical JAR/folder identity: `payload1-380e6f2b673023afc042140c6c10ca65435c4be964972ef1481b934f7e029f4c`. Java17 fresh recompilation matches all894 produced runtime classes and packaged resources. Both Java17 and stock Steam25 native fingerprint checks accept the same JAR and extracted folder. No dependency on the lab smoke replacement is included.
